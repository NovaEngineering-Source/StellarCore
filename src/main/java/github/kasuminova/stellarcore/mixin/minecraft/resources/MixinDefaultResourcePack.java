package github.kasuminova.stellarcore.mixin.minecraft.resources;

import github.kasuminova.stellarcore.mixin.util.StellarCoreResourcePack;
import net.minecraft.client.resources.DefaultResourcePack;
import net.minecraft.client.resources.ResourceIndex;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(DefaultResourcePack.class)
public abstract class MixinDefaultResourcePack implements StellarCoreResourcePack {

    @Shadow
    @Nullable
    protected abstract InputStream getResourceStream(final ResourceLocation location);

    @Final
    @Shadow
    private ResourceIndex resourceIndex;

    @Unique
    private final Map<ResourceLocation, Boolean> stellar_core$resourceExistsCache = new ConcurrentHashMap<>();

    @Unique
    private boolean stellar_core$cacheEnabled = false;

    /**
     * @author Kasumi_Nova
     * @reason Cache
     */
    @Inject(method = "resourceExists", at = @At("HEAD"), cancellable = true)
    public void resourceExists(final ResourceLocation location, final CallbackInfoReturnable<Boolean> cir) {
        if (!stellar_core$cacheEnabled) {
            return;
        }
        cir.setReturnValue(stellar_core$resourceExistsCache.computeIfAbsent(location, (key) ->
                this.getResourceStream(location) != null || this.resourceIndex.isFileExisting(location)));
    }

    @Unique
    @Override
    public void stellar_core$onReload() {
        stellar_core$resourceExistsCache.clear();
    }

    @Override
    public void stellar_core$enableCache() {
        stellar_core$cacheEnabled = true;
    }

    @Override
    public void stellar_core$disableCache() {
        stellar_core$resourceExistsCache.clear();
        stellar_core$cacheEnabled = false;
    }

}
