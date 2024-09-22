package github.kasuminova.stellarcore.mixin.techguns;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import techguns.client.ClientProxy;
import techguns.entities.projectiles.BioGunProjectile;

@Mixin(BioGunProjectile.class)
@SuppressWarnings("MethodMayBeStatic")
public abstract class MixinBioGunProjectile extends Entity {

    @SuppressWarnings("DataFlowIssue")
    public MixinBioGunProjectile() {
        super(null);
    }

    @Redirect(method = "<init>(Lnet/minecraft/world/World;)V", at = @At(value = "INVOKE", target = "Ltechguns/client/ClientProxy;get()Ltechguns/client/ClientProxy;"))
    private ClientProxy injectInitGetProxy() {
        if (world.isRemote || !StellarCoreConfig.BUG_FIXES.techguns.serverSideEntityCrashFixes) {
            return ClientProxy.get();
        }
        return null;
    }

    @Redirect(method = "<init>(Lnet/minecraft/world/World;)V", at = @At(value = "INVOKE", target = "Ltechguns/client/ClientProxy;createFXOnEntity(Ljava/lang/String;Lnet/minecraft/entity/Entity;)V"))
    private void injectInitCreateFX(final ClientProxy instance, final String name, final Entity ent) {
        if (instance != null || !StellarCoreConfig.BUG_FIXES.techguns.serverSideEntityCrashFixes) {
            assert instance != null;
            instance.createFXOnEntity(name, ent);
        }
    }

}
