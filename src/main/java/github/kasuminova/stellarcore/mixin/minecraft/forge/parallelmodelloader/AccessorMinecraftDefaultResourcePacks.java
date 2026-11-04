package github.kasuminova.stellarcore.mixin.minecraft.forge.parallelmodelloader;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourcePack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(Minecraft.class)
public interface AccessorMinecraftDefaultResourcePacks {

    @Accessor("defaultResourcePacks")
    List<IResourcePack> stellar_core$getDefaultResourcePacks();
}
