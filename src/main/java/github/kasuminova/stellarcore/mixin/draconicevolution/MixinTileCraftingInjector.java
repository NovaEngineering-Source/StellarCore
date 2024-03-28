package github.kasuminova.stellarcore.mixin.draconicevolution;

import com.brandon3055.draconicevolution.blocks.tileentity.TileCraftingInjector;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.mixin.util.ITileCraftingInjector;
import github.kasuminova.stellarcore.mixin.util.IFusionCraftingCore;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(TileCraftingInjector.class)
public class MixinTileCraftingInjector extends TileEntity implements ITileCraftingInjector {

    @Unique
    protected IFusionCraftingCore stellar_core$core = null;
    
    @Override
    public void invalidate() {
        super.invalidate();
        if (!StellarCoreConfig.BUG_FIXES.draconicEvolution.craftingInjector) {
            return;
        }
        if (this.stellar_core$core != null) {
            this.stellar_core$core.onInjectorUnload();
        }
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        if (!StellarCoreConfig.BUG_FIXES.draconicEvolution.craftingInjector) {
            return;
        }
        if (this.stellar_core$core != null) {
            this.stellar_core$core.onInjectorUnload();
        }
    }

    @Override
    @SuppressWarnings("AddedMixinMembersNamePattern")
    public void onInjectorAddToCore(final IFusionCraftingCore core) {
        this.stellar_core$core = core;
    }

}
