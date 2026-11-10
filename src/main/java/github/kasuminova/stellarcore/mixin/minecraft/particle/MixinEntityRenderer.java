package github.kasuminova.stellarcore.mixin.minecraft.particle;

import github.kasuminova.stellarcore.client.particle.ParticleCulling;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {

    @Redirect(
        method = "renderWorldPass",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleManager;renderParticles(Lnet/minecraft/entity/Entity;F)V"),
        require = 0
    )
    private void stellar_core$renderParticles(final ParticleManager manager, final Entity entity,
                                              final float partialTicks) {
        ParticleCulling.INSTANCE.begin();
        manager.renderParticles(entity, partialTicks);
        ParticleCulling.INSTANCE.end();
    }

    @Redirect(
        method = "renderWorldPass",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleManager;renderLitParticles(Lnet/minecraft/entity/Entity;F)V"),
        require = 0
    )
    private void stellar_core$renderLitParticles(final ParticleManager manager, final Entity entity,
                                                 final float partialTicks) {
        ParticleCulling.INSTANCE.begin();
        manager.renderLitParticles(entity, partialTicks);
        ParticleCulling.INSTANCE.end();
    }
}
