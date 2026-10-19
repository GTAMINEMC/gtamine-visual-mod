package com.gtamine.visual.editor;

import com.gtamine.visual.editor.betterhud.*;
import com.gtamine.visual.editor.geometry.*;
import com.gtamine.visual.editor.nminimap.*;
import com.gtamine.visual.editor.ui.ChatActionPlacement;
import com.gtamine.visual.editor.ui.ResetHudButton;
import com.gtamine.visual.module.VisualModule;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.ResourceManager;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** The editor owns local offsets only; it never sends packets or changes resource packs. */
public final class InterfaceEditorModule implements VisualModule {
    private static InterfaceEditorModule instance;
    private final LayoutStore store = new LayoutStore(FabricLoader.getInstance().getConfigDir()
            .resolve("gtamine_visual/interface-layout.json"));
    private final Map<Text, Prepared> cache = new IdentityHashMap<>();
    private final List<DetectedBlock> known = new ArrayList<>();
    private final Map<String, Rect> visible = new LinkedHashMap<>();
    private ShaderLayout shader;
    private String profile = "";
    private static final String MINIMAP = "nminimap";
    private MinimapLayout minimap;
    private String minimapProfile = "";
    private int minimapMetadata = -1;
    private long minimapSeen;
    private final NativeMinimapRenderer nativeMinimap = new NativeMinimapRenderer();
    private boolean active;
    private boolean customized;
    private long revision;
    private String dragging;
    private Rect dragStart;
    private boolean dragMoved;
    private double mouseStartX, mouseStartY;
    private LayoutStore.Offset dragOffset;
    private SnapEngine.Result snap;
    private int cachedWidth, cachedHeight;
    private int physicalWidth, physicalHeight;
    private static final class Prepared {
        final List<HudGlyph> glyphs;
        final List<DetectedBlock> blocks;
        final Map<Integer, String> membership;
        final int x, y;
        long revision = -1;
        List<Run> runs = List.of();
        Prepared(List<HudGlyph> glyphs, List<DetectedBlock> blocks, Map<Integer, String> membership, int x, int y) {
            this.glyphs = glyphs; this.blocks = blocks; this.membership = membership; this.x = x; this.y = y;
        }
    }
    private record Run(OrderedText text, double x, double y) { }
    private record Loaded(ShaderLayout shader, String profile, MinimapLayout minimap, String minimapProfile,
                          Map<Integer, MinimapSprite> sprites) { }

