package github.kasuminova.stellarcore.mixin.ctm;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.shaded.org.jctools.maps.NonBlockingHashMap;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import team.chisel.ctm.client.texture.IMetadataSectionCTM;
import team.chisel.ctm.client.util.ResourceUtil;

import java.util.Map;

@Mixin(value = ResourceUtil.class, remap = false)
public class MixinResourceUtil {

    @Mutable
    @Shadow
    @Final
    private static Map<ResourceLocation, IMetadataSectionCTM> metadataCache;

    @Unique
    private static IMetadataSectionCTM stellar_core$absent = null;

    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void injectClinit(final CallbackInfo ci) {
        if (!StellarCoreConfig.PERFORMANCE.vanilla.parallelModelLoader) {
            return;
        }
        stellar_core$absent = new IMetadataSectionCTM.V1();
        metadataCache = new NonBlockingHashMap<>();
    }

    @Redirect(
            method = "getMetadata(Lnet/minecraft/util/ResourceLocation;)Lteam/chisel/ctm/client/texture/IMetadataSectionCTM;",
            at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;")
    )
    private static Object stellar_core$redirectGet(final Map<ResourceLocation, IMetadataSectionCTM> map, final Object key) {
        final IMetadataSectionCTM cached = map.get((ResourceLocation) key);
        return cached == stellar_core$absent ? null : cached;
    }

    @Redirect(
            method = "getMetadata(Lnet/minecraft/util/ResourceLocation;)Lteam/chisel/ctm/client/texture/IMetadataSectionCTM;",
            at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    )
    private static Object stellar_core$redirectPut(final Map<ResourceLocation, IMetadataSectionCTM> map, final Object key, final Object value) {
        return map.put((ResourceLocation) key, value == null ? stellar_core$absent : (IMetadataSectionCTM) value);
    }

}
