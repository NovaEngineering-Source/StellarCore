package github.kasuminova.stellarcore.mixin.athenaeum;

import com.codetaylor.mc.athenaeum.util.EnchantmentHelper;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EnchantmentHelper.class)
public class MixinEnchantmentHelper {

    /**
     * 返回值绝对不可能小于 0，唯一的可能是数值溢出。
     * 修复玩家经验等级大于 15466 时经验溢出导致无法完成配方的问题。
     */
    @ModifyReturnValue(method = "getPlayerExperienceTotal", at = @At("RETURN"), remap = false)
    private static int checkGetPlayerExperienceTotalRange(int value) {
        return value < 0 ? Integer.MAX_VALUE : value;
    }

}
