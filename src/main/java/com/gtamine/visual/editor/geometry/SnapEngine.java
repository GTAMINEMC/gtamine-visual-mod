package com.gtamine.visual.editor.geometry;

import java.util.List;

/** GUI units, independent of physical display resolution. */
public final class SnapEngine {
    private static final double DISTANCE = 5;
    private SnapEngine() { }

    public record Result(Rect rect, Double verticalGuide, Double horizontalGuide) { }

    public static Result snap(Rect moving, List<Rect> others, double screenWidth, double screenHeight) {
        double bestX = DISTANCE + 1, bestY = DISTANCE + 1;
        Double guideX = null, guideY = null;
        double[] xs = {moving.x(), moving.x() + moving.width() / 2, moving.right()};
        double[] ys = {moving.y(), moving.y() + moving.height() / 2, moving.bottom()};
        var targets = new java.util.ArrayList<>(others);
        targets.add(new Rect(0, 0, screenWidth, screenHeight));
        for (Rect target : targets) {
            for (double x : xs) for (double tx : new double[]{target.x(), target.x() + target.width() / 2, target.right()}) {
                double delta = tx - x;
                if (Math.abs(delta) <= DISTANCE && Math.abs(delta) < Math.abs(bestX)) {
                    bestX = delta;
                    guideX = tx;
                }
            }
            for (double y : ys) for (double ty : new double[]{target.y(), target.y() + target.height() / 2, target.bottom()}) {
                double delta = ty - y;
                if (Math.abs(delta) <= DISTANCE && Math.abs(delta) < Math.abs(bestY)) {
                    bestY = delta;
                    guideY = ty;
                }
            }
        }
        double x = moving.x() + (guideX == null ? 0 : bestX);
        double y = moving.y() + (guideY == null ? 0 : bestY);
        x = Math.max(0, Math.min(x, Math.max(0, screenWidth - moving.width())));
        y = Math.max(0, Math.min(y, Math.max(0, screenHeight - moving.height())));
        return new Result(new Rect(x, y, moving.width(), moving.height()), guideX, guideY);
    }
}
