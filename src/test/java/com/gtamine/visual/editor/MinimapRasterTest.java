package com.gtamine.visual.editor;

import com.gtamine.visual.editor.nminimap.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MinimapRasterTest {
    private static final MinimapLayout LAYOUT = new MinimapLayout(.364, .364, .05025, .05025, 64, 0);

    private static int[] coloredMap() {
        int[] map = new int[128 * 128];
        for (int y = 1; y < 128; y++) for (int x = 1; x < 128; x++) {
            map[y * 128 + x] = x > 33 ? 0xFFFF0000 : 0xFF0000FF;
        }
        return map;
    }

    @Test void rendersSquareWithoutShaderAndIgnoresRoundRotation() {
        int[] first = new int[256 * 256], second = new int[first.length];
        MinimapRaster.compose(first, coloredMap(), LAYOUT, 17, 0, List.of(), Map.of(), 0);
        MinimapRaster.compose(second, coloredMap(), LAYOUT, 129, 1.5, List.of(), Map.of(), 0);
        assertArrayEquals(first, second);
        assertEquals(0xFF0000FF, first[128 * 256 + 32]);
        assertEquals(0xFFFF0000, first[128 * 256 + 224]);
    }

    @Test void cropsCircleAndRotatesMapPixels() {
        int[] image = new int[256 * 256];
        MinimapRaster.compose(image, coloredMap(), LAYOUT, 67, Math.PI, List.of(), Map.of(), 0);
        assertEquals(0, image[0]);
        assertEquals(0, image[255]);
        assertEquals(0xFFFF0000, image[128 * 256 + 32]);
        assertEquals(0xFF0000FF, image[128 * 256 + 224]);
    }

    @Test void preservesServerMarkersAndClampsThemToCircle() {
        int[] image = new int[256 * 256];
        var sprite = new MinimapSprite(1, 1, 2, 2, new int[]{0xFF00FF00});
        var markers = List.of(new MinimapMarker(100, 0xFF8000));
        MinimapRaster.compose(image, coloredMap(), LAYOUT, 67, 0, markers, Map.of(100, sprite), 8);
        int firstGreenX = 256, lastGreenX = 0;
        for (int y = 0; y < 256; y++) for (int x = 0; x < 256; x++) if (image[y * 256 + x] == 0xFF00FF00) {
            firstGreenX = Math.min(firstGreenX, x); lastGreenX = Math.max(lastGreenX, x);
        }
        assertTrue(firstGreenX > 210 && lastGreenX < 250, "Marker must stay near the right crop edge");
    }

    @Test void alphaCompositesBordersAndMarkersWithoutChangingOpaquePixels() {
        assertEquals(0xFF123456, MinimapRaster.blend(0xFF123456, 0));
        assertEquals(0xFFABCDEF, MinimapRaster.blend(0xFF123456, 0xFFABCDEF));
        int result = MinimapRaster.blend(0xFF0000FF, 0x80FF0000);
        assertEquals(255, result >>> 24);
        assertEquals(128, (result >>> 16) & 255);
        assertEquals(127, result & 255);
    }
}
