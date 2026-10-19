package com.gtamine.visual.editor;

import com.gtamine.visual.editor.betterhud.ShaderLayout;
import com.gtamine.visual.editor.geometry.Rect;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ShaderLayoutTest {
    private static final String HEADER = """
            #define HEIGHT_BIT 13
            #define MAX_BIT 10
            #define ADD_OFFSET 4095
            #define DEFAULT_OFFSET 10
            """;

    @Test void readsMinifiedBetterHudLayoutAndDecodesScreenCoordinates() {
        var layout = ShaderLayout.parse(HEADER + "switch(id){case 1:xGui=ui.x*100.0/100.0;yGui=ui.y*50.0/100.0;layer=2;break;}");
        int bit = 1025;
        Rect raw = new Rect(480 - 110, bit * 8192.0 + 4095 + 10 + 20, 100, 12);
        var rect = layout.transforms().get(1).screenRect(raw, bit, layout, 960, 540, 1920, 1080);
        assertEquals(new Rect(850, 290, 100, 12), rect);
    }

    @Test void compensatesStaticScaleAndPivot() {
        var layout = ShaderLayout.parse(HEADER + "case 2:pos.x=(pos.x-(10))*2.0*uiScreen.x+(10);pos.y=(pos.y-(-4))*0.5+(-4);xGui=ui.x*50.0/100.0;break;");
        var transform = layout.transforms().get(2);
        assertEquals(1, transform.xScale(960, 1920));
        assertEquals(0.5, transform.yScale(540, 1080));
        assertEquals(-4, transform.pivotY());
    }

    @Test void rejectsUnknownOrChangedEncodingInsteadOfMovingWrongElements() {
        assertThrows(IllegalArgumentException.class, () -> ShaderLayout.parse("void main() {}"));
        assertThrows(IllegalArgumentException.class, () -> ShaderLayout.parse(HEADER.replace("13", "12") + "case 1:xGui=0;break;"));
        assertThrows(IllegalArgumentException.class, () -> ShaderLayout.parse(HEADER + "case 1:xGui=custom();break;"));
        assertThrows(IllegalArgumentException.class, () -> ShaderLayout.parse(HEADER + "case 1:xGui=0;pos.x=other();break;"));
    }
}
