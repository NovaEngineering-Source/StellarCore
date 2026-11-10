package github.kasuminova.stellarcore.mixin.minecraft.particle;

import github.kasuminova.stellarcore.client.particle.ParticleBillboardBounds;
import github.kasuminova.stellarcore.client.particle.ParticleCulling;
import github.kasuminova.stellarcore.mixin.util.ParticleRenderBounds;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Particle.class)
public abstract class MixinParticle implements ParticleRenderBounds {

    @Shadow
    protected double prevPosX;
    @Shadow
    protected double prevPosY;
    @Shadow
    protected double prevPosZ;
    @Shadow
    protected double posX;
    @Shadow
    protected double posY;
    @Shadow
    protected double posZ;
    @Shadow
    protected float particleScale;
    @Shadow
    protected float particleAngle;
    @Shadow
    protected float prevParticleAngle;

    @Override
    public boolean stellar_core$isBillboardOutside(final float partialTicks, final float rotationX,
                                                   final float rotationZ, final float rotationYZ,
                                                   final float rotationXY, final float rotationXZ) {
        if (!ParticleCulling.INSTANCE.isActive()) {
            return false;
        }
        // Match the actual emitted float coordinates, including vanilla's interpolation origin.
        final float x = (float) (prevPosX + (posX - prevPosX) * partialTicks - Particle.interpPosX);
        final float y = (float) (prevPosY + (posY - prevPosY) * partialTicks - Particle.interpPosY);
        final float z = (float) (prevPosZ + (posZ - prevPosZ) * partialTicks - Particle.interpPosZ);
        double quaternionLengthSquared = 1.0D;
        if (particleAngle != 0.0F) {
            final Vec3d axis = Particle.cameraViewDir;
            if (axis == null) {
                return false;
            }
            final float angle = particleAngle + (particleAngle - prevParticleAngle) * partialTicks;
            final float w = MathHelper.cos(angle * 0.5F);
            final float sin = MathHelper.sin(angle * 0.5F);
            final float axisX = sin * (float) axis.x, axisY = sin * (float) axis.y, axisZ = sin * (float) axis.z;
            quaternionLengthSquared = (double) w * w + (double) axisX * axisX
                + (double) axisY * axisY + (double) axisZ * axisZ;
        }
        final double radius = ParticleBillboardBounds.radius(particleScale, rotationX, rotationZ,
            rotationYZ, rotationXY, rotationXZ, quaternionLengthSquared);
        return ParticleCulling.INSTANCE.isOutside(x, y, z, radius);
    }
}
