package com.gtamine.visual.editor;

import com.gtamine.visual.editor.geometry.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SnapEngineTest {
    @Test void snapsNearestEdgeAndShowsGuide() {
        var result = SnapEngine.snap(new Rect(97, 71, 20, 12), List.of(new Rect(100, 30, 60, 20)), 800, 600);
        assertEquals(100, result.rect().x());
        assertEquals(100, result.verticalGuide());
        assertNull(result.horizontalGuide());
    }
    @Test void snapsScreenCenter() {
        var result = SnapEngine.snap(new Rect(388, 100, 20, 12), List.of(), 800, 600);
        assertEquals(390, result.rect().x());
        assertEquals(400, result.verticalGuide());
    }
    @Test void clampsBlocksInsideScreen() {
        var result = SnapEngine.snap(new Rect(-20, 700, 100, 20), List.of(), 800, 600);
        assertEquals(new Rect(0, 580, 100, 20), result.rect());
    }
    @Test void doesNotSnapOutsideThreshold() {
        var original = new Rect(83, 71, 10, 12);
        var result = SnapEngine.snap(original, List.of(new Rect(100, 30, 60, 20)), 800, 600);
        assertEquals(original, result.rect());
        assertNull(result.verticalGuide());
    }
}
