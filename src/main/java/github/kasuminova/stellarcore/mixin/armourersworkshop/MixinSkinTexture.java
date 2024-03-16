package github.kasuminova.stellarcore.mixin.armourersworkshop;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import moe.plushie.armourers_workshop.common.skin.data.SkinTexture;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SkinTexture.class)
public abstract class MixinSkinTexture {

    @Shadow(remap = false) protected abstract void deleteTexture();

    @Shadow(remap = false) private int textureId;

    @Redirect(method = "finalize",
            at = @At(
                    value = "INVOKE",
                    target = "Lmoe/plushie/armourers_workshop/common/skin/data/SkinTexture;deleteTexture()V",
                    remap = false),
            remap = false)
    private void onDeleteTexture(final SkinTexture instance) {
        if (!StellarCoreConfig.BUG_FIXES.armourersWorkshop.skinTexture) {
            deleteTexture();
            return;
        }
        if (textureId != -1) {
            Minecraft.getMinecraft().addScheduledTask(this::deleteTexture);
        }
    }

}
