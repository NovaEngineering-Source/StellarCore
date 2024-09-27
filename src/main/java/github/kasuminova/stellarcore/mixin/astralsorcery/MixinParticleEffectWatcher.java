package github.kasuminova.stellarcore.mixin.astralsorcery;

import hellfirepvp.astralsorcery.common.util.ParticleEffectWatcher;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(value = ParticleEffectWatcher.class, remap = false)
public class MixinParticleEffectWatcher {

    @Unique
    private final Int2ObjectMap<Set<BlockPos>> stellar_core$worldWatch = new Int2ObjectOpenHashMap<>();

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    public void injectTick(final TickEvent.Type type, final Object[] context, final CallbackInfo ci) {
        stellar_core$worldWatch.computeIfAbsent(((World) context[0]).provider.getDimension(), (key) -> new ObjectOpenHashSet<>()).clear();
        ci.cancel();
    }

    @Inject(method = "mayFire", at = @At("HEAD"), cancellable = true)
    public void injectMayFire(final World world, final BlockPos pos, final CallbackInfoReturnable<Boolean> cir) {
        int dimId = world.provider.getDimension();
        Set<BlockPos> worldPos = stellar_core$worldWatch.computeIfAbsent(dimId, (key) -> new ObjectOpenHashSet<>());
        cir.setReturnValue(!worldPos.contains(pos) && worldPos.add(pos));
    }

}
