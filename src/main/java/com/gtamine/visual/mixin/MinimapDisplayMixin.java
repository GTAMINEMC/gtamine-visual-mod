package com.gtamine.visual.mixin;

import com.gtamine.visual.editor.InterfaceEditorModule;
import net.minecraft.client.render.entity.DisplayEntityRenderer;
import net.minecraft.client.render.entity.state.TextDisplayEntityRenderState;
import net.minecraft.entity.decoration.DisplayEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DisplayEntityRenderer.TextDisplayEntityRenderer.class)
public abstract class MinimapDisplayMixin {
    @Inject(method = "updateRenderState(Lnet/minecraft/entity/decoration/DisplayEntity$TextDisplayEntity;Lnet/minecraft/client/render/entity/state/TextDisplayEntityRenderState;F)V", at = @At("TAIL"))
    private void gtamineVisual$captureMarkers(DisplayEntity.TextDisplayEntity entity,
                                               TextDisplayEntityRenderState state, float delta, CallbackInfo ci) {
        var editor = InterfaceEditorModule.instance();
        if (editor != null) editor.captureMinimapMarkers(entity.getId(), entity.getText());
    }
}
