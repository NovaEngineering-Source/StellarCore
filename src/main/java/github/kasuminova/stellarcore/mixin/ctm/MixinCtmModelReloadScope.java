package github.kasuminova.stellarcore.mixin.ctm;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import github.kasuminova.stellarcore.client.integration.ctm.CTMModelBakeLifecycle;
import net.minecraft.client.renderer.block.model.ModelManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SimpleReloadableResourceManager.class)
public class MixinCtmModelReloadScope {
    @WrapOperation(method = {"registerReloadListener", "notifyReloadListeners"},
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/IResourceManagerReloadListener;onResourceManagerReload(Lnet/minecraft/client/resources/IResourceManager;)V"))
    private void stellar_core$scopeModelReload(final IResourceManagerReloadListener listener,
                                               final IResourceManager manager, final Operation<Void> original) {
        if (!(listener instanceof ModelManager)) {
            original.call(listener, manager);
            return;
        }
        CTMModelBakeLifecycle.begin();
        boolean completed = false;
        try {
            original.call(listener, manager);
            completed = true;
        } finally {
            if (completed) CTMModelBakeLifecycle.complete();
            else CTMModelBakeLifecycle.abort();
        }
    }
}
