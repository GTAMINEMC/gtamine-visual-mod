package com.gtamine.visual.editor.betterhud;

import com.gtamine.visual.editor.geometry.Rect;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/** Spatial groups are inferred client-side; BetterHud does not transmit layout IDs. */
public final class DetectedBlock {
    public final List<Integer> glyphs = new ArrayList<>();
    public Rect bounds;
    public String id;
    public String fonts;

    public static List<DetectedBlock> detect(List<HudGlyph> source) {
        List<DetectedBlock> blocks = new ArrayList<>();
        for (int i = 0; i < source.size(); i++) {
            HudGlyph glyph = source.get(i);
            if (glyph.bounds() == null) continue;
            DetectedBlock block = new DetectedBlock();
            block.bounds = glyph.bounds();
            block.glyphs.add(i);
            // Merge against actual glyphs, not the growing group's empty rectangular interior.
            for (int j = blocks.size() - 1; j >= 0; j--) {
                DetectedBlock candidate = blocks.get(j);
                if (!candidate.bounds.near(glyph.bounds(), 6)) continue;
                boolean touches = candidate.glyphs.stream().anyMatch(index -> source.get(index).bounds().near(glyph.bounds(), 6));
                if (touches) {
                    block.bounds = block.bounds.union(candidate.bounds);
                    block.glyphs.addAll(candidate.glyphs);
                    blocks.remove(j);
                }
            }
            blocks.add(block);
        }
        for (DetectedBlock block : blocks) {
            TreeSet<String> fonts = new TreeSet<>();
            for (int index : block.glyphs) fonts.add(source.get(index).font());
            block.fonts = String.join("|", fonts);
        }
        return blocks;
    }
}
