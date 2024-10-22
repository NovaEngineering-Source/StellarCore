package github.kasuminova.stellarcore.mixin.minecraft.forge.capability;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.capabilities.CapabilityDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(CapabilityDispatcher.class)
public class MixinCapabilityDispatcher {

    @Redirect(
            method = "serializeNBT()Lnet/minecraft/nbt/NBTTagCompound;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/nbt/NBTTagCompound;setTag(Ljava/lang/String;Lnet/minecraft/nbt/NBTBase;)V",
                    remap = true
            ),
            remap = false
    )
    private void redirectSerializeNBTSetTag(final NBTTagCompound instance, final String key, final NBTBase value) {
        if (value.isEmpty()) {
            return;
        }
        instance.setTag(key, value);
    }

}
