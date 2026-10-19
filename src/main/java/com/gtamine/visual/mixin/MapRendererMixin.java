package com.gtamine.visual.mixin;

import com.gtamine.visual.editor.InterfaceEditorModule;
import com.gtamine.visual.editor.nminimap.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.render.MapRenderer;
import net.minecraft.client.render.MapRenderState;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.map.MapState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MapRenderer.class)
public abstract class MapRendererMixin {
    @Inject(method = "update", at = @At("TAIL"))
    private void gtamineVisual$identify(MapIdComponent id, MapState source, MapRenderState state, CallbackInfo ci) {
        int metadata = MinimapLayout.metadata(source.colors);
        ((MinimapState)state).gtamineVisual$metadata(metadata);
        var editor = InterfaceEditorModule.instance();
        if (editor != null && metadata >= 0) editor.captureMinimap(source.colors);
    }

    @WrapMethod(method = "draw")
    private void gtamineVisual$moveMap(MapRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue,
                                      boolean decorations, int light, Operation<Void> original) {
        var editor = InterfaceEditorModule.instance();
        int metadata = ((MinimapState)state).gtamineVisual$metadata();
        if (editor != null && metadata >= 0 && editor.takeOverMinimap(metadata)) return;
        original.call(state, matrices, queue, decorations, light);
    }
}
