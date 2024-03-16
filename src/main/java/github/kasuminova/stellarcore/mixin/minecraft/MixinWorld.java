package github.kasuminova.stellarcore.mixin.minecraft;

import github.kasuminova.stellarcore.mixin.util.BlockSnapShotProvider;
import net.minecraft.world.World;
import net.minecraftforge.common.util.BlockSnapshot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.LinkedList;

@Mixin(World.class)
public class MixinWorld implements BlockSnapShotProvider {

    @Unique public LinkedList<BlockSnapshot> stellarcore$capturedBlockSnapshots = new LinkedList<>();

    @SuppressWarnings("rawtypes")
    @Redirect(method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;I)Z",
            at = @At(value = "INVOKE", target = "Ljava/util/ArrayList;add(Ljava/lang/Object;)Z")
    )
    public boolean onSetBlockStateAddSnapshot(final ArrayList instance, final Object e) {
        return stellarcore$capturedBlockSnapshots.add((BlockSnapshot) e);
    }

    @SuppressWarnings("rawtypes")
    @Redirect(method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;I)Z",
            at = @At(value = "INVOKE", target = "Ljava/util/ArrayList;remove(Ljava/lang/Object;)Z")
    )
    public boolean onSetBlockStateRemoveSnapshot(final ArrayList instance, final Object e) {
        return stellarcore$capturedBlockSnapshots.remove((BlockSnapshot) e);
    }

    @Override
    @SuppressWarnings("AddedMixinMembersNamePattern")
    public LinkedList<BlockSnapshot> getCapturedBlockSnapshots() {
        return stellarcore$capturedBlockSnapshots;
    }

}
