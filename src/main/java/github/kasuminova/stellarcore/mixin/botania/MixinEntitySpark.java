package github.kasuminova.stellarcore.mixin.botania;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vazkii.botania.api.mana.spark.ISparkAttachable;
import vazkii.botania.common.entity.EntitySpark;

@Mixin(EntitySpark.class)
public abstract class MixinEntitySpark {

    @Unique private boolean stellarcore$receiveLeastOne = false;
    @Unique private int stellarcore$failureCounter = 1;

    @Inject(
            method = "onUpdate",
            at = @At(
                    value = "INVOKE",
                    target = "Lvazkii/botania/common/entity/EntitySpark;getUpgrade()Lvazkii/botania/api/mana/spark/SparkUpgradeType;",
                    remap = false
            ),
            cancellable = true
    )
    private void injectOnUpdatePre(final CallbackInfo ci) {
        if (!StellarCoreConfig.PERFORMANCE.botania.sparkImprovements) {
            return;
        }
        if (((Entity) (Object) this).world.getTotalWorldTime() % stellarcore$failureCounter != 0) {
            ci.cancel();
        }
    }

    @Redirect(method = "onUpdate", at = @At(value = "INVOKE", target = "Lvazkii/botania/api/mana/spark/ISparkAttachable;recieveMana(I)V", remap = false))
    private void redirectReceiveMana(final ISparkAttachable attachable, final int mana) {
        attachable.recieveMana(mana);
        if (!StellarCoreConfig.PERFORMANCE.botania.sparkImprovements) {
            return;
        }
        if (mana != 0) {
            stellarcore$receiveLeastOne = true;
        }
    }

    @Inject(method = "onUpdate", at = @At("TAIL"))
    private void injectOnUpdateTail(final CallbackInfo ci) {
        if (!StellarCoreConfig.PERFORMANCE.botania.sparkImprovements) {
            return;
        }
        if (stellarcore$receiveLeastOne) {
            if (stellarcore$failureCounter > 1) {
                stellarcore$failureCounter--;
            }
        } else if (stellarcore$failureCounter < StellarCoreConfig.PERFORMANCE.botania.sparkMaxWorkDelay) {
            stellarcore$failureCounter++;
        }
    }

}
