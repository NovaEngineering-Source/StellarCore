package github.kasuminova.stellarcore.mixin.minecraft.forge.parallelmodelloader;

import net.minecraftforge.client.model.ForgeBlockStateV1;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = ForgeBlockStateV1.Variant.class, remap = false)
public interface AccessorForgeBlockStateV1Variant {

    @Invoker
    boolean invokeIsVanillaCompatible();

}
