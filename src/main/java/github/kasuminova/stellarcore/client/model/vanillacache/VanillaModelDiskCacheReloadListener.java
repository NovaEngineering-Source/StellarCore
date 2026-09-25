package github.kasuminova.stellarcore.client.model.vanillacache;

import github.kasuminova.stellarcore.common.util.StellarLog;
import net.minecraft.client.resources.IResourceManager;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.resource.IResourceType;
import net.minecraftforge.client.resource.ISelectiveResourceReloadListener;
import net.minecraftforge.client.resource.VanillaResourceType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.function.Predicate;

/**
 * Keeps the model disk cache honest across resource reloads.
 *
 * <p>A reload can change the environment in two ways:</p>
 * <ul>
 *   <li>the user enabled/disabled/reordered resource packs, or edited a folder
 *       pack's contents — the fingerprint changes, the cache must not be reused;</li>
 *   <li>nothing meaningful changed (a language switch, a reload triggered by a
 *       mod) — the fingerprint is identical, and reusing the cache is both
 *       correct and much faster than rebuilding it.</li>
 * </ul>
 *
 * <p>We cannot tell which case we are in until the fingerprint has been
 * recomputed, so the sequence is: drop the current view, recompute, then ask
 * {@link VanillaModelDiskCache#prepareSync} to read whatever file matches the new
 * environment. That single code path therefore implements both "inherit" and
 * "discard", with no special cases.</p>
 *
 * <p>Since the reloaded models have not been produced yet at this point, the
 * save is deferred to the first {@link TextureStitchEvent.Post} after the reload,
 * by which time the model loader has finished and the capture map is complete.</p>
 */
public final class VanillaModelDiskCacheReloadListener implements ISelectiveResourceReloadListener {

    public static final VanillaModelDiskCacheReloadListener INSTANCE = new VanillaModelDiskCacheReloadListener();

    private final Object stitchLock = new Object();
    private boolean awaitingStitch = false;

    private VanillaModelDiskCacheReloadListener() {
    }

    @Override
    public void onResourceManagerReload(final IResourceManager resourceManager,
                                        final Predicate<IResourceType> resourcePredicate) {
        // Only model JSONs are cached here; a texture- or language-only reload
        // cannot change what we store.
        if (!resourcePredicate.test(VanillaResourceType.MODELS)) {
            return;
        }
        final VanillaModelDiskCache cache = VanillaModelDiskCache.INSTANCE;
        if (!cache.isEnabled()) {
            return;
        }
        try {
            cache.onModelCacheCleared();
            cache.prepareSync(Loader.instance().getConfigDir());
            awaitNextStitch();
        } catch (Throwable t) {
            StellarLog.LOG.warn("[StellarCore-VanillaModelDiskCache] Reload handling failed", t);
        }
    }

    private void awaitNextStitch() {
        synchronized (stitchLock) {
            if (awaitingStitch) {
                return;
            }
            awaitingStitch = true;
            MinecraftForge.EVENT_BUS.register(this);
        }
    }

    @SubscribeEvent
    public void onTextureStitchPost(final TextureStitchEvent.Post event) {
        synchronized (stitchLock) {
            if (!awaitingStitch) {
                return;
            }
            awaitingStitch = false;
            MinecraftForge.EVENT_BUS.unregister(this);
        }
        final VanillaModelDiskCache cache = VanillaModelDiskCache.INSTANCE;
        if (!cache.isEnabled()) {
            return;
        }
        try {
            cache.saveAsync(Loader.instance().getConfigDir());
        } catch (Throwable t) {
            StellarLog.LOG.warn("[StellarCore-VanillaModelDiskCache] Post-reload save failed", t);
        }
    }
}
