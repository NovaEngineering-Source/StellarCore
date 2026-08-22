package github.kasuminova.stellarcore.mixin.minecraft.resources;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import github.kasuminova.stellarcore.client.resource.DirectoryPathIndex;
import github.kasuminova.stellarcore.client.resource.ResourceExistingCache;
import github.kasuminova.stellarcore.common.util.MutableResourcePackBindings;
import github.kasuminova.stellarcore.common.util.PublishedState;
import github.kasuminova.stellarcore.common.util.StellarLog;
import github.kasuminova.stellarcore.mixin.util.StellarCoreMutableResourceManager;
import github.kasuminova.stellarcore.mixin.util.StellarCoreResourcePack;
import github.kasuminova.stellarcore.shaded.org.jctools.maps.NonBlockingHashMap;
import github.kasuminova.stellarcore.shaded.org.jctools.maps.NonBlockingHashSet;
import net.minecraft.client.resources.FallbackResourceManager;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mixin(SimpleReloadableResourceManager.class)
public abstract class MixinSimpleReloadableResourceManager implements StellarCoreMutableResourceManager {

    @Final
    @Shadow
    @Mutable
    private Map<String, FallbackResourceManager> domainResourceManagers;

    @Final
    @Shadow
    @Mutable
    private Set<String> setResourceDomains;

    @Unique
    private MutableResourcePackBindings stellar_core$bindings = new MutableResourcePackBindings();
    @Unique
    private ThreadLocal<Set<String>> stellar_core$loadingPackDomains = new ThreadLocal<>();
    @Final
    @Shadow
    private MetadataSerializer rmMetadataSerializer;
    @Unique
    private volatile PublishedState stellar_core$publishedState = PublishedState.empty();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void stellar_core$init(final MetadataSerializer rmMetadataSerializerIn, final CallbackInfo ci) {
        this.domainResourceManagers = new NonBlockingHashMap<>();
        this.setResourceDomains = new NonBlockingHashSet<>();
        this.stellar_core$bindings = new MutableResourcePackBindings();
        this.stellar_core$loadingPackDomains = new ThreadLocal<>();
    }

    @Inject(method = "clearResources", at = @At("RETURN"))
    private void stellar_core$clearResources(final CallbackInfo ci) {
        final Map<String, FallbackResourceManager> emptyManagers = new NonBlockingHashMap<>();
        final Set<String> emptyDomains = new NonBlockingHashSet<>();
        synchronized (this) {
            this.stellar_core$bindings.beginFullReload();
            this.domainResourceManagers = emptyManagers;
            this.setResourceDomains = emptyDomains;
            this.stellar_core$publishedState = PublishedState.empty();
        }
        try {
            ResourceExistingCache.clear();
        } finally {
            DirectoryPathIndex.clear();
        }
    }

    @Inject(method = "reloadResourcePack", at = @At("HEAD"))
    private void stellar_core$prepareResourcePackReload(final IResourcePack resourcePack, final CallbackInfo ci) {
        this.stellar_core$loadingPackDomains.remove();
        synchronized (this) {
            final Map<String, FallbackResourceManager> workingManagers =
                this.stellar_core$bindings.rebuildFallbackManagers(this.rmMetadataSerializer);
            this.domainResourceManagers = new NonBlockingHashMap<>(workingManagers);
            final Set<String> workingDomains = new NonBlockingHashSet<>();
            workingDomains.addAll(workingManagers.keySet());
            this.setResourceDomains = workingDomains;
        }
    }

