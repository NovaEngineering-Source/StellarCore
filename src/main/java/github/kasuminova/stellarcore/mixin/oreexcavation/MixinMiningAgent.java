package github.kasuminova.stellarcore.mixin.oreexcavation;

import github.kasuminova.stellarcore.mixin.util.BlockSnapShotProvider;
import net.minecraft.entity.player.EntityPlayerMP;
import oreexcavation.handlers.MiningAgent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;

@Mixin(MiningAgent.class)
public class MixinMiningAgent {

    @Shadow(remap = false) @Final public EntityPlayerMP player;

    @SuppressWarnings("rawtypes")
    @Redirect(method = "tickMiner",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/ArrayList;remove(I)Ljava/lang/Object;",
                    remap = false),
            remap = false)
    private Object onTickMinerRemoveSnapshots(final ArrayList instance, final int i) {
        return ((BlockSnapShotProvider) player.world).getCapturedBlockSnapshots().remove(i);
    }

    @SuppressWarnings("rawtypes")
    @Redirect(method = "tickMiner",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/ArrayList;size()I",
                    remap = false),
            remap = false)
    private int onTickMinerGetSnapshotsSize(final ArrayList instance) {
        return ((BlockSnapShotProvider) player.world).getCapturedBlockSnapshots().size();
    }

    @SuppressWarnings("rawtypes")
    @Redirect(method = "tickMiner",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/ArrayList;get(I)Ljava/lang/Object;",
                    remap = false),
            remap = false)
    private Object onTickMinerGetSnapshot(final ArrayList instance, final int i) {
        return ((BlockSnapShotProvider) player.world).getCapturedBlockSnapshots().get(i);
    }

    @SuppressWarnings("rawtypes")
    @Redirect(method = "tickMiner",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/ArrayList;clear()V",
                    remap = false),
            remap = false)
    private void onTickMinerClearSnapshots(final ArrayList instance) {
        ((BlockSnapShotProvider) player.world).getCapturedBlockSnapshots().clear();
    }

}
