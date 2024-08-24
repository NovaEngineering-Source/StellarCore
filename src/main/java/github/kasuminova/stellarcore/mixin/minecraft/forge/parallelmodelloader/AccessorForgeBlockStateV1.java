package github.kasuminova.stellarcore.mixin.minecraft.forge.parallelmodelloader;

import com.google.common.collect.Multimap;
import net.minecraftforge.client.model.ForgeBlockStateV1;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ForgeBlockStateV1.class, remap = false)
public interface AccessorForgeBlockStateV1 {

    @Accessor
    Multimap<String, ForgeBlockStateV1.Variant> getVariants();

}
