package github.kasuminova.stellarcore.mixin.minecraft.resources;

import com.llamalad7.mixinextras.sugar.Local;
import github.kasuminova.stellarcore.client.resource.DirectoryPathIndex;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import net.minecraft.client.resources.AbstractResourcePack;
import net.minecraft.client.resources.FolderResourcePack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.File;

@Mixin(FolderResourcePack.class)
public abstract class MixinFolderResourcePack extends AbstractResourcePack {

    public MixinFolderResourcePack(File resourcePackFileIn) {
        super(resourcePackFileIn);
    }

    @Redirect(
        method = "getFile",
        at = @At(value = "INVOKE", target = "Ljava/io/File;isFile()Z")
    )
    private boolean stellar_core$isIndexedFile(File file, @Local(argsOnly = true) String path) {
        if (!StellarCoreConfig.PERFORMANCE.vanilla.directoryResourcePackIndex) {
            return file.isFile();
        }
        return DirectoryPathIndex.contains(this.resourcePackFile, path, file);
    }

}
