package github.kasuminova.stellarcore.mixin.ucw;

import net.minecraft.client.resources.IResourcePack;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.asie.ucw.UCWFakeResourcePack;

import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * 糟糕的对所有png均返回true
 */
@Mixin(UCWFakeResourcePack.class)
public abstract class MixinUCWFakeResourcePack implements IResourcePack {

    @Inject(method = "resourceExists", at = @At("HEAD"), cancellable = true)
    public void stellar_core$restrictResourceExists(final ResourceLocation location, final CallbackInfoReturnable<Boolean> cir) {
        if (location != null && !this.getResourceDomains().contains(location.getNamespace())) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }

    @Inject(method = "getInputStream", at = @At("HEAD"))
    public void stellar_core$restrictGetInputStream(final ResourceLocation location, final CallbackInfoReturnable<InputStream> cir) throws FileNotFoundException {
        if (location != null && !this.getResourceDomains().contains(location.getNamespace())) {
            throw new FileNotFoundException(location.toString());
        }
    }

}
