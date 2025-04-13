package github.kasuminova.stellarcore.mixin.minecraft.phys;

import net.minecraft.util.math.AxisAlignedBB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = AxisAlignedBB.class, priority = 999)
public abstract class MixinAxisAlignedBB {
    @Shadow @Final public double minX;

    @Shadow @Final public double minY;

    @Shadow @Final public double minZ;

    @Shadow @Final public double maxX;

    @Shadow @Final public double maxY;

    @Shadow @Final public double maxZ;

    /**
     * @author Creeam
     * @reason Optimize AABB
     */
    @Overwrite
    public AxisAlignedBB intersect(AxisAlignedBB other) {
        return new AxisAlignedBB(
                this.minX > other.minX ? this.minX : other.minX,
                this.minY > other.minY ? this.minY : other.minY,
                this.minZ > other.minZ ? this.minZ : other.minZ,
                this.maxX < other.maxX ? this.maxX : other.maxX,
                this.maxY < other.maxY ? this.maxY : other.maxY,
                this.maxZ < other.maxZ ? this.maxZ : other.maxZ
        );
    }

    /**
     * @author Creeam
     * @reason Direct comparison, avoid method call
     */
    @Overwrite
    public boolean intersects(AxisAlignedBB other) {
        return this.minX < other.maxX &&
                this.maxX > other.minX &&
                this.minY < other.maxY &&
                this.maxY > other.minY &&
                this.minZ < other.maxZ &&
                this.maxZ > other.minZ;
    }
}
