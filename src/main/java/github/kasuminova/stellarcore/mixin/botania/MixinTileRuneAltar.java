package github.kasuminova.stellarcore.mixin.botania;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vazkii.botania.api.recipe.RecipeRuneAltar;
import vazkii.botania.common.block.tile.TileRuneAltar;
import vazkii.botania.common.block.tile.TileSimpleInventory;

import java.util.Collections;
import java.util.List;

@Mixin(TileRuneAltar.class)
public abstract class MixinTileRuneAltar extends TileSimpleInventory {

    @Shadow(remap = false)
    public abstract boolean isEmpty();

    @Shadow(remap = false)
    RecipeRuneAltar currentRecipe;

    @Shadow(remap = false)
    List<ItemStack> lastRecipe;

    @Unique
    private boolean stellar_core$shouldGetEntities = true;

    @Inject(method = "updateRecipe", at = @At("HEAD"), cancellable = true, remap = false)
    private void injectUpdateRecipe(final CallbackInfo ci) {
        if (!StellarCoreConfig.PERFORMANCE.botania.runeAltarImprovements) {
            return;
        }

        if ((this.lastRecipe == null || this.lastRecipe.isEmpty()) && this.currentRecipe == null && isEmpty()) {
            ci.cancel();
        }
    }

    @Redirect(
            method = "update",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;getEntitiesWithinAABB(Ljava/lang/Class;Lnet/minecraft/util/math/AxisAlignedBB;)Ljava/util/List;",
                    remap = true
            ),
            remap = false
    )
    private List<Entity> redirectUpdateGetEntities(final World instance, final Class<? extends Entity> classEntity, final AxisAlignedBB bb) {
        if (!StellarCoreConfig.PERFORMANCE.botania.runeAltarImprovements) {
            return instance.getEntitiesWithinAABB(classEntity, bb);
        }

        if (this.stellar_core$shouldGetEntities) {
            this.stellar_core$shouldGetEntities = false;
            return instance.getEntitiesWithinAABB(classEntity, bb);
        }
        this.stellar_core$shouldGetEntities = true;
        return Collections.emptyList();
    }

}
