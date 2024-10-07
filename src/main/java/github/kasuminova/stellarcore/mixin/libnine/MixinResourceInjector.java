package github.kasuminova.stellarcore.mixin.libnine;

import github.kasuminova.stellarcore.shaded.org.jctools.maps.NonBlockingHashMap;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@SuppressWarnings({"ValueOfIncrementOrDecrementUsed", "SynchronizeOnNonFinalField"})
@Mixin(targets = "io.github.phantamanta44.libnine.client.model.ParameterizedItemModelLoader$ResourceInjector", remap = false)
public class MixinResourceInjector {

    @Final
    @Shadow
    @Mutable
    private Map<String, String> resources;

    @Shadow private long resourceIndex;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void injectInit(final CallbackInfo ci) {
        this.resources = new NonBlockingHashMap<>();
    }

    /**
     * @author Kasumi_Nova
     * @reason Thread safe.
     */
    @Overwrite
    ResourceLocation injectResource(String resource) {
        synchronized (resources) {
            this.resources.put("models/" + this.resourceIndex + ".json", resource);
            return new ResourceLocation("libnine_pi", Long.toString(this.resourceIndex++));
        }
    }

}
