package github.kasuminova.stellarcore.mixin.minecraft.forge.parallelmodelloader;

import github.kasuminova.stellarcore.common.util.VariantSubmodelContext;
import net.minecraftforge.client.model.ForgeBlockStateV1;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ForgeBlockStateV1.Deserializer.class, remap = false)
public abstract class MixinForgeVariantContextWriter {

    @Redirect(method = "deserialize(Lcom/google/gson/JsonElement;Ljava/lang/reflect/Type;Lcom/google/gson/JsonDeserializationContext;)Lnet/minecraftforge/client/model/ForgeBlockStateV1;", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD,
        target = "Lnet/minecraftforge/client/model/ForgeBlockStateV1$Variant$Deserializer;simpleSubmodelKey:Ljava/lang/String;"), require = 1)
    private void stellar_core$setSubmodelKey(final ForgeBlockStateV1.Variant.Deserializer parser, final String key) {
        ((VariantSubmodelContext) parser).stellar_core$setSubmodelKey(key);
    }
}
