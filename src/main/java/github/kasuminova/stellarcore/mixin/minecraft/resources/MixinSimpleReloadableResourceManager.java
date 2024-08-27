package github.kasuminova.stellarcore.mixin.minecraft.resources;

import github.kasuminova.stellarcore.mixin.util.StellarCoreResourcePackReloadListener;
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
        resourcesPacksList.stream()
                .filter(StellarCoreResourcePackReloadListener.class::isInstance)
                .map(StellarCoreResourcePackReloadListener.class::cast)
                .forEach(StellarCoreResourcePackReloadListener::stellar_core$onReload);
    }

}
