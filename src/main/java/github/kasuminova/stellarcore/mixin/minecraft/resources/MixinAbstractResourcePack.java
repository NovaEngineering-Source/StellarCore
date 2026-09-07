package github.kasuminova.stellarcore.mixin.minecraft.resources;

import github.kasuminova.stellarcore.client.resource.DirectoryPathIndex;
import github.kasuminova.stellarcore.client.resource.ZipEntryIndex;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.mixin.util.StellarCoreAbstractResourcePackAccessor;
import github.kasuminova.stellarcore.mixin.util.StellarCoreFileResourcePackAccessor;
import github.kasuminova.stellarcore.mixin.util.StellarCoreResourcePack;
import github.kasuminova.stellarcore.shaded.org.jctools.maps.NonBlockingHashSet;
import net.minecraft.client.resources.AbstractResourcePack;
import net.minecraft.client.resources.FolderResourcePack;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;

@Mixin(AbstractResourcePack.class)
public abstract class MixinAbstractResourcePack implements StellarCoreResourcePack, StellarCoreAbstractResourcePackAccessor {

    @Shadow
    @Final
    protected File resourcePackFile;

    @Shadow
    protected abstract boolean hasResourceName(final String name);

    @Shadow
    private static String locationToName(final ResourceLocation location) {
        return null;
    }

    @Unique
    private final NonBlockingHashSet<ResourceLocation> stellar_core$resourceExistsCache = new NonBlockingHashSet<>();

    @Unique
    private volatile boolean stellar_core$cacheEnabled = false;

    @Unique
    private byte stellar_core$packFileKind;

    /**
     * @author Kasumi_Nova
     * @reason Cache. Only positive results are cached: a probe failing inside the concurrent
     * lazy ZipFile initialization of FileResourcePack must not poison later lookups, misses
     * are simply re-probed on the next call.
     */
    @Inject(method = "resourceExists", at = @At("HEAD"), cancellable = true)
    public void injectResourceExists(final ResourceLocation location, final CallbackInfoReturnable<Boolean> cir) {
        if (!stellar_core$cacheEnabled) {
            return;
        }
        if (location == null) {
            cir.setReturnValue(false);
            return;
        }
        if (stellar_core$resourceExistsCache.contains(location)) {
            cir.setReturnValue(true);
            return;
        }

        final boolean exists;
        try {
            exists = this.hasResourceName(locationToName(location));
        } catch (Throwable probeFailure) {
            return;
        }
        if (exists) {
            stellar_core$resourceExistsCache.add(location);
        }
        cir.setReturnValue(exists);
    }

    @Unique
    private boolean stellar_core$isDirectoryPack() {
        final byte kind = stellar_core$packFileKind;
        if (kind != 0) {
            return kind == 1;
        }
        // A FolderResourcePack is directory-backed even when its root file points elsewhere
        // (e.g. a pack.mcmeta file); it must stay mutable so new files are never missed.
        final boolean directory = (this.resourcePackFile != null && this.resourcePackFile.isDirectory())
            || FolderResourcePack.class.isInstance(this);
        stellar_core$packFileKind = (byte) (directory ? 1 : 2);
        return directory;
    }

    @Unique
    @Override
    public void stellar_core$onReload() {
        if (!stellar_core$cacheEnabled) {
            return;
        }
        stellar_core$resourceExistsCache.clear();
    }

    @Override
    public void stellar_core$enableCache() {
        stellar_core$cacheEnabled = true;
        if (stellar_core$isDirectoryPack()) {
            if (StellarCoreConfig.PERFORMANCE.vanilla.directoryResourcePackIndex) {
                DirectoryPathIndex.prewarmAsync(this.resourcePackFile);
            }
            return;
        }
        // Force FileResourcePack's lazy ZipFile open on the single reload thread,
        // closing the concurrent-initialization race window on the worker threads.
        this.hasResourceName("pack.mcmeta");
        if (StellarCoreConfig.PERFORMANCE.vanilla.archiveResourcePackIndex
            && this instanceof StellarCoreFileResourcePackAccessor accessor) {
            ZipEntryIndex.prewarmAsync(this.resourcePackFile, accessor::stellar_core$getResourcePackZipFile);
        }
    }

    @Override
    public void stellar_core$disableCache() {
        stellar_core$resourceExistsCache.clear();
        stellar_core$cacheEnabled = false;
    }

    @Override
    public boolean stellar_core$isMutableResourcePack() {
        return stellar_core$isDirectoryPack();
    }

    @Override
    public File stellar_core$getResourcePackFile() {
        return this.resourcePackFile;
    }
}
