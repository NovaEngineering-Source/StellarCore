package github.kasuminova.stellarcore.mixin.minecraft.entitytracker.raytrace;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityPlayer.class)
public class MixinEntityPlayer {
    @Unique
    private final boolean stellarCore$isFakePlayer = ((EntityPlayer) (Object) this) instanceof FakePlayer;
    @Unique
    public dev.tr7zw.entityculling.CullTask stellarCore$cullTask = null;
    @Unique
    private World stellarCore$lastWorld = null;

    @Inject(method = "onUpdate", at = @At("HEAD"))
    public void onUpdate(CallbackInfo ci) {
        if (stellarCore$isFakePlayer) return;

        EntityPlayer player = (EntityPlayer) (Object) this;
        World currentWorld = player.getEntityWorld();

        boolean needsNewTask = stellarCore$cullTask == null ||
                stellarCore$lastWorld != currentWorld;

        if (needsNewTask) {
            if (stellarCore$cullTask != null) {
                stellarCore$cullTask.signalStop();
            }

            final com.logisticscraft.occlusionculling.OcclusionCullingInstance culling =
                    new com.logisticscraft.occlusionculling.OcclusionCullingInstance(
                            64,
                            new dev.tr7zw.entityculling.DefaultChunkDataProvider(currentWorld)
                    );

            stellarCore$cullTask = new dev.tr7zw.entityculling.CullTask(
                    culling, player,
                    50,
                    10
            );

            stellarCore$cullTask.setup();
            stellarCore$lastWorld = currentWorld;
        } else {
            stellarCore$cullTask.requestCullSignal();
        }
    }

    @Inject(method = "setDead", at = @At("TAIL"))
    public void setDead(CallbackInfo ci) {
        if (stellarCore$isFakePlayer) return;
        if (this.stellarCore$cullTask != null) {
            this.stellarCore$cullTask.signalStop();
            this.stellarCore$cullTask = null;
        }
    }
}