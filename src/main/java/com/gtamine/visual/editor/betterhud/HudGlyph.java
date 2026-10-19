package com.gtamine.visual.editor.betterhud;

import com.gtamine.visual.editor.geometry.Rect;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;

public record HudGlyph(int codepoint, Style style, float cursor, float advance,
                       Rect bounds, ShaderLayout.Transform transform, int shaderId) {
    public OrderedText ordered() { return visitor -> visitor.accept(0, style, codepoint); }
    public String font() { return style.getFont().toString(); }
}
