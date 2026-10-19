package com.gtamine.visual.editor;

import com.gtamine.visual.editor.betterhud.*;
import com.gtamine.visual.editor.geometry.Rect;
import net.minecraft.text.Style;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DetectedBlockTest {
    private static HudGlyph glyph(Rect bounds) {
        return new HudGlyph(65, Style.EMPTY, 0, 8, bounds, null, 1);
    }
    @Test void keepsSeparateWidgetsAndCombinesImageWithItsLabel() {
        var groups = DetectedBlock.detect(List.of(glyph(new Rect(10, 10, 20, 20)),
                glyph(new Rect(34, 14, 40, 10)), glyph(new Rect(200, 10, 30, 20)), glyph(null)));
        assertEquals(2, groups.size());
        assertTrue(groups.stream().anyMatch(group -> group.glyphs.size() == 2 && group.bounds.equals(new Rect(10, 10, 64, 20))));
    }
    @Test void mergesTransitiveConnectionsAndDoesNotFillEmptyInterior() {
        var groups = DetectedBlock.detect(List.of(glyph(new Rect(10, 10, 5, 5)),
                glyph(new Rect(30, 10, 5, 5)), glyph(new Rect(20, 10, 5, 5))));
        assertEquals(1, groups.size());
    }
}
