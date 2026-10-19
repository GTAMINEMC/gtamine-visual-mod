package com.gtamine.visual.mixin;

import com.gtamine.visual.editor.InterfaceEditorModule;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.client.render.VertexConsumer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(targets = "net.minecraft.client.font.BakedGlyphImpl$BakedGlyphRect")
public abstract class MinimapMarkerMixin {
    @Shadow @Final private Style style;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void gtamineVisual$hideMarkers(Matrix4f matrix, VertexConsumer consumer, int light,
                                           boolean noDepth, CallbackInfo ci) {
        var editor = InterfaceEditorModule.instance();
        if (editor != null && editor.nativeMinimapEnabled()
                && style.getFont() instanceof StyleSpriteSource.Font font
                && font.id().getNamespace().equals("nminimap")) ci.cancel();
    }

}