    @Redirect(
        method = "reloadResourcePack",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/resources/IResourcePack;getResourceDomains()Ljava/util/Set;"
        )
    )
    private Set<String> stellar_core$captureResourcePackDomains(final IResourcePack pack) {
        final Set<String> domains = pack.getResourceDomains();
        this.stellar_core$loadingPackDomains.set(
            ImmutableSet.copyOf(domains)
        );
        return domains;
    }

    @Inject(method = "reloadResourcePack", at = @At("RETURN"))
    private void stellar_core$recordResourcePack(final IResourcePack resourcePack, final CallbackInfo ci) {
        final Set<String> namespaces = this.stellar_core$loadingPackDomains.get();
        this.stellar_core$loadingPackDomains.remove();
        if (namespaces == null) {
            throw new IllegalStateException("Resource pack domains were not captured for " + resourcePack.getPackName());
        }
        synchronized (this) {
            final MutableResourcePackBindings.RecordPlan bindingsPlan =
                this.stellar_core$bindings.prepareRecordPack(resourcePack, namespaces);
            final PublishedState publishedPlan = stellar_core$createPublishedWorkingState();
            if (resourcePack instanceof StellarCoreResourcePack pack) {
                ResourceExistingCache.addResourcePack(pack);
            }
            this.stellar_core$bindings.commit(bindingsPlan);
            this.stellar_core$publishedState = publishedPlan;
        }
    }

    @Override
    public void stellar_core$refreshMutableResourcePackNamespaces() {
        final MutableResourcePackBindings.RefreshPlan plan;
        try {
            synchronized (this) {
                plan = this.stellar_core$bindings.refreshMutableNamespaces(this.rmMetadataSerializer);
                stellar_core$publish(plan);
            }
        } catch (RuntimeException | Error failure) {
            StellarLog.LOG.error(
                "[StellarCore-ResourceManager] MUTABLE_NAMESPACE_REFRESH_FAILED thread={}",
                Thread.currentThread().getName(), failure
            );
            throw failure;
        }
        if (!plan.isEmpty()) {
            StellarLog.LOG.info(
                "[StellarCore-ResourceManager] MUTABLE_NAMESPACE_REFRESH_SUCCESS namespaces={} thread={}",
                plan.namespaces(), Thread.currentThread().getName()
            );
        }
    }

    @Inject(method = "getResource", at = @At("HEAD"), cancellable = true)
    private void stellar_core$getResource(final ResourceLocation location,
                                          final CallbackInfoReturnable<IResource> cir) throws IOException {
        FallbackResourceManager fallback = this.stellar_core$publishedState.managers().get(location.getNamespace());
        if (fallback == null) {
            fallback = stellar_core$attachOnMiss(location, null);
            if (fallback == null) {
                throw new FileNotFoundException(location.toString());
            }
        }
        try {
            cir.setReturnValue(fallback.getResource(location));
        } catch (FileNotFoundException failure) {
            final FallbackResourceManager replacement = stellar_core$attachOnMiss(location, fallback);
            if (replacement == null) {
                throw failure;
            }
            cir.setReturnValue(replacement.getResource(location));
        }
    }

    @Inject(method = "getAllResources", at = @At("HEAD"), cancellable = true)
    private void stellar_core$getAllResources(final ResourceLocation location,
                                              final CallbackInfoReturnable<List<IResource>> cir) throws IOException {
        FallbackResourceManager fallback = this.stellar_core$publishedState.managers().get(location.getNamespace());
        if (fallback == null) {
            fallback = stellar_core$attachOnMiss(location, null);
            if (fallback == null) {
                throw new FileNotFoundException(location.toString());
            }
        }
        try {
            cir.setReturnValue(fallback.getAllResources(location));
        } catch (FileNotFoundException failure) {
            final FallbackResourceManager replacement = stellar_core$attachOnMiss(location, fallback);
            if (replacement == null) {
                throw failure;
            }
            cir.setReturnValue(replacement.getAllResources(location));
        }
    }

    @Inject(method = "getResourceDomains", at = @At("HEAD"), cancellable = true)
    private void stellar_core$getResourceDomains(final CallbackInfoReturnable<Set<String>> cir) {
        cir.setReturnValue(this.stellar_core$publishedState.domains());
    }

    @Unique
    private FallbackResourceManager stellar_core$attachOnMiss(final ResourceLocation location,
                                                              final FallbackResourceManager observed) {
        final MutableResourcePackBindings.RefreshPlan plan;
        try {
            synchronized (this) {
                plan = this.stellar_core$bindings.discoverResource(this.rmMetadataSerializer, location);
                if (plan.isEmpty()) {
                    final FallbackResourceManager current =
                        this.stellar_core$publishedState.managers().get(location.getNamespace());
                    return current != observed ? current : null;
                }
                stellar_core$publish(plan);
            }
        } catch (RuntimeException | Error failure) {
            StellarLog.LOG.error(
                "[StellarCore-ResourceManager] LATE_RESOURCE_PACK_ATTACH_FAILED location={} thread={}",
                location, Thread.currentThread().getName(), failure
            );
            throw failure;
        }
        StellarLog.LOG.info(
            "[StellarCore-ResourceManager] LATE_RESOURCE_PACK_ATTACHED location={} namespaces={} thread={}",
            location, plan.namespaces(), Thread.currentThread().getName()
        );
        return plan.replacements().get(location.getNamespace());
    }

    @Unique
    private void stellar_core$publish(final MutableResourcePackBindings.RefreshPlan plan) {
        if (plan.isEmpty()) {
            return;
        }
        final PublishedState current = this.stellar_core$publishedState;
        final Map<String, FallbackResourceManager> nextManagers = new HashMap<>(current.managers());
        nextManagers.putAll(plan.replacements());
        final Set<String> nextDomains = new HashSet<>(current.domains());
        nextDomains.addAll(plan.namespaces());
        final PublishedState next = new PublishedState(
            Collections.unmodifiableMap(nextManagers), Collections.unmodifiableSet(nextDomains)
        );
        final Map<String, FallbackResourceManager> nextWorkingManagers =
            new NonBlockingHashMap<>(next.managers());
        final Set<String> nextWorkingDomains = new NonBlockingHashSet<>();
        nextWorkingDomains.addAll(next.domains());
        this.stellar_core$bindings.commit(plan);
        this.domainResourceManagers = nextWorkingManagers;
        this.setResourceDomains = nextWorkingDomains;
        this.stellar_core$publishedState = next;
    }

    @Unique
    private PublishedState stellar_core$createPublishedWorkingState() {
        return new PublishedState(
            ImmutableMap.copyOf(this.domainResourceManagers),
            ImmutableSet.copyOf(this.setResourceDomains)
        );
    }

}
