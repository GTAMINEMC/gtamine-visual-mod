package com.gtamine.visual.serverlist;

import com.gtamine.visual.server.ServerDomainPolicy;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;

/** Menu-only presentation module, independent of the connected world's modules. */
public final class ServerListModule {
    private static final String LABEL = "ɢᴛᴀᴍɪɴᴇ";
    private static final int HEIGHT = 10;
    private static final int GAP = 5;
    private String address;
    private boolean supported;

    public boolean supports(ServerInfo server) {
        if (!server.address.equals(address)) {
            address = server.address;
            supported = ServerDomainPolicy.allows(ServerAddress.parse(address).getAddress());
        }
        return supported;
    }

    /** Only shortens the displayed title; saved names and server status stay untouched. */
    public void renderTitle(DrawContext context, TextRenderer renderer, String title, int x, int y,
                            int color, int right, ServerInfo server) {
        Text status = server.getStatus() == ServerInfo.Status.INCOMPATIBLE
                ? server.version : server.playerCountLabel;
        int titleRight = right - 25 - renderer.getWidth(status);
        int badgeWidth = renderer.getWidth(LABEL) + 8;
        int available = Math.max(0, titleRight - x - badgeWidth - GAP);
        String displayed = title;
        if (renderer.getWidth(title) > available) {
            displayed = available >= renderer.getWidth("…")
                    ? renderer.trimToWidth(title, available - renderer.getWidth("…")) + "…" : "";
        }
        context.drawTextWithShadow(renderer, displayed, x, y, color);
        int badgeX = x + renderer.getWidth(displayed) + GAP;
        drawBadge(context, renderer, badgeX, y - 1, badgeWidth);
    }

    private static void drawBadge(DrawContext context, TextRenderer renderer, int x, int y, int width) {
        // Integer GUI pixels keep the four cut-off corners crisp at every GUI scale.
        for (int column = 0; column < width; column++) {
            int inset = column == 0 || column == width - 1 ? 1 : 0;
            float t = column / (float) (width - 1);
            int red = Math.round(74 + (42 - 74) * t);
            int green = Math.round(197 + (129 - 197) * t);
            int blue = Math.round(76 + (62 - 76) * t);
            context.fill(x + column, y + inset, x + column + 1, y + HEIGHT - inset,
                    0xFF000000 | red << 16 | green << 8 | blue);
        }
        context.drawTextWithShadow(renderer, LABEL, x + 4, y, 0xFFFFFFFF);
    }
}
