package github.kasuminova.stellarcore.mixin.resourceloader;

import github.kasuminova.stellarcore.client.resource.DirectoryPathIndex;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.util.StellarLog;
import github.kasuminova.stellarcore.mixin.util.StellarCoreResourcePack;
import github.kasuminova.stellarcore.shaded.org.jctools.maps.NonBlockingHashMap;
import lumien.resourceloader.loader.NormalResourceLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Map;

@Mixin(value = NormalResourceLoader.class, remap = false)
public class MixinNormalResourceLoader implements StellarCoreResourcePack {

    @Unique
    private final Map<ResourceLocation, Boolean> stellar_core$resourceExistsCache = new NonBlockingHashMap<>();

    @Unique
    private final Map<String, File> stellar_core$namespaceRoots = new NonBlockingHashMap<>();

    @Unique
    private boolean stellar_core$cacheEnabled;

    @Inject(method = "resourceExists", at = @At("HEAD"), remap = true, cancellable = true)
    private void stellar_core$usePositiveCache(@Nullable final ResourceLocation location,
                                               final CallbackInfoReturnable<Boolean> cir) {
        if (stellar_core$cacheEnabled && location != null
            && Boolean.TRUE.equals(stellar_core$resourceExistsCache.get(location))) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "resourceExists", at = @At("RETURN"), remap = true)
    private void stellar_core$rememberExisting(@Nullable final ResourceLocation location,
                                               final CallbackInfoReturnable<Boolean> cir) {
        if (stellar_core$cacheEnabled && location != null && cir.getReturnValueZ()) {
            stellar_core$resourceExistsCache.putIfAbsent(location, Boolean.TRUE);
        }
    }

    @Redirect(
        method = {"resourceExists", "func_110589_b"},
        at = @At(value = "INVOKE", target = "Ljava/io/File;isFile()Z")
    )
    private boolean stellar_core$isIndexedFile(final File file, final ResourceLocation location) {
        if (!StellarCoreConfig.PERFORMANCE.vanilla.resourceExistStateCache
            || !StellarCoreConfig.PERFORMANCE.vanilla.directoryResourcePackIndex
            || location == null) {
            return file.isFile();
        }
        final String namespace = location.getNamespace();
        final File root = stellar_core$getNamespaceRoot(namespace);
        return DirectoryPathIndex.contains(root, location.getPath(), file);
    }

    @Unique
    private File stellar_core$getNamespaceRoot(final String namespace) {
        final File cached = stellar_core$namespaceRoots.get(namespace);
        if (cached != null) {
            return cached;
        }
        final File created = new File(Minecraft.getMinecraft().gameDir, "resources/" + namespace);
        final File previous = stellar_core$namespaceRoots.putIfAbsent(namespace, created);
        return previous == null ? created : previous;
    }

    @Unique
    private void stellar_core$prewarmNamespaceRoots() {
        final File resourcesRoot = new File(Minecraft.getMinecraft().gameDir, "resources");
        final File[] namespaceRoots = resourcesRoot.listFiles(File::isDirectory);
        if (namespaceRoots == null) {
            if (resourcesRoot.isDirectory()) {
                StellarLog.LOG.error("[StellarCore-DirectoryPathIndex] Failed to list ResourceLoader root: {}",
                    resourcesRoot.getAbsolutePath());
            }
            return;
        }
        for (File namespaceRoot : namespaceRoots) {
            final File previous = stellar_core$namespaceRoots.putIfAbsent(namespaceRoot.getName(), namespaceRoot);
            DirectoryPathIndex.prewarmAsync(previous == null ? namespaceRoot : previous);
        }
    }

    @Override
    public void stellar_core$onReload() {
        stellar_core$resourceExistsCache.clear();
    }

    @Override
    public void stellar_core$disableCache() {
        stellar_core$resourceExistsCache.clear();
        stellar_core$cacheEnabled = false;
    }

    @Override
    public void stellar_core$enableCache() {
        stellar_core$cacheEnabled = true;
        if (StellarCoreConfig.PERFORMANCE.vanilla.directoryResourcePackIndex) {
            stellar_core$prewarmNamespaceRoots();
        }
    }

    @Override
    public boolean stellar_core$isMutableResourcePack() {
        return true;
    }
}
