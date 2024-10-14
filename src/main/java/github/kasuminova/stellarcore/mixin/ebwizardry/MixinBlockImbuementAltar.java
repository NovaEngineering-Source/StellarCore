package github.kasuminova.stellarcore.mixin.ebwizardry;

import com.llamalad7.mixinextras.sugar.Local;
import electroblob.wizardry.block.BlockImbuementAltar;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(BlockImbuementAltar.class)
public class MixinBlockImbuementAltar {

    @Inject(method = "onBlockActivated", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;copy()Lnet/minecraft/item/ItemStack;"), cancellable = true)
    private void injectOnBlockActivated(final World world, final BlockPos pos, final IBlockState block, final EntityPlayer player,
                                        final EnumHand hand, final EnumFacing side,
                                        final float hitX, final float hitY, final float hitZ,
                                        final CallbackInfoReturnable<Boolean> cir,
                                        @Local(name = "toInsert") final ItemStack toInsert)
    {
        if (!StellarCoreConfig.BUG_FIXES.ebWizardry.blockImbuementAltar) {
            return;
        }
        if (toInsert.isEmpty()) {
            cir.setReturnValue(true);
        }
    }

}
