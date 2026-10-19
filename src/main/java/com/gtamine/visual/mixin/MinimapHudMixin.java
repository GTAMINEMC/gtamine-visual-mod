package com.gtamine.visual.mixin;

import com.gtamine.visual.editor.InterfaceEditorModule;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class MinimapHudMixin {
    @Inject(method = "renderBossBarHud", at = @At("TAIL"))
    private void gtamineVisual$drawMinimap(DrawContext context, RenderTickCounter counter, CallbackInfo ci) {
        var editor = InterfaceEditorModule.instance();
        if (editor != null) editor.renderNativeMinimap(context);
    }
}
