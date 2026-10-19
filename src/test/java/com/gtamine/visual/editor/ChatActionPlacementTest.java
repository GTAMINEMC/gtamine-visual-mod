package com.gtamine.visual.editor;

import com.gtamine.visual.editor.geometry.Rect;
import com.gtamine.visual.editor.ui.ChatActionPlacement;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ChatActionPlacementTest {
    @Test void staysAboveInputAtRightEdge() {
        assertEquals(new Rect(216, 178, 180, 18),
                ChatActionPlacement.find(400, 200, 180, 18, List.of()));
    }

    @Test void clearsStackedButtonsAndReturnsWhenTheyDisappear() {
        var obstacles = List.of(new Rect(350, 176, 46, 20), new Rect(300, 150, 96, 20));
        assertEquals(new Rect(216, 128, 180, 18),
                ChatActionPlacement.find(400, 200, 180, 18, obstacles));
        assertEquals(178, ChatActionPlacement.find(400, 200, 180, 18, List.of()).y());
    }

    @Test void ignoresWidgetsOnOtherSideAndNeverOverlapsFullyOccupiedArea() {
        assertEquals(178, ChatActionPlacement.find(400, 200, 180, 18,
                List.of(new Rect(4, 176, 40, 20))).y());
        assertNull(ChatActionPlacement.find(320, 60, 180, 18,
                List.of(new Rect(0, 0, 320, 60))));
    }
}