    public InterfaceEditorModule() {
        instance = this;
        ResourceLoader.get(ResourceType.CLIENT_RESOURCES).registerReloader(
                Identifier.of("gtamine_visual", "betterhud_layout"), (resources, prepare, synchronizer, apply) ->
                        CompletableFuture.supplyAsync(() -> load(resources.getResourceManager()), prepare)
                                .thenCompose(synchronizer::whenPrepared).thenAcceptAsync(loaded -> {
                            finishDrag();
                            shader = loaded.shader;
                            profile = loaded.profile;
                            minimap = loaded.minimap;
                            minimapProfile = loaded.minimapProfile;
                            customized = store.hasProfile(profile);
                            clearSession();
                            nativeMinimap.resources(loaded.sprites);
                            if (shader != null) LoggerFactory.getLogger("gtamine_visual").info(
                                    "HUD editor: loaded {} BetterHud shader layouts", shader.transforms().size());
                            if (minimap != null) LoggerFactory.getLogger("gtamine_visual").info("HUD editor: NMinimap support loaded");
                        }, apply));

        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof ChatScreen)) return;
            var reset = new ResetHudButton(Text.translatable("gtamine_visual.editor.reset"), button -> {
                resetPositions();
                // Keep the chat draft and restore typing focus after using the editor action.
                screen.children().stream().filter(child -> child instanceof TextFieldWidget)
                        .findFirst().ifPresent(screen::setFocused);
            });
            reset.visible = active && !client.options.hudHidden;
            Screens.getButtons(screen).add(reset);
            ScreenEvents.beforeRender(screen).register((s, context, mx, my, delta) -> {
                reset.visible = active && !client.options.hudHidden;
                if (!reset.visible) return;
                int inputTop = s.height - 14;
                var obstacles = new ArrayList<Rect>();
                for (var child : s.children()) {
                    if (child == reset || !(child instanceof ClickableWidget widget) || !widget.visible) continue;
                    if (widget instanceof TextFieldWidget) inputTop = Math.min(inputTop, widget.getY());
                    obstacles.add(new Rect(widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight()));
                }
                reset.setWidth(Math.min(client.textRenderer.getWidth(reset.getMessage()) + 16, s.width - 8));
                var position = ChatActionPlacement.find(s.width, inputTop, reset.getWidth(), reset.getHeight(), obstacles);
                reset.visible = position != null;
                if (position != null) reset.setPosition((int) position.x(), (int) position.y());
            });
            ScreenMouseEvents.allowMouseClick(screen).register((s, click) -> {
                if (reset.visible && click.button() == 0 && reset.isMouseOver(click.x(), click.y())) {
                    reset.mouseClicked(click, false);
                    return false;
                }
                return !startDrag(click.x(), click.y(), click.button());
            });
            ScreenMouseEvents.allowMouseDrag(screen).register((s, click, dx, dy) -> {
                if (dragging == null || click.button() != 0) return true;
                drag(click.x(), click.y());
                return false;
            });
            ScreenMouseEvents.allowMouseRelease(screen).register((s, click) -> {
                if (dragging == null || click.button() != 0) return true;
                drag(click.x(), click.y());
                finishDrag();
                return false;
            });
            ScreenEvents.remove(screen).register(s -> finishDrag());
            ScreenEvents.afterRender(screen).register((s, context, mx, my, delta) -> overlay(context, mx, my));
        });
    }

    private static Loaded load(ResourceManager resources) {
        String source = readResource(resources, Identifier.ofVanilla("shaders/core/rendertype_text.vsh"));
        ShaderLayout hud = null;
        MinimapLayout map = null;
        String mapProfile = "";
        try { hud = ShaderLayout.parse(source); }
        catch (IllegalArgumentException ignored) { /* BetterHud is optional. */ }
        try {
            String config = readResource(resources, Identifier.of("nminimap", "shaders/include/config.glsl"));
            String body = readResource(resources, Identifier.of("nminimap", "shaders/include/vertex_body.glsl"));
            map = MinimapLayout.parse(config, body);
            mapProfile = digest(config);
        } catch (IllegalArgumentException ignored) { /* NMinimap is optional. */ }
        return new Loaded(hud, hud == null ? "" : digest(source), map, mapProfile,
                map == null ? Map.of() : MinimapResources.load(resources));
    }

    private static String readResource(ResourceManager resources, Identifier id) {
        var resource = resources.getResource(id);
        if (resource.isEmpty()) return "";
        try (var reader = resource.get().getReader()) {
            return reader.lines().collect(java.util.stream.Collectors.joining("\n"));
        } catch (java.io.IOException ex) {
            LoggerFactory.getLogger("gtamine_visual").warn("Cannot read editor resource {}", id, ex);
            return "";
        }
    }

    public static InterfaceEditorModule instance() { return instance; }
    @Override public String id() { return "interface_editor"; }
    @Override public void enable() { active = true; clearSession(); }
    @Override public void disable() { finishDrag(); active = false; clearSession(); }

    private void clearSession() {
        cache.clear();
        known.clear();
        visible.clear();
        minimapMetadata = -1;
        minimapSeen = 0;
        nativeMinimap.reset();
    }

    public void beginFrame() {
        visible.clear();
        var window = MinecraftClient.getInstance().getWindow();
        if (cachedWidth != window.getScaledWidth() || cachedHeight != window.getScaledHeight()
                || physicalWidth != window.getFramebufferWidth() || physicalHeight != window.getFramebufferHeight()) {
            finishDrag();
            cache.clear();
            cachedWidth = window.getScaledWidth();
            cachedHeight = window.getScaledHeight();
            physicalWidth = window.getFramebufferWidth();
            physicalHeight = window.getFramebufferHeight();
        }
        if (active && minimap != null && minimapRecent()) {
            visible.put(MINIMAP, minimap.bounds(minimapMetadata, cachedWidth));
        }
    }

    public boolean render(DrawContext context, TextRenderer renderer, Text text, int x, int y, int color) {
        if (!active || shader == null) return false;
        if (!customized && !(MinecraftClient.getInstance().currentScreen instanceof ChatScreen)) return false;
        Prepared prepared = cache.get(text);
        if (prepared == null || prepared.x != x || prepared.y != y) {
            List<HudGlyph> glyphs = BetterHudReader.read(text, renderer, x, y, shader);
            if (glyphs.size() >= 8192) return false;
            List<DetectedBlock> blocks = DetectedBlock.detect(glyphs);
            Map<Integer, String> membership = new HashMap<>();
            Set<String> used = new HashSet<>();
            for (DetectedBlock block : blocks) {
                identify(block, glyphs, used);
                used.add(block.id);
                for (int index : block.glyphs) membership.put(index, block.id);
            }
            prepared = new Prepared(glyphs, blocks, membership, x, y);
            if (cache.size() >= 16) cache.clear();
            cache.put(text, prepared);
        }
        for (DetectedBlock block : prepared.blocks) {
            visible.put(block.id, block.bounds);
        }
        if (prepared.blocks.isEmpty()) return false;
        // Preserve vanilla's single text submission when no group has been moved.
        if (prepared.blocks.stream().allMatch(block -> {
            var offset = store.get(key(block.id));
            return offset.x() == 0 && offset.y() == 0;
        })) return false;

        if (prepared.revision != revision) {
            prepared.runs = prepareRuns(prepared);
            prepared.revision = revision;
        }
        for (Run run : prepared.runs) {
            context.getMatrices().pushMatrix();
            try {
                context.getMatrices().translate((float)run.x, (float)run.y);
                context.drawText(renderer, run.text, 0, y, color, true);
            } finally { context.getMatrices().popMatrix(); }
        }
        return true;
    }

    private List<Run> prepareRuns(Prepared prepared) {
        // Consecutive glyphs with the same translation are submitted together, preserving batching.
        List<Run> result = new ArrayList<>();
        List<HudGlyph> run = new ArrayList<>();
        double previousX = Double.NaN, previousY = Double.NaN;
        for (int i = 0; i < prepared.glyphs.size(); i++) {
            HudGlyph glyph = prepared.glyphs.get(i);
            String blockId = prepared.membership.get(i);
            double dx = 0, dy = 0;
            if (blockId != null && glyph.transform() != null) {
                Rect original = visible.get(blockId);
                Rect moved = moved(blockId, original);
                dx = (moved.x() - original.x()) / glyph.transform().xScale(cachedWidth, physicalWidth);
                dy = (moved.y() - original.y()) / glyph.transform().yScale(cachedHeight, physicalHeight);
            }
            if (!run.isEmpty() && (Math.abs(dx - previousX) > 0.001 || Math.abs(dy - previousY) > 0.001)) {
                result.add(makeRun(run, previousX, previousY));
                run.clear();
            }
            run.add(glyph);
            previousX = dx;
            previousY = dy;
        }
        if (!run.isEmpty()) result.add(makeRun(run, previousX, previousY));
        return List.copyOf(result);
    }

    private Run makeRun(List<HudGlyph> source, double dx, double dy) {
        List<HudGlyph> glyphs = List.copyOf(source);
        OrderedText ordered = visitor -> {
            for (int i = 0; i < glyphs.size(); i++) {
                HudGlyph glyph = glyphs.get(i);
                if (!visitor.accept(i, glyph.style(), glyph.codepoint())) return false;
            }
            return true;
        };
        return new Run(ordered, glyphs.getFirst().cursor() + dx, dy);
    }

    private void identify(DetectedBlock block, List<HudGlyph> glyphs, Set<String> used) {
        DetectedBlock nearest = null;
        double distance = Double.MAX_VALUE;
        for (DetectedBlock candidate : known) {
            if (used.contains(candidate.id) || !candidate.fonts.equals(block.fonts)) continue;
            double d = Math.abs(candidate.bounds.x() - block.bounds.x()) + Math.abs(candidate.bounds.y() - block.bounds.y());
            if (d < distance && block.bounds.near(candidate.bounds, 24)) { nearest = candidate; distance = d; }
        }
        if (nearest != null) {
            block.id = nearest.id;
            nearest.bounds = block.bounds;
            return;
        }
        // Text contents, balances and counters are deliberately excluded from identity.
        TreeSet<String> anchors = new TreeSet<>();
        double localY = Double.MAX_VALUE;
        for (int index : block.glyphs) {
            HudGlyph glyph = glyphs.get(index);
            anchors.add(glyph.transform().guiX() + "," + glyph.transform().guiY());
            localY = Math.min(localY, glyph.bounds().y() - cachedHeight * glyph.transform().guiY() / 100);
        }
        String base = digest(block.fonts + anchors + Math.round(localY / 4));
        int ordinal = 0;
        String candidate;
        do { candidate = base + "-" + ordinal++; } while (containsId(candidate));
        block.id = candidate;
        if (known.size() < 512) {
            DetectedBlock tracking = new DetectedBlock();
            tracking.id = block.id;
            tracking.fonts = block.fonts;
            tracking.bounds = block.bounds;
            known.add(tracking);
        }
    }

    private boolean containsId(String id) { return known.stream().anyMatch(block -> id.equals(block.id)); }
    private String key(String id) { return MINIMAP.equals(id) ? "nminimap/" + minimapProfile : profile + "/" + id; }
    private Rect moved(String id, Rect original) {
        var offset = store.get(key(id));
        double x = original.x() + offset.x() * cachedWidth;
        double y = original.y() + offset.y() * cachedHeight;
        // Do not pull an untouched, intentionally clipped server element onto the screen.
        if (offset.x() == 0 && offset.y() == 0) return original;
        return new Rect(Math.max(0, Math.min(x, Math.max(0, cachedWidth - original.width()))),
                Math.max(0, Math.min(y, Math.max(0, cachedHeight - original.height()))), original.width(), original.height());
    }

    private boolean startDrag(double x, double y, int button) {
        if (!active || button != 0 || y >= cachedHeight - 16) return false;
        var entries = new ArrayList<>(visible.entrySet());
        Collections.reverse(entries);
        for (var entry : entries) {
            Rect bounds = moved(entry.getKey(), entry.getValue());
            if (!bounds.contains(x, y)) continue;
            dragging = entry.getKey();
            dragStart = bounds;
            dragMoved = false;
            mouseStartX = x;
            mouseStartY = y;
            dragOffset = store.get(key(dragging));
            return true;
        }
        return false;
    }

    private void drag(double x, double y) {
        if (dragging == null || !active) return;
        if (!dragMoved && Math.hypot(x - mouseStartX, y - mouseStartY) < 1) return;
        dragMoved = true;
        Rect original = visible.get(dragging);
        if (original == null) { finishDrag(); return; }
        List<Rect> others = new ArrayList<>();
        visible.forEach((id, rect) -> { if (!id.equals(dragging)) others.add(moved(id, rect)); });
        snap = SnapEngine.snap(dragStart.move(x - mouseStartX, y - mouseStartY), others, cachedWidth, cachedHeight);
        store.put(key(dragging), new LayoutStore.Offset((snap.rect().x() - original.x()) / cachedWidth,
                (snap.rect().y() - original.y()) / cachedHeight));
        customized = true;
        revision++;
    }

    private void finishDrag() {
        if (dragging != null && !store.get(key(dragging)).equals(dragOffset)) store.save();
        dragging = null;
        snap = null;
    }

    private void resetPositions() {
        if (!active) return;
        // Cancel the current drag so releasing the mouse cannot restore an old offset.
        dragging = null;
        snap = null;
        store.resetAll();
        customized = false;
        revision++;
        cache.clear();
    }

    private void overlay(DrawContext context, int mx, int my) {
        if (!active || MinecraftClient.getInstance().options.hudHidden) return;
        context.createNewRootLayer();
        visible.forEach((id, original) -> {
            Rect rect = moved(id, original);
            int color = id.equals(dragging) ? 0xFF40E0A0 : rect.contains(mx, my) ? 0xFFE8FFFF : 0x887AB8C8;
            context.drawStrokedRectangle((int)Math.floor(rect.x()) - 2, (int)Math.floor(rect.y()) - 2,
                    (int)Math.ceil(rect.width()) + 4, (int)Math.ceil(rect.height()) + 4, color);
        });
        if (snap != null) {
            if (snap.verticalGuide() != null) context.drawVerticalLine(snap.verticalGuide().intValue(), 0, cachedHeight, 0xAA40E0A0);
            if (snap.horizontalGuide() != null) context.drawHorizontalLine(0, cachedWidth, snap.horizontalGuide().intValue(), 0xAA40E0A0);
        }
    }

    public boolean shouldHideMinimap() {
        return active && minimap != null && MinecraftClient.getInstance().options.hudHidden;
    }

    public void captureMinimap(byte[] colors) {
        if (active && minimap != null) nativeMinimap.captureMap(colors);
    }

    public boolean takeOverMinimap(int metadata) {
        if (!nativeMinimapEnabled() || !nativeMinimap.hasMap()) return false;
        minimapMetadata = metadata;
        minimapSeen = System.nanoTime();
        return true;
    }

    private boolean minimapRecent() {
        return minimapMetadata >= 0 && System.nanoTime() - minimapSeen < 250_000_000L;
    }

    public boolean nativeMinimapEnabled() { return active && minimap != null; }

    public void captureMinimapMarkers(int entityId, Text text) {
        if (nativeMinimapEnabled()) nativeMinimap.captureMarkers(entityId, text);
    }

    public void renderNativeMinimap(DrawContext context) {
        var client = MinecraftClient.getInstance();
        if (!nativeMinimapEnabled() || shouldHideMinimap() || !minimapRecent()
                || client.player == null || cachedWidth <= 0 || cachedHeight <= 0) return;
        Rect original = minimap.bounds(minimapMetadata, cachedWidth);
        nativeMinimap.draw(context, minimap, minimapMetadata, moved(MINIMAP, original), client.player.getYaw());
    }

    private static String digest(String text) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))).substring(0, 24); }
        catch (java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
}
