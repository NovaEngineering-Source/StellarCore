package github.kasuminova.stellarcore.mixin.minecraft.world;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.util.LinkedFakeArrayList;
import net.minecraft.world.World;
import net.minecraftforge.common.util.BlockSnapshot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;

@Mixin(World.class)
public class MixinWorld {

    @Shadow(remap = false) public ArrayList<BlockSnapshot> capturedBlockSnapshots;

    @Inject(method = "init", at = @At("HEAD"))
    private void injectInit(final CallbackInfoReturnable<World> cir) {
        if (!StellarCoreConfig.PERFORMANCE.vanilla.capturedBlockSnapshots) {
            return;
        }
        this.capturedBlockSnapshots = new LinkedFakeArrayList<>(this.capturedBlockSnapshots);
    }

}
