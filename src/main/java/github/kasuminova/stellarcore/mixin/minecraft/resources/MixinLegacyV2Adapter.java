package github.kasuminova.stellarcore.mixin.minecraft.resources;

import github.kasuminova.stellarcore.mixin.util.StellarCoreAbstractResourcePackAccessor;
import github.kasuminova.stellarcore.mixin.util.StellarCoreFileResourcePackAccessor;
import github.kasuminova.stellarcore.mixin.util.StellarCoreResourcePack;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.LegacyV2Adapter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.zip.ZipFile;

@Mixin(LegacyV2Adapter.class)
public abstract class MixinLegacyV2Adapter
    implements StellarCoreResourcePack, StellarCoreAbstractResourcePackAccessor, StellarCoreFileResourcePackAccessor {

    @Final
    @Shadow
    private IResourcePack pack;

    @Override
    public void stellar_core$onReload() {
        if (this.pack instanceof StellarCoreResourcePack wrapped) {
            wrapped.stellar_core$onReload();
        }
    }

    @Override
    public void stellar_core$enableCache() {
        if (this.pack instanceof StellarCoreResourcePack wrapped) {
            wrapped.stellar_core$enableCache();
        }
    }

    @Override
    public void stellar_core$disableCache() {
        if (this.pack instanceof StellarCoreResourcePack wrapped) {
            wrapped.stellar_core$disableCache();
        }
    }

    @Override
    public boolean stellar_core$isMutableResourcePack() {
        if (this.pack instanceof StellarCoreResourcePack wrapped) {
            return wrapped.stellar_core$isMutableResourcePack();
        }
        return false;
    }

    @Override
    public boolean stellar_core$isPersistent() {
        return this.pack instanceof StellarCoreResourcePack wrapped && wrapped.stellar_core$isPersistent();
    }

    /**
     * The wrapped pack's root is this adapter's root as well: it serves the same entries, and the
     * path rewriting never crosses an archive or a namespace boundary.
     */
    @Nullable
    @Override
    public File stellar_core$getResourcePackFile() {
        return this.pack instanceof StellarCoreAbstractResourcePackAccessor wrapped
            ? wrapped.stellar_core$getResourcePackFile()
            : null;
    }

    @Nullable
    @Override
    public ZipFile stellar_core$getResourcePackZipFile() throws IOException {
        return this.pack instanceof StellarCoreFileResourcePackAccessor wrapped
            ? wrapped.stellar_core$getResourcePackZipFile()
            : null;
    }

}
