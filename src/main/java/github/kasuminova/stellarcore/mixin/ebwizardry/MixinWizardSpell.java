package github.kasuminova.stellarcore.mixin.ebwizardry;

import electroblob.wizardry.loot.WizardSpell;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(WizardSpell.class)
public class MixinWizardSpell {

    @Redirect(
            method = "apply",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/apache/logging/log4j/Logger;warn(Ljava/lang/String;)V",
                    ordinal = 2,
                    remap = false
            )
    )
    private void injectApply(final Logger instance, final String s) {
        if (!StellarCoreConfig.FEATURES.ebwizardry.preventWizardSpellLogSpam) {
            instance.warn(s);
        }
    }

}
