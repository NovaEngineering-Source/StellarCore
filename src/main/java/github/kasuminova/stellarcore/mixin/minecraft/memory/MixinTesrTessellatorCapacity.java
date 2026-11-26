package github.kasuminova.stellarcore.mixin.minecraft.memory;

import github.kasuminova.stellarcore.client.memory.RenderBufferCapacity;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(TileEntityRendererDispatcher.class)
public abstract class MixinTesrTessellatorCapacity {
    @ModifyArg(method = "<init>", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/Tessellator;<init>(I)V"), index = 0, require = 1)
    private int stellar_core$smallTesrBuffer(final int requested) {
        return RenderBufferCapacity.initialInts(requested);
    }
}
