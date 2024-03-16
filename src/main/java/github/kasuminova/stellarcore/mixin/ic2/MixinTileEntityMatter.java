package github.kasuminova.stellarcore.mixin.ic2;

import github.kasuminova.stellarcore.mixin.util.ITaskExecutor;
import hellfirepvp.modularmachinery.ModularMachinery;
import ic2.core.block.machine.tileentity.TileEntityMatter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TileEntityMatter.class)
@SuppressWarnings("MethodMayBeStatic")
public class MixinTileEntityMatter {

    @Redirect(method = "updateEntityServer", at = @At(value = "INVOKE", target = "Lic2/core/block/machine/tileentity/TileEntityMatter;markDirty()V"))
    public void redirectMarkDirty(final TileEntityMatter instance) {
        ((ITaskExecutor) ModularMachinery.EXECUTE_MANAGER).addTEMarkTask(instance);
    }

}
