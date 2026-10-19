package com.gtamine.visual.editor.nminimap;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Shader-independent composition; inputs are only server map pixels and marker resources. */
public final class MinimapRaster {
    public static final int SIZE = 256;
    private MinimapRaster() { }

    public static double padding(Map<Integer, MinimapSprite> sprites) {
        double max = 0;
        for (var sprite : sprites.values()) max = Math.max(max, Math.hypot(sprite.scaleX(), sprite.scaleY()));
        return Math.min(128, max + 2);
    }

    public static void compose(int[] output, int[] map, MinimapLayout layout, int metadata, double rotation,
                               List<MinimapMarker> markers, Map<Integer, MinimapSprite> sprites, double padding) {
        Arrays.fill(output, 0);
        boolean round = metadata == 29 || metadata == 67;
        double radius = layout.content() / 2, center = 1 + radius;
        double minimum = 1 - layout.border() / 2 - padding;
        double extent = layout.content() + (round ? 0 : -1) + layout.border() + 2 * padding;
        double step = extent / SIZE;
        double cosine = round ? Math.cos(rotation) : 1, sine = round ? Math.sin(rotation) : 0;
        double halfBorder = layout.border() / 2;
        for (int py = 0; py < SIZE; py++) {
            double y = minimum + (py + 0.5) * step;
            for (int px = 0; px < SIZE; px++) {
                double x = minimum + (px + 0.5) * step;
                double distance = round ? radius - Math.hypot(x - center, y - center)
                        : Math.min(Math.min(x - 1, layout.content() - x), Math.min(y - 1, layout.content() - y));
                if (distance < 0 && (halfBorder <= 0 || -distance >= halfBorder)) continue;
                double sourceX = center + cosine * (x - center) + sine * (y - center);
                double sourceY = center - sine * (x - center) + cosine * (y - center);
                int sx = (int)Math.floor(sourceX), sy = (int)Math.floor(sourceY);
                int color = sx >= 1 && sx < 128 && sy >= 1 && sy < 128 ? map[sy * 128 + sx] : 0;
                if (halfBorder > 0 && distance < halfBorder) {
                    double t = Math.max(0, Math.min(1, Math.abs(distance) / halfBorder));
                    double opacity = 0.7 * (1 - t * t * (3 - 2 * t));
                    int border = ((int)Math.round(opacity * 255) << 24) | 0x111111;
                    color = blend(distance < 0 ? 0 : color, border);
                }
                output[py * SIZE + px] = color;
            }
        }
        for (var marker : markers) {
            var sprite = sprites.get(marker.codepoint());
            if (sprite == null) continue;
            double x = ((marker.color() >>> 16) & 255) / 255.0 * 128;
            double y = ((marker.color() >>> 8) & 255) / 255.0 * 128;
            if (round) {
                x -= 64; y -= 64;
                double length = Math.hypot(x, y);
                if (length > radius) { x *= radius / length; y *= radius / length; }
                double rx = cosine * x - sine * y;
                y = sine * x + cosine * y + center;
                x = rx + center;
            } else {
                x = Math.max(1, Math.min(layout.content(), x));
                y = Math.max(1, Math.min(layout.content(), y));
            }
            double angle = (marker.color() & 255) / 255.0 * Math.PI * 2 + (round ? rotation : 0);
            double cos = Math.cos(angle), sin = Math.sin(angle);
            double halfWidth = sprite.scaleX(), halfHeight = sprite.scaleY();
            double reach = Math.hypot(halfWidth, halfHeight);
            int left = Math.max(0, (int)Math.floor((x - reach - minimum) / step));
            int right = Math.min(SIZE - 1, (int)Math.ceil((x + reach - minimum) / step));
            int top = Math.max(0, (int)Math.floor((y - reach - minimum) / step));
            int bottom = Math.min(SIZE - 1, (int)Math.ceil((y + reach - minimum) / step));
            for (int py = top; py <= bottom; py++) for (int px = left; px <= right; px++) {
                double dx = minimum + (px + 0.5) * step - x, dy = minimum + (py + 0.5) * step - y;
                double u = (cos * dx + sin * dy) / (2 * halfWidth) + 0.5;
                double v = (-sin * dx + cos * dy) / (2 * halfHeight) + 0.5;
                if (u >= 0 && u < 1 && v >= 0 && v < 1) {
                    int pixel = sprite.pixels()[(int)(v * sprite.height()) * sprite.width() + (int)(u * sprite.width())];
                    int index = py * SIZE + px;
                    output[index] = blend(output[index], pixel);
                }
            }
        }
    }

    public static int blend(int background, int foreground) {
        int alpha = foreground >>> 24;
        if (alpha == 0) return background;
        if (alpha == 255) return foreground;
        int backgroundAlpha = background >>> 24;
        int combined = alpha + backgroundAlpha * (255 - alpha) / 255;
        if (combined == 0) return 0;
        return (combined << 24) | (channel(background, foreground, 16, alpha, backgroundAlpha, combined) << 16)
                | (channel(background, foreground, 8, alpha, backgroundAlpha, combined) << 8)
                | channel(background, foreground, 0, alpha, backgroundAlpha, combined);
    }

    private static int channel(int background, int foreground, int shift, int alpha, int backgroundAlpha, int combined) {
        return (((foreground >>> shift) & 255) * alpha
                + ((background >>> shift) & 255) * backgroundAlpha * (255 - alpha) / 255) / combined;
    }
}
