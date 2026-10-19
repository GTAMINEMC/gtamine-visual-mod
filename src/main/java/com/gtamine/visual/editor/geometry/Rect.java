package com.gtamine.visual.editor.geometry;

public record Rect(double x, double y, double width, double height) {
    public double right() { return x + width; }
    public double bottom() { return y + height; }
    public Rect move(double dx, double dy) { return new Rect(x + dx, y + dy, width, height); }
    public boolean contains(double px, double py) {
        return px >= x && py >= y && px <= right() && py <= bottom();
    }
    public boolean near(Rect other, double gap) {
        return x <= other.right() + gap && right() + gap >= other.x
                && y <= other.bottom() + gap && bottom() + gap >= other.y;
    }
    public Rect union(Rect other) {
        double left = Math.min(x, other.x), top = Math.min(y, other.y);
        return new Rect(left, top, Math.max(right(), other.right()) - left,
                Math.max(bottom(), other.bottom()) - top);
    }
}
