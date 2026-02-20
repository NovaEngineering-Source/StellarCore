package github.kasuminova.stellarcore.mixin.resourceloader;

import github.kasuminova.stellarcore.client.resource.DirectoryPathIndex;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.mixin.util.StellarCoreResourcePack;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(value = lumien.resourceloader.loader.NormalResourceLoader.class, remap = false)
public class MixinNormalResourceLoader implements StellarCoreResourcePack {

    @Unique
    private final Map<ResourceLocation, Boolean> stellar_core$resourceExistsCache = new ConcurrentHashMap<>();

    @Unique
    private final Map<String, File> stellar_core$baseDirCache = new ConcurrentHashMap<>();

    @Unique
    private boolean stellar_core$cacheEnabled = false;

    @Inject(method = {"resourceExists", "func_110589_b"}, at = @At("HEAD"), cancellable = true)
    private void stellar_core$resourceExists(@Nullable final ResourceLocation location, final CallbackInfoReturnable<Boolean> cir) {
        if (!stellar_core$cacheEnabled || location == null) {
            return;
        }
        cir.setReturnValue(stellar_core$resourceExistsCached(location));
    }

    @Inject(method = {"getInputStream", "func_110590_a"}, at = @At("HEAD"), cancellable = true)
    private void stellar_core$getInputStream(@Nullable final ResourceLocation location, final CallbackInfoReturnable<InputStream> cir) throws java.io.IOException {
        if (location == null) {
            cir.setReturnValue(null);
            return;
        }

        // ResourceLoader's original implementation does an expensive getCanonicalFile() check
        // just for a warning about case-insensitive file systems. Skip it on the hot path.
        final boolean exists = stellar_core$cacheEnabled
            ? stellar_core$resourceExistsCached(location)
            : stellar_core$resourceExists0(location);
        if (!exists) {
            cir.setReturnValue(null);
            return;
        }

        final File file = stellar_core$resolve(location);
        cir.setReturnValue(new FileInputStream(file));
    }

    @Unique
    private boolean stellar_core$resourceExistsCached(final ResourceLocation location) {
        final Boolean cached = stellar_core$resourceExistsCache.get(location);
        if (cached != null) {
            return cached;
        }
        final boolean computed = stellar_core$resourceExists0(location);
        final Boolean existing = stellar_core$resourceExistsCache.putIfAbsent(location, computed);
        return existing != null ? existing : computed;
    }

    @Unique
    private boolean stellar_core$resourceExists0(final ResourceLocation location) {
        final String namespace = location.getNamespace();
        final String path = location.getPath();
        if (namespace == null || namespace.isEmpty() || path == null || path.isEmpty()) {
            return false;
        }

        final File baseDir = stellar_core$baseDirCache.computeIfAbsent(namespace, ns ->
                new File(Minecraft.getMinecraft().gameDir, "resources/" + ns)
        );

        if (StellarCoreConfig.PERFORMANCE.vanilla.directoryResourcePackIndex) {
            final Boolean indexed = DirectoryPathIndex.tryContains(baseDir, path);
            if (indexed != null) {
                return indexed;
            }
            DirectoryPathIndex.prewarmAsync(baseDir);
        }

        return new File(baseDir, path).isFile();
    }

    @Unique
    private File stellar_core$resolve(final ResourceLocation location) {
        final String namespace = location.getNamespace();
        final File baseDir = stellar_core$baseDirCache.computeIfAbsent(namespace, ns ->
                new File(Minecraft.getMinecraft().gameDir, "resources/" + ns)
        );
        return new File(baseDir, location.getPath());
    }

    @Override
    public void stellar_core$onReload() {
        stellar_core$resourceExistsCache.clear();
        if (!StellarCoreConfig.PERFORMANCE.vanilla.directoryResourcePackIndex) {
            return;
        }
        final File resourcesDir = new File(Minecraft.getMinecraft().gameDir, "resources");
        final File[] namespaces = resourcesDir.listFiles();
        if (namespaces == null || namespaces.length == 0) {
            return;
        }
        for (File namespaceDir : namespaces) {
            if (namespaceDir == null || !namespaceDir.isDirectory()) {
                continue;
            }
            DirectoryPathIndex.prewarmAsync(namespaceDir);
        }
    }

    @Override
    public void stellar_core$disableCache() {
        stellar_core$resourceExistsCache.clear();
        stellar_core$cacheEnabled = false;
    }

    @Override
    public void stellar_core$enableCache() {
        stellar_core$cacheEnabled = true;
    }
}
