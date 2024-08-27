package github.kasuminova.stellarcore.mixin.minecraft.resources;

import github.kasuminova.stellarcore.mixin.util.StellarCoreResourcePackReloadListener;
import net.minecraft.client.resources.AbstractResourcePack;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(AbstractResourcePack.class)
public abstract class MixinAbstractResourcePack implements StellarCoreResourcePackReloadListener {

    @Shadow
    protected abstract boolean hasResourceName(final String name);

    @Shadow
    private static String locationToName(final ResourceLocation location) {
        return null;
    }

    @Unique
    private final Map<ResourceLocation, Boolean> stellar_core$resourceExistsCache = new ConcurrentHashMap<>();

    /**
     * @author Kasumi_Nova
     * @reason Cache
     */
    @Inject(method = "resourceExists", at = @At("HEAD"), cancellable = true)
    public void injectResourceExists(final ResourceLocation location, final CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(stellar_core$resourceExistsCache.computeIfAbsent(location, (key) -> this.hasResourceName(locationToName(location))));
    }

    @Unique
    @Override
    public void stellar_core$onReload() {
        stellar_core$resourceExistsCache.clear();
    }

}
