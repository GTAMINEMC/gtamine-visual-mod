package com.gtamine.visual.editor.nminimap;

import com.gtamine.visual.editor.geometry.Rect;
import java.util.regex.Pattern;

/** Geometry from NMinimap's config.glsl and vertex_body.glsl, in scaled GUI units. */
public record MinimapLayout(double sizeX, double sizeY, double offsetX, double offsetY,
                            double content, double border) {
    private static final String N = "([0-9]+(?:\\.[0-9]+)?)";
    public static MinimapLayout parse(String config, String body) {
        if (!body.contains("0xFF0000") || !body.contains("0x597D27") || !body.contains("0x3737DC")
                || !body.contains("MAP_CROP_RADIUS") || !body.contains("MAP_BORDER_WIDTH")) {
            throw new IllegalArgumentException("Unsupported NMinimap shader");
        }
        double[] size = vector(config, "MAP_SIZE"), offset = vector(config, "MAP_OFFSET");
        double content = number(config, "#define\\s+MAP_CONTENT_SIZE\\s+" + N);
        double border = number(config, "#define\\s+MAP_BORDER_WIDTH\\s+min\\(\\s*" + N + "\\s*,\\s*MAP_CONTENT_SIZE\\s*\\*\\s*0\\.1\\s*\\)");
        if (content < 2 || content > 127 || size[0] <= 0 || size[1] <= 0 || size[0] > 4 || size[1] > 4
                || offset[0] > 2 || offset[1] > 2) throw new IllegalArgumentException("Invalid minimap geometry");
        return new MinimapLayout(size[0], size[1], offset[0], offset[1], content, Math.min(border, content * 0.1));
    }
    private static double[] vector(String source, String name) {
        var matcher = Pattern.compile("#define\\s+" + name + "\\s+vec2\\(\\s*" + N + "\\s*,\\s*" + N + "\\s*\\)").matcher(source);
        if (!matcher.find()) throw new IllegalArgumentException("Missing " + name);
        return new double[]{Double.parseDouble(matcher.group(1)), Double.parseDouble(matcher.group(2))};
    }
    private static double number(String source, String regex) {
        var matcher = Pattern.compile(regex).matcher(source);
        if (!matcher.find()) throw new IllegalArgumentException("Missing minimap constant");
        return Double.parseDouble(matcher.group(1));
    }

    public Rect bounds(int metadata, double width) {
        boolean right = metadata == 129 || metadata == 29;
        boolean round = metadata == 29 || metadata == 67;
        double minimum = 1 - border / 2;
        double maximum = content + (round ? 1 : 0) + border / 2;
        double originX = right ? width - width / 2 * (offsetX + sizeX * (content + 2) / 128) : width / 2 * offsetX;
        return new Rect(originX + width * sizeX * minimum / 256,
                width * offsetY / 2 + width * sizeY * minimum / 256,
                width * sizeX * (maximum - minimum) / 256, width * sizeY * (maximum - minimum) / 256);
    }

    public static int metadata(byte[] colors) {
        if (colors == null || colors.length != 128 * 128 || colors[0] != 18 || colors[1] != 4 || colors[2] != 49) return -1;
        int metadata = Byte.toUnsignedInt(colors[3]);
        return metadata == 17 || metadata == 129 || metadata == 29 || metadata == 67 ? metadata : -1;
    }
}
