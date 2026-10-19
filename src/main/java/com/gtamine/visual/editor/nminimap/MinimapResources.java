package com.gtamine.visual.editor.nminimap;

import com.google.gson.JsonParser;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import java.util.HashMap;
import java.util.Map;

public final class MinimapResources {
    private MinimapResources() { }
    public static Map<Integer, MinimapSprite> load(ResourceManager resources) {
        Map<Integer, MinimapSprite> sprites = new HashMap<>();
        var font = resources.getResource(Identifier.of("nminimap", "font/default.json"));
        if (font.isEmpty()) return Map.of();
        try (var reader = font.get().getReader()) {
            var providers = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("providers");
            for (var entry : providers) {
                var provider = entry.getAsJsonObject();
                if (!provider.has("type") || !provider.get("type").getAsString().equals("bitmap")) continue;
                var chars = provider.getAsJsonArray("chars");
                if (chars.size() != 1) continue;
                int[] codepoints = chars.get(0).getAsString().codePoints().toArray();
                if (codepoints.length != 1) continue;
                Identifier textureId = Identifier.of(provider.get("file").getAsString()).withPrefixedPath("textures/");
                try (var stream = resources.open(textureId); var image = NativeImage.read(stream)) {
                    int width = image.getWidth(), height = image.getHeight();
                    if (width < 3 || height < 3 || width > 256 || height > 256) continue;
                    int metadata = image.getColorArgb(0, 0);
                    if ((metadata >>> 24) != 3 || (metadata & 0xffff) != 0x0100) continue;
                    int size = image.getColorArgb(0, 1);
                    int sx = (size >>> 16) & 255, sy = (size >>> 8) & 255;
                    if (sx == 0 || sy == 0 || sx > 64 || sy > 64) continue;
                    int[] pixels = new int[width * height];
                    for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
                        int pixel = image.getColorArgb(x, y);
                        pixels[y * width + x] = (pixel >>> 24) < 26 ? 0 : pixel;
                    }
                    sprites.put(codepoints[0], new MinimapSprite(width, height, sx, sy, pixels));
                }
            }
        } catch (Exception ex) {
            org.slf4j.LoggerFactory.getLogger("gtamine_visual").warn("Cannot read NMinimap marker resources", ex);
        }
        return Map.copyOf(sprites);
    }
}
