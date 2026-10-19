package com.gtamine.visual.qa;

import com.gtamine.visual.editor.geometry.Rect;
import com.gtamine.visual.editor.nminimap.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.block.MapColor;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Identifier;
import java.nio.file.Files;
import java.util.*;

/** Exercises the real GUI/GPU path independently of world shaders and live server access. */
public final class MinimapRenderTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            context.waitTicks(20);
            if (Boolean.getBoolean("gtamine.qa.requireShaders")) {
                context.runOnClient(client -> {
                    Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                    Object instance = api.getMethod("getInstance").invoke(null);
                    if (!(boolean)api.getMethod("isShaderPackInUse").invoke(instance)) throw new AssertionError("Iris shader pack is not active");
                });
            }
            NativeMinimapRenderer[] renderer = new NativeMinimapRenderer[1];
            context.runOnClient(client -> {
                renderer[0] = new NativeMinimapRenderer();
                renderer[0].resources(Map.of());
                byte[] map = new byte[128 * 128];
                Arrays.fill(map, MapColor.MAGENTA.getRenderColorByte(MapColor.Brightness.HIGH));
                map[0] = 18; map[1] = 4; map[2] = 49; map[3] = 17;
                renderer[0].captureMap(map);
                HudElementRegistry.addLast(Identifier.of("gtamine_visual", "qa_map"), (draw, ticks) -> {
                    renderer[0].draw(draw, new MinimapLayout(.364, .364, .05, .05, 64, 0), 17,
                            new Rect(20, 20, 100, 100), 0);
                });
            });
            context.waitTicks(10);
            var screenshot = context.takeScreenshot("native-minimap-shader-compatibility");
            double scale = context.computeOnClient(client -> client.getWindow().getScaleFactor());
            try (var input = Files.newInputStream(screenshot); var image = NativeImage.read(input)) {
                int expected = MapColor.MAGENTA.getRenderColor(MapColor.Brightness.HIGH);
                int actual = image.getColorArgb((int)(50 * scale), (int)(50 * scale));
                if (expected != actual) throw new AssertionError("GUI map pixel changed under shaders: expected "
                        + Integer.toHexString(expected) + ", got " + Integer.toHexString(actual));
            }
            context.runOnClient(client -> {
                HudElementRegistry.removeElement(Identifier.of("gtamine_visual", "qa_map"));
                renderer[0].reset();
            });
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }
}
