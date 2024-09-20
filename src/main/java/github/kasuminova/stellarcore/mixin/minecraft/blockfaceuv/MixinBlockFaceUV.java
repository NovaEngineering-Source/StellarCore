package github.kasuminova.stellarcore.mixin.minecraft.blockfaceuv;

import github.kasuminova.stellarcore.client.pool.BlockFaceUVsPool;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockFaceUV.class)
public class MixinBlockFaceUV {

    @Shadow
    public float[] uvs;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void injectInit(final float[] uvsIn, final int rotationIn, final CallbackInfo ci) {
        if (this.uvs != null) {
            BlockFaceUVsPool.INSTANCE.canonicalizeAsync(uvsIn, (canonicalizeUVs) -> this.uvs = canonicalizeUVs);
        }
    }

    @Inject(method = "setUvs", at = @At("HEAD"))
    private void injectSetUV(final float[] uvsIn, final CallbackInfo ci) {
        if (this.uvs == null) {
            BlockFaceUVsPool.INSTANCE.canonicalizeAsync(uvsIn, (canonicalizeUVs) -> this.uvs = canonicalizeUVs);
        }
    }

}
