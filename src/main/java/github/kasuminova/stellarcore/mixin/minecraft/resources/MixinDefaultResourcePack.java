package github.kasuminova.stellarcore.mixin.minecraft.resources;

import github.kasuminova.stellarcore.client.resource.ClasspathAssetIndex;
import github.kasuminova.stellarcore.mixin.util.StellarCoreResourcePack;
import net.minecraft.client.resources.DefaultResourcePack;
import net.minecraft.client.resources.ResourceIndex;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(DefaultResourcePack.class)
public abstract class MixinDefaultResourcePack implements StellarCoreResourcePack {

    @Unique
    private static final java.util.Set<String> STELLAR_CORE$DEFAULT_RESOURCE_DOMAINS = java.util.Collections.singleton("minecraft");

    @Shadow
    @Nullable
    protected abstract InputStream getResourceStream(final ResourceLocation location);

    @Final
    @Shadow
    private ResourceIndex resourceIndex;

    @Shadow
    public abstract Set<String> getResourceDomains();

    @Unique
    private final Map<ResourceLocation, Boolean> stellar_core$resourceExistsCache = new ConcurrentHashMap<>();

    @Unique
    private boolean stellar_core$cacheEnabled = false;

    /**
     * @author Kasumi_Nova
     * @reason Cache
     */
    @Inject(method = "resourceExists", at = @At("HEAD"), cancellable = true)
    public void resourceExists(final ResourceLocation location, final CallbackInfoReturnable<Boolean> cir) {
        if (location == null) {
            cir.setReturnValue(false);
            return;
        }

        if (stellar_core$cacheEnabled) {
            final Boolean cached = stellar_core$resourceExistsCache.get(location);
            if (cached != null) {
                cir.setReturnValue(cached);
                return;
            }

            final boolean computed = stellar_core$resourceExists0(location);
            final Boolean existing = stellar_core$resourceExistsCache.putIfAbsent(location, computed);
            cir.setReturnValue(existing != null ? existing : computed);
            return;
        }

        // Even when caching is disabled, still prefer ResourceIndex/ClasspathAssetIndex to avoid expensive
        // Class#getResource fallbacks when the index is already ready.
        cir.setReturnValue(stellar_core$resourceExists0(location));
    }

    @Unique
    private boolean stellar_core$resourceExists0(final ResourceLocation location) {
        if (this.resourceIndex.isFileExisting(location)) {
            return true;
        }

        final Set<String> resourceDomains = getResourceDomains();
        final String namespace = location.getNamespace();
        if (namespace != null && !namespace.isEmpty() && resourceDomains.contains(namespace)) {
            final Boolean indexed = ClasspathAssetIndex.tryContains(location);
            if (indexed != null) {
                return indexed;
            }
            // Ensure background init has started, but do not block this call.
            ClasspathAssetIndex.prewarmAsync(java.util.Collections.singleton(namespace));
        }

        final InputStream stream = this.getResourceStream(location);
        if (stream == null) {
            return false;
        }
        try {
            stream.close();
        } catch (Exception ignored) {
        }
        return true;
    }

    @Unique
    @Override
    public void stellar_core$onReload() {
        stellar_core$resourceExistsCache.clear();
    }

    @Override
    public void stellar_core$enableCache() {
        stellar_core$cacheEnabled = true;
        // Pre-index classpath assets for the hot namespace to avoid repeated classpath scans.
        // Other namespaces will be prewarmed on-demand.
        ClasspathAssetIndex.prewarmAsync(STELLAR_CORE$DEFAULT_RESOURCE_DOMAINS);
    }

    @Override
    public void stellar_core$disableCache() {
        stellar_core$resourceExistsCache.clear();
        stellar_core$cacheEnabled = false;
    }

}
