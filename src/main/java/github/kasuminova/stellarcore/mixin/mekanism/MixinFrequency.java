package github.kasuminova.stellarcore.mixin.mekanism;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import mekanism.api.Coord4D;
import mekanism.common.frequency.Frequency;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;
import java.util.UUID;

@Mixin(Frequency.class)
public class MixinFrequency {

    @Shadow(remap = false) public Set<Coord4D> activeCoords;

    @Inject(method = "<init>(Lio/netty/buffer/ByteBuf;)V", at = @At("RETURN"), remap = false)
    private void injectInit(final ByteBuf dataStream, final CallbackInfo ci) {
        if (!StellarCoreConfig.PERFORMANCE.mekanism.frequency) {
            return;
        }
        this.activeCoords = new ObjectOpenHashSet<>();
    }

    @Inject(method = "<init>(Ljava/lang/String;Ljava/util/UUID;)V", at = @At("RETURN"), remap = false)
    private void injectInit(final String n, final UUID uuid, final CallbackInfo ci) {
        if (!StellarCoreConfig.PERFORMANCE.mekanism.frequency) {
            return;
        }
        this.activeCoords = new ObjectOpenHashSet<>();
    }

    @Inject(method = "<init>(Lnet/minecraft/nbt/NBTTagCompound;)V", at = @At("RETURN"), remap = false)
    private void injectInit(final NBTTagCompound nbtTags, final CallbackInfo ci) {
        if (!StellarCoreConfig.PERFORMANCE.mekanism.frequency) {
            return;
        }
        this.activeCoords = new ObjectOpenHashSet<>();
    }

}
