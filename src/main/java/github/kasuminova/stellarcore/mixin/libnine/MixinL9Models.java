package github.kasuminova.stellarcore.mixin.libnine;

import com.google.gson.stream.JsonReader;
import github.kasuminova.stellarcore.client.integration.libnine.L9ModelSelection;
import github.kasuminova.stellarcore.client.integration.libnine.L9ModelTypeReader;
import github.kasuminova.stellarcore.client.resource.ResourceExistingCache;
import github.kasuminova.stellarcore.mixin.util.StellarCoreResourcePack;
import io.github.phantamanta44.libnine.client.model.L9Models;
import io.github.phantamanta44.libnine.util.helper.ResourceUtils;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

import java.io.FileNotFoundException;
import java.io.InputStreamReader;

@Mixin(value = L9Models.class, remap = false)
public class MixinL9Models implements StellarCoreResourcePack {

    @Unique
    private static final Object stellar_core$NO_MODEL_TYPE = new Object();

    static {
        // L9Models.registerModels() is static and constructs its two loaders, never an L9Models.
        // Register one persistent owner when the optional LibNine mixin is initialized.
        ResourceExistingCache.addResourcePack((StellarCoreResourcePack) (Object) new L9Models());
    }

    /**
     * @author StellarCore
     * @reason Cache model type only inside the resource-loading lifecycle.
     *
     */
    @Overwrite
    public static boolean isOfType(final ResourceLocation resource, final String type) {
        final Object modelType = L9ModelSelection.read(resource, MixinL9Models::stellar_core$readModelType);
        return modelType != null && modelType != stellar_core$NO_MODEL_TYPE && modelType.equals(type);
    }

    @Unique
    private static Object stellar_core$readModelType(final ResourceLocation location) {
        try (IResource resource = ResourceUtils.getResource(location)) {
            final JsonReader reader = new JsonReader(new InputStreamReader(resource.getInputStream()));
            final String type = L9ModelTypeReader.read(reader);
            return type != null ? type : stellar_core$NO_MODEL_TYPE;
        } catch (FileNotFoundException missing) {
            return stellar_core$NO_MODEL_TYPE;
        } catch (Exception invalid) {
            return null;
        }
    }

    @Override
    public void stellar_core$onReload() {
        L9ModelSelection.reload();
    }

    @Override
    public void stellar_core$disableCache() {
        L9ModelSelection.disable();
    }

    @Override
    public void stellar_core$enableCache() {
        L9ModelSelection.enable();
    }

    @Override
    public boolean stellar_core$isPersistent() {
        return true;
    }

}
