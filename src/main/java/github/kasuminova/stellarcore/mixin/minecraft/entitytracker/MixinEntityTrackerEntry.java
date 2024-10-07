package github.kasuminova.stellarcore.mixin.minecraft.entitytracker;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityTrackerEntry;
import net.minecraft.entity.player.EntityPlayerMP;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(EntityTrackerEntry.class)
public class MixinEntityTrackerEntry {

    @Final
    @Shadow
    @Mutable
    public Set<EntityPlayerMP> trackingPlayers;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void injectInit(final Entity entityIn, final int rangeIn, final int maxRangeIn, final int updateFrequencyIn, final boolean sendVelocityUpdatesIn, final CallbackInfo ci) {
        this.trackingPlayers = new ObjectOpenHashSet<>();
    }

}
