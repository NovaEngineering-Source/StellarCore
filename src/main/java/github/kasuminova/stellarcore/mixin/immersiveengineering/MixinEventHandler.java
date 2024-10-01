package github.kasuminova.stellarcore.mixin.immersiveengineering;

import blusunrize.immersiveengineering.common.EventHandler;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Set;

@Mixin(value = EventHandler.class, remap = false)
public class MixinEventHandler {

    @SuppressWarnings("MethodMayBeStatic")
    @Redirect(method = "onWorldTick", at = @At(value = "INVOKE", target = "Ljava/util/Set;isEmpty()Z"))
    private boolean redirectOnWorldTickIsEmpty(final Set<TileEntity> instance, @Local(name = "dim") int dim) {
        if (instance.isEmpty()) {
            return true;
        }
        return !instance.stream().anyMatch(te -> te.getWorld().provider.getDimension() == dim);
    }

}
