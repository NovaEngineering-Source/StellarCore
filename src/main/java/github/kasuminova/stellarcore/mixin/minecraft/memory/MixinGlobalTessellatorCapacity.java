package github.kasuminova.stellarcore.mixin.minecraft.memory;

import github.kasuminova.stellarcore.client.memory.RenderBufferCapacity;
import net.minecraft.client.renderer.Tessellator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Tessellator.class)
public abstract class MixinGlobalTessellatorCapacity {
    @ModifyArg(method = "<clinit>", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/Tessellator;<init>(I)V"), index = 0, require = 1)
    private static int stellar_core$smallGlobalBuffer(final int requested) {
        return RenderBufferCapacity.initialInts(requested);
    }
}
