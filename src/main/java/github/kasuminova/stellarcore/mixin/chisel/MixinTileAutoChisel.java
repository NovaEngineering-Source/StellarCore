package github.kasuminova.stellarcore.mixin.chisel;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.util.HashedItemStack;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import team.chisel.api.carving.ICarvingGroup;
import team.chisel.api.carving.ICarvingRegistry;
import team.chisel.api.carving.ICarvingVariation;
import team.chisel.common.block.TileAutoChisel;

import java.util.Map;
import java.util.WeakHashMap;

@Mixin(TileAutoChisel.class)
public class MixinTileAutoChisel {

    @Unique
    private static final Map<HashedItemStack, ICarvingGroup> CACHED_CARVING_GROUP = new WeakHashMap<>();
    @Unique
    private static final Map<HashedItemStack, ICarvingVariation> CACHED_VARIATION_GROUP = new WeakHashMap<>();

    @Unique
    private long stellar_core$interval = 20;

    @ModifyConstant(method = "update", constant = @Constant(longValue = 20L))
    private long modifyUpdateInterval(long interval) {
        if (!StellarCoreConfig.PERFORMANCE.chisel.autoChiselImprovements) {
            return interval;
        }
        return stellar_core$interval;
    }

    @Redirect(
            method = "update",
            at = @At(
                    value = "INVOKE",
                    target = "Lteam/chisel/api/carving/ICarvingRegistry;getGroup(Lnet/minecraft/item/ItemStack;)Lteam/chisel/api/carving/ICarvingGroup;",
                    remap = false
            ))
    @SuppressWarnings("MethodMayBeStatic")
    private ICarvingGroup redirectGetGroup(final ICarvingRegistry instance, final ItemStack stack) {
        if (!StellarCoreConfig.PERFORMANCE.chisel.autoChiselImprovements) {
            return instance.getGroup(stack);
        }

        HashedItemStack hashed = HashedItemStack.ofTagUnsafe(stack);
        ICarvingGroup group;
        if ((group = CACHED_CARVING_GROUP.get(hashed)) != null) {
            return group;
        }
        CACHED_CARVING_GROUP.put(HashedItemStack.ofTag(stack), (group = instance.getGroup(stack)));
        return group;
    }

    @Redirect(
            method = "update",
            at = @At(
                    value = "INVOKE",
                    target = "Lteam/chisel/api/carving/ICarvingRegistry;getVariation(Lnet/minecraft/item/ItemStack;)Lteam/chisel/api/carving/ICarvingVariation;",
                    remap = false
            ))
    @SuppressWarnings("MethodMayBeStatic")
    private ICarvingVariation redirectGetVariation(final ICarvingRegistry instance, final ItemStack stack) {
        if (!StellarCoreConfig.PERFORMANCE.chisel.autoChiselImprovements) {
            return instance.getVariation(stack);
        }

        HashedItemStack hashed = HashedItemStack.ofTagUnsafe(stack);
        ICarvingVariation variation;
        if ((variation = CACHED_VARIATION_GROUP.get(hashed)) != null) {
            return variation;
        }
        CACHED_VARIATION_GROUP.put(HashedItemStack.ofTag(stack), (variation = instance.getVariation(stack)));
        return variation;
    }

    @Inject(method = "mergeOutput", at = @At("HEAD"), remap = false)
    private void injectMergeOutput(final ItemStack stack, final CallbackInfo ci) {
        if (!StellarCoreConfig.PERFORMANCE.chisel.autoChiselImprovements) {
            return;
        }
        stellar_core$interval = Math.max(stellar_core$interval - 5, 20);
    }

    @Inject(method = "setSourceSlot", at = @At("HEAD"), remap = false)
    private void injectSetSourceSlot(final int slot, final CallbackInfo ci) {
        if (!StellarCoreConfig.PERFORMANCE.chisel.autoChiselImprovements) {
            return;
        }

        if (slot == -1) {
            int maxWorkDelay = StellarCoreConfig.PERFORMANCE.chisel.autoChiselMaxWorkDelay;
            stellar_core$interval = Math.min(stellar_core$interval + 5, maxWorkDelay);
        }
    }

}
