package github.kasuminova.stellarcore.mixin.ctm;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import github.kasuminova.stellarcore.client.model.JsonElementDeserialization;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.reflect.Type;

// Both deserializers are package-private implementation classes.
@Mixin(targets = {
    "team.chisel.ctm.client.util.BlockstatePredicateParser$PredicateDeserializer",
    "team.chisel.ctm.client.util.BlockstatePredicateParser$MapDeserializer"
}, remap = false)
public abstract class MixinBlockstatePredicateDeserializer {

    @Redirect(method = "deserialize", at = @At(value = "INVOKE",
        target = "Lcom/google/gson/JsonDeserializationContext;deserialize(Lcom/google/gson/JsonElement;Ljava/lang/reflect/Type;)Ljava/lang/Object;"), require = 1)
    private Object stellar_core$reuseTreeReader(final JsonDeserializationContext context, final JsonElement json, final Type type) {
        return JsonElementDeserialization.deserialize(context, json, type);
    }
}
