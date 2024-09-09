package github.kasuminova.stellarcore.mixin.minecraft.stitcher;

import github.kasuminova.stellarcore.StellarCore;
import github.kasuminova.stellarcore.client.texture.StitcherCache;
import github.kasuminova.stellarcore.common.util.StellarLog;
import net.minecraft.client.renderer.texture.Stitcher;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Mixin(Stitcher.class)
public abstract class MixinStitcher {

    @Final
    @Shadow
    private Set<Stitcher.Holder> setStitchHolders;

    @Final
    @Shadow
    private List<Stitcher.Slot> stitchSlots;

    @Shadow
    private int currentWidth;

    @Shadow
    private int currentHeight;

    @Unique
    private long stellar_core$startTime;

    @Inject(method = "doStitch", at = @At("HEAD"), cancellable = true)
    private void injectDoStitch(final CallbackInfo ci) {
        stellar_core$startTime = System.currentTimeMillis();

        StitcherCache cache = StitcherCache.getActiveCache();
        if (cache == null) {
            StellarLog.LOG.info("[StellarCore-MixinStitcher] Current TextureMap has no cache found, skipping...");
            return;
        }

        cache.parseTag((Stitcher) (Object) this, setStitchHolders);
        StitcherCache.State cacheState = cache.getCacheState();
        if (cacheState == StitcherCache.State.AVAILABLE) {
            stellar_core$applyCache(cache);
            StellarLog.LOG.info("[StellarCore-MixinStitcher] Stitched {} texture sprites, cache state: {}, took {}ms.", setStitchHolders.size(), cacheState, System.currentTimeMillis() - stellar_core$startTime);
            ci.cancel();
        } else {
            cache.clear();
        }
    }

    @Inject(method = "doStitch", at = @At("RETURN"))
    private void injectDoStitchTail(final CallbackInfo ci) {
        StitcherCache cache = StitcherCache.getActiveCache();
        if (cache == null) {
            return;
        }
        stellar_core$storeCache(cache);
        StellarLog.LOG.info("[StellarCore-MixinStitcher] Stitched {} texture sprites, cache state: {}, took {}ms.", setStitchHolders.size(), StitcherCache.State.UNAVAILABLE, System.currentTimeMillis() - stellar_core$startTime);
    }

    @Unique
    private void stellar_core$applyCache(StitcherCache cache) {
        this.stitchSlots.clear();
        this.stitchSlots.addAll(cache.getSlots());
        this.currentWidth = cache.getWidth();
        this.currentHeight = cache.getHeight();
        stellar_core$storeCache(cache);
    }

    @Unique
    private void stellar_core$storeCache(final StitcherCache cache) {
        synchronized (cache) {
            if (cache.getCacheState() == StitcherCache.State.AVAILABLE) {
                cache.clear();
                StellarLog.LOG.info("[StellarCore-MixinStitcher] Stitching cache is already available, skipped storing...");
                return;
            }
            CompletableFuture.runAsync(() -> {
                synchronized (cache) {
                    long startTime = System.currentTimeMillis();
                    StellarLog.LOG.info("[StellarCore-MixinStitcher] Storing stitcher cache...");
                    cache.cache(setStitchHolders, stitchSlots, currentWidth, currentHeight);
                    cache.writeToFile();
                    cache.clear();
                    StellarLog.LOG.info("[StellarCore-MixinStitcher] Stored stitcher cache, took {}ms.", System.currentTimeMillis() - startTime);
                }
            });
        }
        StitcherCache.setActiveMap(null);
    }

}
