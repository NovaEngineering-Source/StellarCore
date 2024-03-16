package github.kasuminova.stellarcore.mixin.mets;

import net.lrsoft.mets.manager.EnchantmentManager;
import net.minecraft.enchantment.Enchantment;
import net.minecraftforge.event.RegistryEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 移除节能附魔，实际作用接近无。
 */
@Mixin(EnchantmentManager.class)
public class MixinEnchantmentManager {

    @Inject(method = "onEnchantmentInit", at = @At("HEAD"), cancellable = true, remap = false)
    private static void removeEnchantment(final RegistryEvent.Register<Enchantment> event, final CallbackInfo ci) {
        ci.cancel();
    }

}
