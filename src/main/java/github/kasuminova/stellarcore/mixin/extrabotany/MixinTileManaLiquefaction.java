package github.kasuminova.stellarcore.mixin.extrabotany;

import com.meteor.extrabotany.common.block.fluid.ModFluid;
import com.meteor.extrabotany.common.block.tile.TileManaLiquefaction;
import com.meteor.extrabotany.common.core.config.ConfigHandler;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vazkii.botania.common.block.tile.TileMod;
import vazkii.botania.common.block.tile.mana.TileSpreader;

@Mixin(TileManaLiquefaction.class)
public abstract class MixinTileManaLiquefaction extends TileMod {

    @Shadow(remap = false)
    public abstract boolean isFull();

    @Shadow(remap = false)
    public int energy;

    @Shadow(remap = false)
    public abstract int getCurrentMana();

    @Shadow(remap = false)
    public abstract void recieveMana(final int mana);

    @Final
    @Shadow(remap = false) 
    @SuppressWarnings({"StaticVariableMayNotBeInitialized", "NonConstantFieldWithUpperCaseName"})
    private static int MAX_ENERGY;

    /**
     * @author Kasumi_Nova
     * @reason 修复液态魔力溢出。
     */
    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    public void update(final CallbackInfo ci) {
        if (!StellarCoreConfig.BUG_FIXES.extraBotany.tileManaLiquefaction) {
            return;
        }
        ci.cancel();

        // 极其具有迷惑性的变量。
        if (!ConfigHandler.DISABLE_MANALIQUEFICATION) {
            return;
        }

        int redstone = 0;
        EnumFacing[] facings = EnumFacing.VALUES;
        for (final EnumFacing facing : facings) {
            int redstoneSide = this.getWorld().getRedstonePower(this.getPos().offset(facing), facing);
            redstone = Math.max(redstone, redstoneSide);
        }

        for (final EnumFacing facing : facings) {
            BlockPos neighbor = this.getPos().offset(facing);
            if (!this.world.isBlockLoaded(neighbor)) {
                continue;
            }

            TileEntity te = this.world.getTileEntity(neighbor);
            if (te == null) {
                continue;
            }

            IFluidHandler storage = null;
            if (te.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, facing.getOpposite())) {
                storage = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, facing.getOpposite());
            } else if (te.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null)) {
                storage = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null);
            }

            if (storage != null) {
                if (redstone == 0) {
                    if (!this.isFull() && storage.drain(new FluidStack(ModFluid.fluidMana, 1), true) != null) {
                        ++this.energy;
                    }
                } else if (this.energy >= 25) {
                    this.energy -= storage.fill(new FluidStack(ModFluid.fluidMana, 25), true);
                }
            }

            int speed = ConfigHandler.MG_TRANSFERSPEED;
            if (te instanceof TileSpreader && redstone == 0) {
                @SuppressWarnings("PatternVariableCanBeUsed")
                TileSpreader spreader = (TileSpreader) te;
                if (this.getCurrentMana() >= speed && spreader.getCurrentMana() < spreader.getMaxMana()) {
                    int current = Math.min(speed, spreader.getMaxMana() - spreader.getCurrentMana());
                    spreader.recieveMana(current);
                    this.recieveMana(-current);
                }
            }
        }

        if (redstone == 0) {
            if (this.energy > 0 && this.getCurrentMana() <= 998000) {
                this.recieveMana(2000);
                this.energy -= 2;
            }
        } else if (this.getCurrentMana() >= 2000 && (this.energy + 2) <= MAX_ENERGY) {
            this.recieveMana(-2000);
            this.energy += 2;
        }
    }

}
