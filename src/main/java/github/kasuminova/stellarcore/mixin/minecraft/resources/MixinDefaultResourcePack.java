package github.kasuminova.stellarcore.mixin.minecraft.resources;

import github.kasuminova.stellarcore.client.resource.ClasspathAssetIndex;
import github.kasuminova.stellarcore.mixin.util.StellarCoreResourcePack;
import github.kasuminova.stellarcore.shaded.org.jctools.maps.NonBlockingHashMap;
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
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

@Mixin(DefaultResourcePack.class)
public abstract class MixinDefaultResourcePack implements StellarCoreResourcePack {

    @Unique
    private static final Set<String> STELLAR_CORE$DEFAULT_RESOURCE_DOMAINS = Collections.singleton("minecraft");

    @Shadow
    @Nullable
    protected abstract InputStream getResourceStream(final ResourceLocation location);

    @Final
    @Shadow
    private ResourceIndex resourceIndex;

    @Shadow
    public abstract Set<String> getResourceDomains();

    @Unique
    private final Map<ResourceLocation, Boolean> stellar_core$resourceExistsCache = new NonBlockingHashMap<>();

    @Unique
    private volatile boolean stellar_core$cacheEnabled = false;

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
        if (!stellar_core$cacheEnabled) {
            final Boolean probed = stellar_core$probeResourceExists(location);
            cir.setReturnValue(probed == null ? Boolean.FALSE : probed);
            return;
        }
        final Boolean cached = stellar_core$resourceExistsCache.get(location);
        if (cached != null) {
            cir.setReturnValue(cached);
            return;
        }

        final Boolean probed = stellar_core$probeResourceExists(location);
        if (probed == null) {
            cir.setReturnValue(false);
            return;
        }
        final Boolean previous = stellar_core$resourceExistsCache.putIfAbsent(location, probed);
        cir.setReturnValue(previous == null ? probed : previous);
    }

    @Unique
    @Nullable
    private Boolean stellar_core$probeResourceExists(final ResourceLocation location) {
        if (this.resourceIndex.isFileExisting(location)) {
            return Boolean.TRUE;
        }

        Boolean indexed = null;
        final String namespace = location.getNamespace();
        if (namespace != null && getResourceDomains().contains(namespace)) {
            indexed = ClasspathAssetIndex.tryContains(location);
            if (Boolean.TRUE.equals(indexed)) {
                return Boolean.TRUE;
            }
            if (indexed == null) {
                ClasspathAssetIndex.prewarmAsync(Collections.singleton(namespace));
            }
        }

        final InputStream stream = this.getResourceStream(location);
        if (stream == null) {
            return indexed != null ? Boolean.FALSE : null;
        }
        try {
            stream.close();
        } catch (IOException ignored) {
        }
        return Boolean.TRUE;
    }

    @Unique
    @Override
    public void stellar_core$onReload() {
        stellar_core$resourceExistsCache.clear();
    }

    @Override
    public void stellar_core$enableCache() {
        stellar_core$cacheEnabled = true;
        ClasspathAssetIndex.prewarmAsync(STELLAR_CORE$DEFAULT_RESOURCE_DOMAINS);
    }

    @Override
    public void stellar_core$disableCache() {
        stellar_core$resourceExistsCache.clear();
        stellar_core$cacheEnabled = false;
    }

}
