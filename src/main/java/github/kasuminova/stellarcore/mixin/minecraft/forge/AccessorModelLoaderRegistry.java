package github.kasuminova.stellarcore.mixin.minecraft.forge;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = ModelLoaderRegistry.class, remap = false)
public interface AccessorModelLoaderRegistry {

    @Invoker("getMissingModel")
    static IModel stellar_core$getMissingModel(ResourceLocation location, Throwable cause) {
        return null;
    }

    @Invoker("addAlias")
    static void stellar_core$addAlias(ResourceLocation from, ResourceLocation to) {

    }

}
