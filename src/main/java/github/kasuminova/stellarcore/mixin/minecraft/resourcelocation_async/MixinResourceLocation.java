package github.kasuminova.stellarcore.mixin.minecraft.resourcelocation_async;

import github.kasuminova.stellarcore.common.pool.DeferredCanonicalizable;
import github.kasuminova.stellarcore.common.pool.ResourceLocationPool;
import github.kasuminova.stellarcore.mixin.util.AccessorResourceLocation;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ResourceLocation.class)
public class MixinResourceLocation implements DeferredCanonicalizable<String>, AccessorResourceLocation {

    @Final
    @Shadow
    @Mutable
    protected String namespace;

    @Final
    @Shadow
    @Mutable
    protected String path;

    @SuppressWarnings({"RedundantCast", "unchecked"})
    @Inject(method = "<init>(I[Ljava/lang/String;)V", at = @At("RETURN"))
    private void injectInit(final int unused, final String[] resourceName, final CallbackInfo ci) {
        if (((ResourceLocation) (Object) this).getClass() == ResourceLocation.class) {
            ResourceLocationPool.INSTANCE.canonicalizeDeferred((DeferredCanonicalizable<String>) (Object) this);
        }
    }

    @Override
    @SuppressWarnings("AddedMixinMembersNamePattern")
    public void canonicalizeAsync() {
        this.namespace = ResourceLocationPool.INSTANCE.canonicalize(this.namespace);
        this.path = ResourceLocationPool.INSTANCE.canonicalize(this.path);
    }

    @Override
    public void stellar_core$setNamespace(final String namespace) {
        this.namespace = namespace;
    }

    @Override
    public void stellar_core$setPath(final String path) {
        this.path = path;
    }

}
