package com.gtamine.visual.mixin;

import com.gtamine.visual.serverlist.ServerListModule;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.network.ServerInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/** Fabric has no event for decorating a saved server's title in the list. */
@Mixin(MultiplayerServerListWidget.ServerEntry.class)
public abstract class ServerListEntryMixin {
    @Shadow @Final private ServerInfo server;
    @Unique private final ServerListModule gtamineVisual$decoration = new ServerListModule();

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V",
            ordinal = 0))
    private void gtamineVisual$renderTitle(DrawContext context, TextRenderer renderer, String title,
                                         int x, int y, int color, Operation<Void> original) {
        if (!gtamineVisual$decoration.supports(server)) {
            original.call(context, renderer, title, x, y, color);
            return;
        }
        var entry = (MultiplayerServerListWidget.ServerEntry) (Object) this;
        gtamineVisual$decoration.renderTitle(context, renderer, title, x, y, color,
                entry.getContentRightEnd(), server);
    }
}
