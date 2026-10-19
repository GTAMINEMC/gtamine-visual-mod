package com.gtamine.visual.mixin;

import com.gtamine.visual.editor.InterfaceEditorModule;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fabric has no per-bossbar text transform event. Vanilla bars and packets stay untouched. */
@Mixin(BossBarHud.class)
public abstract class BossBarHudMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void gtamineVisual$beginFrame(DrawContext context, CallbackInfo ci) {
        var editor = InterfaceEditorModule.instance();
        if (editor != null) editor.beginFrame();
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)V"))
    private void gtamineVisual$renderText(DrawContext context, TextRenderer renderer, Text text, int x, int y, int color) {
        var editor = InterfaceEditorModule.instance();
        if (editor == null || !editor.render(context, renderer, text, x, y, color)) {
            context.drawTextWithShadow(renderer, text, x, y, color);
        }
    }
}
