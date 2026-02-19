package github.kasuminova.stellarcore.mixin.minecraft.stitcher;

import github.kasuminova.stellarcore.client.texture.StitcherCache;
import github.kasuminova.stellarcore.common.util.StellarLog;
import net.minecraft.client.renderer.texture.Stitcher;
import net.minecraft.util.math.MathHelper;
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
            // Retrieve extras BEFORE applyCache (which may clear state).
            List<Stitcher.Holder> extras = cache.getExtraHolders();
            int extraCount = extras == null ? 0 : extras.size();

            // Apply cached slot layout and atlas dimensions.
            this.stitchSlots.clear();
            this.stitchSlots.addAll(cache.getSlots());
            this.currentWidth = cache.getWidth();
            this.currentHeight = cache.getHeight();

            // Allocate any extra sprites that exist in runtime but not in cache
            // (e.g. mods that randomly register different sprites each launch).
            if (extraCount > 0) {
                for (Stitcher.Holder extra : extras) {
                    ((AccessorStitcher) this).invokeAllocateSlot(extra);
                }
                // Re-round atlas dimensions to power of 2 after allocation.
                this.currentWidth = MathHelper.smallestEncompassingPowerOfTwo(this.currentWidth);
                this.currentHeight = MathHelper.smallestEncompassingPowerOfTwo(this.currentHeight);
            }

            // Write updated cache (including extras) for next launch.
            stellar_core$storeCache(cache);

            StellarLog.LOG.info("[StellarCore-MixinStitcher] Stitched {} texture sprites (cached + {} extra), cache state: {}, took {}ms.",
                    setStitchHolders.size(), extraCount, cacheState, System.currentTimeMillis() - stellar_core$startTime);
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
    private void stellar_core$storeCache(final StitcherCache cache) {
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
        StitcherCache.setActiveMap(null);
    }

}
