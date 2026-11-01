package github.kasuminova.stellarcore.mixin.minecraft.forge.parallelmodelloader;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import github.kasuminova.stellarcore.client.model.JsonElementDeserialization;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.reflect.Type;

// These vanilla deserializers are package-private and are not used as Java types here.
@Mixin(targets = {
    "net.minecraft.client.renderer.block.model.BlockPart$Deserializer",
    "net.minecraft.client.renderer.block.model.BlockPartFace$Deserializer",
    "net.minecraft.client.renderer.block.model.ItemCameraTransforms$Deserializer"
}, remap = false)
public abstract class MixinPrivateModelJsonDeserializers {

    @Redirect(method = "*", at = @At(value = "INVOKE",
        target = "Lcom/google/gson/JsonDeserializationContext;deserialize(Lcom/google/gson/JsonElement;Ljava/lang/reflect/Type;)Ljava/lang/Object;"), require = 1)
    private Object stellar_core$reuseTreeReader(final JsonDeserializationContext context, final JsonElement json, final Type type) {
        return JsonElementDeserialization.deserialize(context, json, type);
    }
}
