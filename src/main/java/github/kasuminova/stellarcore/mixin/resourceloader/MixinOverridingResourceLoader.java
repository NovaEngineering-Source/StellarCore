package github.kasuminova.stellarcore.mixin.resourceloader;

import github.kasuminova.stellarcore.client.resource.DirectoryPathIndex;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.util.StellarLog;
import github.kasuminova.stellarcore.mixin.util.StellarCoreResourcePack;
import github.kasuminova.stellarcore.shaded.org.jctools.maps.NonBlockingHashMap;
import lumien.resourceloader.loader.OverridingResourceLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Map;

@Mixin(value = OverridingResourceLoader.class, remap = false)
public class MixinOverridingResourceLoader implements StellarCoreResourcePack {

    @Unique
    private final Map<String, DirectoryPathIndex.Handle> stellar_core$namespaceRoots = new NonBlockingHashMap<>();

    @Shadow
    boolean debug;

    @Unique
    private volatile boolean stellar_core$cacheEnabled;

    @Inject(method = "resourceExists", at = @At("HEAD"), remap = true, cancellable = true)
    private void stellar_core$useDirectoryIndex(@Nullable final ResourceLocation rl,
                                               final CallbackInfoReturnable<Boolean> cir) {
        // Preserve ResourceLoader's debug diagnostics and live filesystem queries outside caching.
        if (stellar_core$cacheEnabled && !debug && rl != null
            && StellarCoreConfig.PERFORMANCE.vanilla.resourceExistStateCache
            && StellarCoreConfig.PERFORMANCE.vanilla.directoryResourcePackIndex) {
            // Subclasses can alias equals/hashCode across paths; always read the actual path.
            cir.setReturnValue(stellar_core$getNamespaceRoot(rl.getNamespace()).contains(rl.getPath()));
        }
    }

    @Unique
    private DirectoryPathIndex.Handle stellar_core$getNamespaceRoot(final String namespace) {
        final DirectoryPathIndex.Handle cached = stellar_core$namespaceRoots.get(namespace);
        if (cached != null) {
            return cached;
        }
        final DirectoryPathIndex.Handle created = DirectoryPathIndex.bind(
            new File(Minecraft.getMinecraft().gameDir, "oresources/" + namespace));
        final DirectoryPathIndex.Handle previous = stellar_core$namespaceRoots.putIfAbsent(namespace, created);
        return previous == null ? created : previous;
    }

    @Unique
    private void stellar_core$prewarmNamespaceRoots() {
        final File resourcesRoot = new File(Minecraft.getMinecraft().gameDir, "oresources");
        final File[] namespaceRoots = resourcesRoot.listFiles(File::isDirectory);
        if (namespaceRoots == null) {
            if (resourcesRoot.isDirectory()) {
                StellarLog.LOG.error("[StellarCore-DirectoryPathIndex] Failed to list ResourceLoader root: {}",
                    resourcesRoot.getAbsolutePath());
            }
            return;
        }
        for (File namespaceRoot : namespaceRoots) {
            stellar_core$getNamespaceRoot(namespaceRoot.getName()).prewarmAsync();
        }
    }

    @Override
    public void stellar_core$onReload() {
        stellar_core$namespaceRoots.clear();
    }

    @Override
    public void stellar_core$disableCache() {
        stellar_core$cacheEnabled = false;
        stellar_core$namespaceRoots.clear();
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
