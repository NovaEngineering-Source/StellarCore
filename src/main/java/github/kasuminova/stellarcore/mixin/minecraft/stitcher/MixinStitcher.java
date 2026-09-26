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

            // Apply the cached slot layout. Allocation resumes from the extent the
            // layout occupies rather than from the stored atlas size, so extras
            // consume the slack that rounding left instead of crossing the
            // power-of-two boundary and doubling the atlas.
            final int atlasWidth = cache.getWidth();
            final int atlasHeight = cache.getHeight();

            this.stitchSlots.clear();
            this.stitchSlots.addAll(cache.getSlots());
            final int occupiedWidth = StitcherCache.occupiedWidth(this.stitchSlots);
            final int occupiedHeight = StitcherCache.occupiedHeight(this.stitchSlots);
            if (occupiedWidth <= 0 || occupiedHeight <= 0) {
                // A cache that lists holders but describes no layout cannot be
                // resumed; let the stitcher pack everything from scratch. The
                // slots must be cleared first because doStitch appends to them.
                this.stitchSlots.clear();
                this.currentWidth = 0;
                this.currentHeight = 0;
                cache.clear();
                return;
            }
            this.currentWidth = occupiedWidth;
            this.currentHeight = occupiedHeight;

            // Allocate any extra sprites that exist in runtime but not in cache
            // (e.g. mods that randomly register different sprites each launch).
            if (extraCount > 0) {
                final boolean[] rotatedSnapshot = new boolean[extraCount];
                for (int i = 0; i < extraCount; i++) {
                    rotatedSnapshot[i] = extras.get(i).isRotated();
                }

                boolean allPlaced = true;
                for (int i = 0; i < extraCount; i++) {
                    if (!((AccessorStitcher) this).invokeAllocateSlot(extras.get(i))) {
                        allPlaced = false;
                        break;
                    }
                }

                if (!allPlaced
                        || MathHelper.smallestEncompassingPowerOfTwo(this.currentWidth) > atlasWidth
                        || MathHelper.smallestEncompassingPowerOfTwo(this.currentHeight) > atlasHeight) {
                    // The extras do not fit inside the atlas the cache describes.
                    // Growing it to fit would spend twice the texture memory on a
                    // guess, so pack everything properly instead.

                    // Undo the rotations allocation left behind, so the re-stitch
                    // sees the holders as they arrived.
                    for (int i = 0; i < extraCount; i++) {
                        final Stitcher.Holder extra = extras.get(i);
                        if (extra.isRotated() != rotatedSnapshot[i]) {
                            extra.rotate();
                        }
                    }
                    this.stitchSlots.clear();
                    this.currentWidth = 0;
                    this.currentHeight = 0;
                    cache.clear();
                    return;
                }
            }

            // Everything fits the atlas the cache describes, so its dimensions
            // stand and the texture is allocated at exactly the size it had.
            this.currentWidth = atlasWidth;
            this.currentHeight = atlasHeight;

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
