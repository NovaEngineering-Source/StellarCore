package github.kasuminova.stellarcore.mixin.minecraft.particle;

import github.kasuminova.stellarcore.client.particle.ParticleCulling;
import github.kasuminova.stellarcore.client.particle.ParticleRenderers;
import github.kasuminova.stellarcore.mixin.util.ParticleRenderBounds;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ParticleManager.class)
public abstract class MixinParticleManager {

    @Redirect(
        method = {"renderParticles", "renderLitParticles"},
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;renderParticle(Lnet/minecraft/client/renderer/BufferBuilder;Lnet/minecraft/entity/Entity;FFFFFF)V"),
        require = 0
    )
    private void stellar_core$renderParticle(final Particle particle, final BufferBuilder buffer,
                                                      final Entity entity, final float partialTicks,
                                                      final float rotationX, final float rotationZ,
                                                      final float rotationYZ, final float rotationXY,
                                                      final float rotationXZ) {
        if (ParticleCulling.INSTANCE.isActive() && particle instanceof ParticleRenderBounds
                && ParticleRenderers.usesBaseRenderer(particle.getClass())
                && ((ParticleRenderBounds) particle).stellar_core$isBillboardOutside(partialTicks,
                    rotationX, rotationZ, rotationYZ, rotationXY, rotationXZ)) {
            return;
        }
        particle.renderParticle(buffer, entity, partialTicks, rotationX, rotationZ, rotationYZ, rotationXY, rotationXZ);
    }
}
