package com.gtamine.visual.editor.betterhud;

import com.gtamine.visual.editor.geometry.Rect;
import com.gtamine.visual.mixin.TextRendererAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;
import java.util.ArrayList;
import java.util.List;

public final class BetterHudReader {
    private BetterHudReader() { }

    public static List<HudGlyph> read(Text text, TextRenderer renderer, int x, int y, ShaderLayout layout) {
        var client = MinecraftClient.getInstance();
        var window = client.getWindow();
        double width = window.getScaledWidth(), height = window.getScaledHeight();
        List<HudGlyph> glyphs = new ArrayList<>();
        float[] cursor = {x};
        text.asOrderedText().accept((index, style, cp) -> {
            if (glyphs.size() >= 8192) return false;
            var glyph = ((TextRendererAccessor)renderer).gtamineVisual$getGlyph(cp, style);
            float advance = glyph.getMetrics().getAdvance(style.isBold());
            var drawn = glyph.create(cursor[0], y, -1, 0, style,
                    style.isBold() ? glyph.getMetrics().getBoldOffset() : 0, glyph.getMetrics().getShadowOffset());
            Rect bounds = null;
            ShaderLayout.Transform transform = null;
            int shaderId = 0;
            if (drawn != null && drawn.getTop() > (1 << 23)) {
                int bit = (int)drawn.getTop() >> layout.heightBit();
                shaderId = bit - (1 << layout.maxBit());
                transform = layout.transforms().get(shaderId);
                if (transform != null) {
                    Rect raw = new Rect(drawn.getLeft(), drawn.getTop(),
                            Math.max(1, Math.min(drawn.getEffectiveMaxX() - drawn.getLeft(), Math.max(1, advance))),
                            drawn.getBottom() - drawn.getTop());
                    bounds = transform.screenRect(raw, bit, layout, width, height, window.getFramebufferWidth(), window.getFramebufferHeight());
                    if (bounds.width() <= 0 || bounds.height() <= 0 || bounds.width() > width * 2 || bounds.height() > height * 2) {
                        bounds = null;
                        transform = null;
                    }
                }
            }
            glyphs.add(new HudGlyph(cp, style, cursor[0], advance, bounds, transform, shaderId));
            cursor[0] += advance;
            return true;
        });
        return glyphs;
    }
}
