package github.kasuminova.stellarcore.mixin.minecraft.resources;

import github.kasuminova.stellarcore.client.resource.DirectoryPathIndex;
import github.kasuminova.stellarcore.client.resource.ResourceExistingCache;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.mixin.util.StellarCoreAbstractResourcePackAccessor;
import github.kasuminova.stellarcore.mixin.util.StellarCoreResourcePack;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import net.minecraft.client.resources.FallbackResourceManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.client.resources.data.MetadataSerializer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.File;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(SimpleReloadableResourceManager.class)
public class MixinSimpleReloadableResourceManager {

    @Final
    @Shadow
    @Mutable
    private Map<String, FallbackResourceManager> domainResourceManagers;

    @Final
    @Shadow
    @Mutable
    private Set<String> setResourceDomains;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void injectInit(final MetadataSerializer rmMetadataSerializerIn, final CallbackInfo ci) {
        this.domainResourceManagers = new Object2ObjectOpenHashMap<>();
        this.setResourceDomains = new ObjectLinkedOpenHashSet<>();
    }

    @Inject(method = "reloadResources", at = @At("HEAD"))
    private void injectReloadResourcePack(final List<IResourcePack> resourcesPacksList, final CallbackInfo ci) {
        ResourceExistingCache.clear();
        if (StellarCoreConfig.PERFORMANCE.vanilla.directoryResourcePackIndex) {
            DirectoryPathIndex.clear();
        }
        resourcesPacksList.stream()
                .filter(StellarCoreResourcePack.class::isInstance)
                .map(StellarCoreResourcePack.class::cast)
                .forEach(ResourceExistingCache::addResourcePack);

        if (!StellarCoreConfig.PERFORMANCE.vanilla.directoryResourcePackIndex) {
            return;
        }

        for (IResourcePack pack : resourcesPacksList) {
            if (!(pack instanceof StellarCoreAbstractResourcePackAccessor)) {
                continue;
            }
            final File root = ((StellarCoreAbstractResourcePackAccessor) pack).stellar_core$getResourcePackFile();
            if (root == null || !root.isDirectory()) {
                continue;
            }
            final File assetsDir = new File(root, "assets");
            final File[] namespaceDirs = assetsDir.listFiles();
            if (namespaceDirs == null || namespaceDirs.length == 0) {
                continue;
            }
            for (File namespaceDir : namespaceDirs) {
                if (namespaceDir == null || !namespaceDir.isDirectory()) {
                    continue;
                }
                DirectoryPathIndex.prewarmAsync(namespaceDir);
            }
        }
    }

}
