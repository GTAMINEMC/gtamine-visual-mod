package com.gtamine.visual.mixin;

import com.gtamine.visual.editor.nminimap.MinimapState;
import net.minecraft.client.render.MapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(MapRenderState.class)
public abstract class MapRenderStateMixin implements MinimapState {
    @Unique private int gtamineVisual$metadata = -1;
    @Override public int gtamineVisual$metadata() { return gtamineVisual$metadata; }
    @Override public void gtamineVisual$metadata(int metadata) { gtamineVisual$metadata = metadata; }
}
