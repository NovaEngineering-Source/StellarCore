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
 * Makes {@code UCWFakeResourcePack} an honest {@link IResourcePack}.
 *
 * <p>Vanilla routes every resource query by the domains a pack <em>declares</em>:
 * {@code UCWFakePack} declares only {@code ucw_generated}, so vanilla never asks it
 * about {@code minecraft:}, {@code tconstruct:} or any other namespace. Its own
 * {@code resourceExists}, however, answers {@code true} for <em>any</em> {@code .png}
 * path regardless of namespace (its pixel data is produced by custom sprite loaders,
 * so it hands out empty streams and expects nothing to read them). For declared
 * domains that is harmless; for every other namespace it turns a plain
 * "resource not found" into "resource exists, zero bytes".</p>
 *
 * <p>Anything that trusts {@code resourceExists} then breaks. StellarCore's mutable-pack
 * late discovery rebuilds a domain's resource chain around packs that answer a probe,
 * so one probe answered by this pack blanks out whole namespaces with empty streams -
 * every sprite in them becomes 0x0 with no frames, models referencing them bake as
 * flat missing-texture planes, and blending model code reading their pixel data dies
 * on an empty frame list. Vanilla paths degrade the same way wherever the empty stream
 * is preferred over a clean "not found".</p>
 *
 * <p>The restriction here restores the normal contract: the pack may only claim and
 * serve resources inside the domains it declares. Its own {@code ucw_generated}
 * behaviour (empty png streams backed by custom loaders, proxied json) is untouched.</p>
 */
@Mixin(value = UCWFakeResourcePack.class, remap = false)
public abstract class MixinUCWFakeResourcePack implements IResourcePack {

    @Inject(method = "resourceExists", at = @At("HEAD"), cancellable = true, remap = false)
    public void stellar_core$restrictResourceExists(final ResourceLocation location, final CallbackInfoReturnable<Boolean> cir) {
        if (location != null && !this.getResourceDomains().contains(location.getNamespace())) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getInputStream", at = @At("HEAD"), remap = false)
    public void stellar_core$restrictGetInputStream(final ResourceLocation location, final CallbackInfoReturnable<InputStream> cir) throws FileNotFoundException {
        if (location != null && !this.getResourceDomains().contains(location.getNamespace())) {
            throw new FileNotFoundException(location.toString());
        }
    }

}
