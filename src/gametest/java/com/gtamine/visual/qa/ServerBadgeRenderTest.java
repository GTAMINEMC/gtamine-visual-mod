package com.gtamine.visual.qa;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.text.Text;
import java.nio.file.Files;

/** Real server entries without network pings or changes to the saved server list. */
public final class ServerBadgeRenderTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        int[] pixel = new int[2];
        context.runOnClient(client -> client.setScreen(new Screen(Text.literal("Server badge QA")) {
            @Override protected void init() {
                var servers = new ServerList(client);
                var owner = new MultiplayerScreen(new TitleScreen()) {
                    @Override public ServerList getServerList() { return servers; }
                };
                var list = new MultiplayerServerListWidget(owner, client, width, height - 40, 20, 36);
                servers.add(server("RolePlay", "mc.gtamine.com:25565"), false);
                servers.add(server("A very long server name that must leave room for the badge and status", "gtamine.ru"), false);
                servers.add(server("Other server", "example.com"), false);
                list.setServers(servers);
                addDrawableChild(list);
                var entry = list.children().getFirst();
                pixel[0] = entry.getContentX() + 35 + client.textRenderer.getWidth("RolePlay") + 5;
                pixel[1] = entry.getContentY();
            }

            private ServerInfo server(String name, String address) {
                var server = new ServerInfo(name, address, ServerInfo.ServerType.OTHER);
                server.setStatus(ServerInfo.Status.UNREACHABLE);
                server.label = Text.literal("Test server description");
                server.playerCountLabel = Text.literal("42/100");
                return server;
            }
        }));
        context.waitTicks(5);
        var screenshot = context.takeScreenshot("gtamine-server-badges");
        double scale = context.computeOnClient(client -> client.getWindow().getScaleFactor());
        try (var input = Files.newInputStream(screenshot); var image = NativeImage.read(input)) {
            int actual = image.getColorArgb((int) (pixel[0] * scale), (int) ((pixel[1] + 1) * scale));
            if (actual != 0xFF4AC54C) throw new AssertionError("Server badge missing: " + Integer.toHexString(actual));
        } catch (Exception ex) { throw new RuntimeException(ex); }
        context.runOnClient(client -> client.setScreen(new TitleScreen()));
    }
}
