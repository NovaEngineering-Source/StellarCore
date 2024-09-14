package github.kasuminova.stellarcore.mixin.minecraft.world_pos_judgement;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(World.class)
public class MixinWorld {

    /**
     * @author Kasumi_Nova
     * @reason Faster than vanilla.
     */
    @Overwrite
    public boolean isValid(BlockPos pos) {
        if (isOutsideBuildHeight(pos)) {
            return false;
        }
        final int x = pos.getX();
        final int z = pos.getZ();
        return x >= -30000000 && z >= -30000000 && x < 30000000 && z < 30000000;
    }

    /**
     * @author Kasumi_Nova
     * @reason Faster than vanilla.
     */
    @Overwrite
    public boolean isOutsideBuildHeight(BlockPos pos) {
        final int y = pos.getY();
        return y < 0 || y >= 256;
    }

}
