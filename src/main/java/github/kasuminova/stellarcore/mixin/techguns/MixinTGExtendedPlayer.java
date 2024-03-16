package github.kasuminova.stellarcore.mixin.techguns;

import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import techguns.capabilities.TGExtendedPlayer;

@Mixin(TGExtendedPlayer.class)
public class MixinTGExtendedPlayer {

    @Shadow(remap = false) public boolean enableSafemode;

    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void onInit(final EntityPlayer entity, final CallbackInfo ci) {
        enableSafemode = true;
    }

}
