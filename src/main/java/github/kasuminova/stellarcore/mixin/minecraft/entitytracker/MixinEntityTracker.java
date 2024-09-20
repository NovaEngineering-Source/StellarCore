package github.kasuminova.stellarcore.mixin.minecraft.entitytracker;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.entity.EntityTracker;
import net.minecraft.entity.EntityTrackerEntry;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(EntityTracker.class)
public class MixinEntityTracker {

    @Final
    @Shadow
    @Mutable
    private Set<EntityTrackerEntry> entries;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void injectInit(final WorldServer theWorldIn, final CallbackInfo ci) {
        this.entries = new ObjectOpenHashSet<>();
    }

}
