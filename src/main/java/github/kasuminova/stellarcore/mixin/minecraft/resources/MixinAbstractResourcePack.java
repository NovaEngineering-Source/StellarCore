package github.kasuminova.stellarcore.mixin.minecraft.resources;

import github.kasuminova.stellarcore.client.resource.DirectoryPathIndex;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.mixin.util.StellarCoreAbstractResourcePackAccessor;
import github.kasuminova.stellarcore.mixin.util.StellarCoreResourcePack;
import net.minecraft.client.resources.AbstractResourcePack;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
    private final Map<ResourceLocation, Boolean> stellar_core$resourceExistsCache = new ConcurrentHashMap<>();

    @Unique
    private boolean stellar_core$cacheEnabled = false;

    @Unique
    private byte stellar_core$packFileKind = 0;

    /**
     * @author Kasumi_Nova
     * @reason Cache
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
        final Boolean cached = stellar_core$resourceExistsCache.get(location);
        if (cached != null) {
            cir.setReturnValue(cached);
            return;
        }

        final boolean computed;
        if (StellarCoreConfig.PERFORMANCE.vanilla.directoryResourcePackIndex && stellar_core$isDirectoryPack()) {
            final String namespace = location.getNamespace();
            final String path = location.getPath();
            if (namespace != null && !namespace.isEmpty() && path != null && !path.isEmpty()) {
                final File namespaceRoot = new File(this.resourcePackFile, "assets" + File.separatorChar + namespace);
                final Boolean indexed = DirectoryPathIndex.tryContains(namespaceRoot, path);
                if (indexed != null) {
                    computed = indexed;
                } else {
                    DirectoryPathIndex.prewarmAsync(namespaceRoot);
                    computed = this.hasResourceName(locationToName(location));
                }
            } else {
                computed = this.hasResourceName(locationToName(location));
            }
        } else {
            computed = this.hasResourceName(locationToName(location));
        }

        final Boolean existing = stellar_core$resourceExistsCache.putIfAbsent(location, computed);
        cir.setReturnValue(existing != null ? existing : computed);
    }

    @Unique
    private boolean stellar_core$isDirectoryPack() {
        final byte kind = stellar_core$packFileKind;
        if (kind != 0) {
            return kind == 1;
        }
        final boolean isDirectory = this.resourcePackFile != null && this.resourcePackFile.isDirectory();
        stellar_core$packFileKind = (byte) (isDirectory ? 1 : 2);
        return isDirectory;
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
    }

    @Override
    public void stellar_core$disableCache() {
        stellar_core$resourceExistsCache.clear();
        stellar_core$cacheEnabled = false;
    }

    @Override
    public File stellar_core$getResourcePackFile() {
        return this.resourcePackFile;
    }
}
