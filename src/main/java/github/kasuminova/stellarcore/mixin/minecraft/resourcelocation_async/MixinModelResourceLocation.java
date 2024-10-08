package github.kasuminova.stellarcore.mixin.minecraft.resourcelocation_async;

import github.kasuminova.stellarcore.common.pool.LowerCaseStringPool;
import github.kasuminova.stellarcore.common.pool.DeferredCanonicalizable;
import github.kasuminova.stellarcore.mixin.util.AccessorResourceLocation;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 一秒 800 万对象，让我们把内存烧成灰。
 */
@Mixin(ModelResourceLocation.class)
public class MixinModelResourceLocation extends ResourceLocation implements DeferredCanonicalizable<String> {

    @Final
    @Shadow
    @Mutable
    private String variant;

    @SuppressWarnings("DataFlowIssue")
    public MixinModelResourceLocation() {
        super(null);
    }

    @SuppressWarnings({"RedundantCast", "unchecked"})
    @Inject(method = "<init>(I[Ljava/lang/String;)V", at = @At("RETURN"))
    private void injectInit(final int unused, final String[] resourceName, final CallbackInfo ci) {
        if (((ModelResourceLocation) (Object) this).getClass() == ModelResourceLocation.class) {
            LowerCaseStringPool.INSTANCE.canonicalizeDeferred((DeferredCanonicalizable<String>) (Object) this);
        }
    }

    @Override
    @SuppressWarnings({"RedundantCast", "AddedMixinMembersNamePattern"})
    public void canonicalizeAsync() {
        AccessorResourceLocation accessor = (AccessorResourceLocation) (Object) this;
        accessor.stellar_core$setNamespace(LowerCaseStringPool.INSTANCE.canonicalize(this.namespace));
        accessor.stellar_core$setPath(LowerCaseStringPool.INSTANCE.canonicalize(this.path));
        this.variant = LowerCaseStringPool.INSTANCE.canonicalize(this.variant);
    }

}
