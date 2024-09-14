package github.kasuminova.stellarcore.mixin.minecraft.chunk_cache;

import com.llamalad7.mixinextras.sugar.Local;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.util.StellarLog;
import github.kasuminova.stellarcore.mixin.util.CachedChunk;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.init.Blocks;
import net.minecraft.util.ReportedException;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.gen.ChunkGeneratorDebug;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings({"StaticVariableMayNotBeInitialized", "NonConstantFieldWithUpperCaseName"})
@Mixin(Chunk.class)
public class MixinChunk implements CachedChunk {

    @Final
    @Shadow
    private World world;

    @Final
    @Shadow
    private ExtendedBlockStorage[] storageArrays;

    @Final
    @Shadow
    public static ExtendedBlockStorage NULL_BLOCK_STORAGE;

    @Unique
    private final Short2ObjectMap<IBlockState> stellar_core$blockStateCache = new Short2ObjectOpenHashMap<>();

    @Unique
    private boolean stellar_core$enableCache = false;

    /**
     * @author Kasumi_Nova
     * @reason State Cache
     */
    @Overwrite
    public IBlockState getBlockState(final int x, final int y, final int z) {
        if (this.world.getWorldType() == WorldType.DEBUG_ALL_BLOCK_STATES) {
            IBlockState iblockstate = null;

            if (y == 60) {
                iblockstate = Blocks.BARRIER.getDefaultState();
            }

            if (y == 70) {
                iblockstate = ChunkGeneratorDebug.getBlockStateFor(x, z);
            }

            return iblockstate == null ? Blocks.AIR.getDefaultState() : iblockstate;
        }

        // StellarCore Start
        short combinedPos = 0;
        if (stellar_core$enableCache) {
            combinedPos = (short) (((z & 15) << 12) | (y << 4) | (x & 15));
            try {
                IBlockState ret = stellar_core$blockStateCache.get(combinedPos);
                if (ret != null) {
                    return ret;
                }
            } catch (Throwable e) {
                if (StellarCoreConfig.DEBUG.enableDebugLog) {
                    StellarLog.LOG.warn("[StellarCore-ChunkCache] Thread `{}` unable to fetch cache due to concurrency error.", Thread.currentThread().getName());
                }
            }
        }
        // StellarCore End

        try {
            // StellarCore Start
            int heightIdx = y >> 4;
            if (y >= 0 && heightIdx < this.storageArrays.length) {
                ExtendedBlockStorage extendedblockstorage = this.storageArrays[heightIdx];
                if (extendedblockstorage != NULL_BLOCK_STORAGE) {
                    IBlockState ret = extendedblockstorage.get(x & 15, y & 15, z & 15);
                    if (stellar_core$enableCache) {
                        stellar_core$storeCache(combinedPos, ret);
                    }
                    return ret;
                    // StellarCore End
                }
            }
            return Blocks.AIR.getDefaultState();
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Getting block state");
            CrashReportCategory crashreportcategory = crashreport.makeCategory("Block being got");
            crashreportcategory.addDetail("Location", () -> CrashReportCategory.getCoordinateInfo(x, y, z));
            throw new ReportedException(crashreport);
        }
    }

    @Unique
    private void stellar_core$storeCache(final short combinedValue, final IBlockState ret) {
        synchronized (stellar_core$blockStateCache) {
            while (true) { // rehash() 暴 力 重 试
                try {
                    stellar_core$blockStateCache.put(combinedValue, ret);
                    break;
                } catch (Throwable ignored) {
                }
            }
        }
    }

    @Inject(
            method = "setBlockState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/chunk/storage/ExtendedBlockStorage;set(IIILnet/minecraft/block/state/IBlockState;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void removeStateCache(final BlockPos pos, final IBlockState state, final CallbackInfoReturnable<IBlockState> cir,
                                  @Local(name = "i") int x, @Local(name = "j") final int y, @Local(name = "k") final int z,
                                  @Local(name = "extendedblockstorage") ExtendedBlockStorage extendedblockstorage) {
        if (!stellar_core$enableCache) {
            return;
        }
        final short combinedValue = (short) ((z << 12) | (y << 4) | x);
//        IBlockState newState = extendedblockstorage.get(x, y & 15, z);

        synchronized (stellar_core$blockStateCache) {
            while (true) {
                try {
                    stellar_core$blockStateCache.remove(combinedValue);
//                    stellar_core$blockStateCache.put(combinedValue, newState);
                    break;
                } catch (Throwable ignored) {
                }
            }
        }
    }

    @Inject(method = "setStorageArrays", at = @At("HEAD"))
    private void injectSetStorageArrays(final ExtendedBlockStorage[] newStorageArrays, final CallbackInfo ci) {
        stellar_core$blockStateCache.clear();
        stellar_core$enableCache = true;
    }

    @Unique
    @Override
    public Short2ObjectMap<IBlockState> stellar_core$getBlockStateCache() {
        return stellar_core$blockStateCache;
    }

    @Unique
    @Override
    public void stellar_core$clearCache() {
        stellar_core$blockStateCache.clear();
    }

}
