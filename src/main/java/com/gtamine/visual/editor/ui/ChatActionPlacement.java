package com.gtamine.visual.editor.ui;

import com.gtamine.visual.editor.geometry.Rect;
import java.util.List;

/** Right-aligned placement above the input, moving above any overlapping widgets. */
public final class ChatActionPlacement {
    private static final int GAP = 4;

    private ChatActionPlacement() { }

    public static Rect find(int screenWidth, int inputTop, int width, int height, List<Rect> obstacles) {
        int x = Math.max(GAP, screenWidth - width - GAP);
        int y = inputTop - height - GAP;
        while (y >= GAP) {
            Rect candidate = new Rect(x, y, width, height);
            int next = y;
            for (Rect obstacle : obstacles) {
                if (candidate.x() < obstacle.right() + GAP && candidate.right() + GAP > obstacle.x()
                        && candidate.y() < obstacle.bottom() + GAP && candidate.bottom() + GAP > obstacle.y()) {
                    next = Math.min(next, (int) Math.floor(obstacle.y()) - height - GAP);
                }
            }
            if (next == y) return candidate;
            y = next;
        }
        // A fully occupied screen has no safe click target.
        return null;
    }
}
