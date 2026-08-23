package net.minecraftforge.client.model;

import net.minecraft.util.ResourceLocation;

public class ModelLoaderRegistryR {

    public static IModel getMissingModel(ResourceLocation location, Throwable cause) {
        return ModelLoaderRegistry.getMissingModel(location, cause);
    }

    public static void addAlias(ResourceLocation from, ResourceLocation to) {
        ModelLoaderRegistry.addAlias(from, to);
    }

}
