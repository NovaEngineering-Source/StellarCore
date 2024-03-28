package github.kasuminova.stellarcore.mixin.draconicevolution;

import com.brandon3055.draconicevolution.api.fusioncrafting.IFusionRecipe;
import com.brandon3055.draconicevolution.blocks.tileentity.TileFusionCraftingCore;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.mixin.util.IFusionCraftingCore;
import github.kasuminova.stellarcore.mixin.util.ITileCraftingInjector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(TileFusionCraftingCore.class)
public abstract class MixinTileFusionCraftingCore implements IFusionCraftingCore {

    @Shadow protected abstract void invalidateCrafting();

    @Shadow public IFusionRecipe activeRecipe;

    @Redirect(
            method = "updateInjectors",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;add(Ljava/lang/Object;)Z",
                    remap = false
            ),
            remap = false
    )
    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean redirectAddInjector(final List instance, final Object e) {
        if (!StellarCoreConfig.BUG_FIXES.draconicEvolution.craftingInjector) {
            return instance.add(e);
        }
        if (e instanceof ITileCraftingInjector injector) {
            injector.onInjectorAddToCore(this);
        }
        return instance.add(e);
    }

    @Override
    @SuppressWarnings("AddedMixinMembersNamePattern")
    public void onInjectorUnload() {
        if (this.activeRecipe != null) {
            invalidateCrafting();
        }
    }

}
