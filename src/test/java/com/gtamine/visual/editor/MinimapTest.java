package com.gtamine.visual.editor;

import com.gtamine.visual.editor.nminimap.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MinimapTest {
    private static final String CONFIG = """
            #define MAP_SIZE vec2(0.364, 0.364)
            #define MAP_OFFSET vec2(0.05025, 0.05025)
            #define MAP_CONTENT_SIZE 40
            #define MAP_CROP_RADIUS (MAP_CONTENT_SIZE/2.0)
            #define MAP_BORDER_WIDTH min(5.0, MAP_CONTENT_SIZE * 0.1)
            """;
    private static final String BODY = "0xFF0000 0x597D27 0x3737DC MAP_CROP_RADIUS MAP_BORDER_WIDTH";

    @Test void identifiesAllFourMapModesAndRejectsOrdinaryMaps() {
        byte[] colors = new byte[128 * 128];
        assertEquals(-1, MinimapLayout.metadata(colors));
        colors[0] = 18; colors[1] = 4; colors[2] = 49;
        for (int metadata : new int[]{17, 129, 29, 67}) {
            colors[3] = (byte)metadata;
            assertEquals(metadata, MinimapLayout.metadata(colors));
        }
        colors[3] = 1;
        assertEquals(-1, MinimapLayout.metadata(colors));
        assertEquals(-1, MinimapLayout.metadata(new byte[4]));
        assertEquals(-1, MinimapLayout.metadata(null));
    }

    @Test void respectsConfiguredMapSizeBorderAndRightAnchor() {
        var layout = MinimapLayout.parse(CONFIG, BODY);
        var left = layout.bounds(67, 960);
        var right = layout.bounds(29, 960);
        assertEquals(4, layout.border());
        assertEquals(960 - left.right(), right.x(), 0.00001);
        assertEquals(left.y(), right.y());
        assertEquals(left.width(), right.width());
        var larger = layout.bounds(67, 1920);
        assertEquals(left.width() * 2, larger.width());
        assertEquals(left.y() * 2, larger.y());
    }

    @Test void rejectsUnknownShaderGeometry() {
        assertThrows(IllegalArgumentException.class, () -> MinimapLayout.parse(CONFIG, "unknown"));
        assertThrows(IllegalArgumentException.class, () -> MinimapLayout.parse(CONFIG.replace("40", "256"), BODY));
    }

}
