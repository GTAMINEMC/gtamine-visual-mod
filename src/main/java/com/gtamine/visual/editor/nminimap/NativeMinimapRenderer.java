package com.gtamine.visual.editor.nminimap;

import com.gtamine.visual.editor.geometry.Rect;
import net.minecraft.block.MapColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import java.util.*;

/** One ordinary GUI texture: no world shader, lightmap encoding or mod-specific shader API. */
public final class NativeMinimapRenderer {
    private static final Identifier TEXTURE = Identifier.of("gtamine_visual", "native_minimap");
    private Map<Integer, MinimapSprite> sprites = Map.of();
    private byte[] incoming, lastColors;
    private final int[] palette = new int[256];
    private final int[] map = new int[128 * 128];
    private final int[] composed = new int[MinimapRaster.SIZE * MinimapRaster.SIZE];
    private List<MinimapMarker> markers = List.of();
    private Text markerText;
    private int markerEntity = -1, lastYaw = Integer.MIN_VALUE, lastMetadata = -1;
    private long markerSeen;
    private boolean dirty = true;
    private NativeImageBackedTexture texture;
    private double padding;

    public NativeMinimapRenderer() {
        for (int i = 0; i < palette.length; i++) palette[i] = MapColor.getRenderColor(i);
    }

    public void resources(Map<Integer, MinimapSprite> sprites) {
        reset();
        this.sprites = sprites;
        padding = MinimapRaster.padding(sprites);
    }

    public void reset() {
        incoming = null; lastColors = null; markerText = null; markerEntity = -1;
        markers = List.of(); dirty = true; lastYaw = Integer.MIN_VALUE; lastMetadata = -1;
        if (texture != null) {
            MinecraftClient.getInstance().getTextureManager().destroyTexture(TEXTURE);
            texture = null;
        }
    }

    public void captureMap(byte[] colors) { incoming = colors; }
    public boolean hasMap() { return incoming != null; }

    public void captureMarkers(int entityId, Text text) {
        List<MinimapMarker> found = new ArrayList<>();
        boolean same = text == markerText || text.equals(markerText);
        if (!same) {
            text.asOrderedText().accept((index, style, cp) -> {
                if (found.size() >= 512) return false;
                if (style.getFont() instanceof StyleSpriteSource.Font font && font.id().equals(Identifier.of("nminimap", "default"))
                        && style.getColor() != null && sprites.containsKey(cp)) found.add(new MinimapMarker(cp, style.getColor().getRgb()));
                return true;
            });
        }
        if (same || !found.isEmpty() || entityId == markerEntity) {
            markerSeen = System.nanoTime();
            markerEntity = entityId;
            if (!same) {
                markerText = text;
                if (!markers.equals(found)) { markers = List.copyOf(found); dirty = true; }
            }
        }
    }

    public void draw(DrawContext context, MinimapLayout layout, int metadata, Rect bounds, float yaw) {
        if (incoming == null) return;
        if (!Arrays.equals(incoming, lastColors)) {
            lastColors = incoming.clone();
            for (int i = 0; i < map.length; i++) map[i] = palette[Byte.toUnsignedInt(lastColors[i])];
            dirty = true;
        }
        if (!markers.isEmpty() && System.nanoTime() - markerSeen > 500_000_000L) {
            markers = List.of(); markerText = null; markerEntity = -1; dirty = true;
        }
        boolean round = metadata == 29 || metadata == 67;
        int rotation = round ? Math.round(yaw * 2) : 0;
        if (lastYaw != rotation || lastMetadata != metadata) dirty = true;
        if (texture == null) {
            texture = new NativeImageBackedTexture("GTAMINE Visual minimap", MinimapRaster.SIZE, MinimapRaster.SIZE, false);
            MinecraftClient.getInstance().getTextureManager().registerTexture(TEXTURE, texture);
            dirty = true;
        }
        if (dirty) {
            MinimapRaster.compose(composed, map, layout, metadata, Math.PI - Math.toRadians(rotation / 2.0), markers, sprites, padding);
            var image = texture.getImage();
            for (int y = 0; y < MinimapRaster.SIZE; y++) for (int x = 0; x < MinimapRaster.SIZE; x++) {
                image.setColorArgb(x, y, composed[y * MinimapRaster.SIZE + x]);
            }
            texture.upload();
            lastYaw = rotation; lastMetadata = metadata; dirty = false;
        }
        double span = layout.content() + (round ? 0 : -1) + layout.border();
        double px = bounds.width() / span, py = bounds.height() / span;
        context.createNewRootLayer();
        context.getMatrices().pushMatrix();
        try {
            context.getMatrices().translate((float)(bounds.x() - padding * px), (float)(bounds.y() - padding * py));
            context.getMatrices().scale((float)((span + padding * 2) * px / MinimapRaster.SIZE),
                    (float)((span + padding * 2) * py / MinimapRaster.SIZE));
            context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, 0, 0, 0, 0,
                    MinimapRaster.SIZE, MinimapRaster.SIZE, MinimapRaster.SIZE, MinimapRaster.SIZE);
        } finally { context.getMatrices().popMatrix(); }
    }
}
