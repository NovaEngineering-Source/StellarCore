package github.kasuminova.stellarcore.mixin.minecraft.entitytracker.raytrace;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.util.FakePlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityPlayer.class)
public class MixinEntityPlayer {
    private final boolean isFakePlayer = ((EntityPlayer) (Object) this) instanceof FakePlayer;
    public dev.tr7zw.entityculling.CullTask stellarCore$cullTask = isFakePlayer ? null : new dev.tr7zw.entityculling.CullTask(
            new com.logisticscraft.occlusionculling.OcclusionCullingInstance(
                    64,
                    new dev.tr7zw.entityculling.DefaultChunkDataProvider(((EntityPlayer) (Object) this).getEntityWorld()
                    )
            ),
            (EntityPlayer) (Object) this,
            50,
            10);

    @Inject(method = "onUpdate", at = @At("HEAD"))
    public void onUpdate(CallbackInfo ci) {
        if (isFakePlayer) return;
        if (this.stellarCore$cullTask != null) {
            this.stellarCore$cullTask.signalStop();
        }
        final com.logisticscraft.occlusionculling.OcclusionCullingInstance culling = new com.logisticscraft.occlusionculling.OcclusionCullingInstance(
                64,
                new dev.tr7zw.entityculling.DefaultChunkDataProvider(((EntityPlayer) (Object) this).getEntityWorld())
        );

        this.stellarCore$cullTask = new dev.tr7zw.entityculling.CullTask(
                culling, (EntityPlayer) (Object) this,
                50,
                10
        );
        if (this.stellarCore$cullTask != null) {
            this.stellarCore$cullTask.requestCullSignal();
            this.stellarCore$cullTask.setup();
        }
    }

    @Inject(method = "setDead", at = @At("TAIL"))
    public void setDead(CallbackInfo ci) {
        if (isFakePlayer) return;
        if (this.stellarCore$cullTask != null) {
            this.stellarCore$cullTask.signalStop();
        }
    }
}
