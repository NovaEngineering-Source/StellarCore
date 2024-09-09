package github.kasuminova.stellarcore.mixin.ancientspellcraft;

import com.windanesz.ancientspellcraft.client.entity.ASFakePlayer;
import github.kasuminova.stellarcore.StellarCore;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.util.StellarLog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Constructor;

@SuppressWarnings({"StaticVariableMayNotBeInitialized", "NonConstantFieldWithUpperCaseName"})
@Mixin(value = ASFakePlayer.class, remap = false)
public class MixinASFakePlayer {

    @Shadow
    public static ASFakePlayer FAKE_PLAYER;

    @Inject(method = "<clinit>", at = @At("HEAD"), cancellable = true)
    private static void injectCLInit(final CallbackInfo ci) {
        if (!StellarCoreConfig.FEATURES.vanilla.handleClientWorldLoad) {
            return;
        }

        EntityPlayerSP player = Minecraft.getMinecraft().player;
        try {
            Constructor<ASFakePlayer> constructor = ASFakePlayer.class.getDeclaredConstructor(World.class);
            constructor.setAccessible(true);
            FAKE_PLAYER = constructor.newInstance(player == null ? null : player.world);
        } catch (Throwable e) {
            StellarLog.LOG.error("[StellarCore] Failed to create ASFakePlayer instance!", e);
        }
        ci.cancel();
    }

}
