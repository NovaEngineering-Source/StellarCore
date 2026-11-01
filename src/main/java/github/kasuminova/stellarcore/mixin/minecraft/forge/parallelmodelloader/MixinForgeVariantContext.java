package github.kasuminova.stellarcore.mixin.minecraft.forge.parallelmodelloader;

import github.kasuminova.stellarcore.common.util.VariantSubmodelContext;
import net.minecraftforge.client.model.ForgeBlockStateV1;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ForgeBlockStateV1.Variant.Deserializer.class, remap = false)
public abstract class MixinForgeVariantContext implements VariantSubmodelContext {

    // One reusable cell per parser/thread; setting null does not allocate a new ThreadLocal entry.
    @Unique
    private final ThreadLocal<String[]> stellar_core$submodelKey = ThreadLocal.withInitial(() -> new String[1]);

    @Override
    public String stellar_core$getSubmodelKey() {
        return this.stellar_core$submodelKey.get()[0];
    }

    @Override
    public void stellar_core$setSubmodelKey(final String key) {
        this.stellar_core$submodelKey.get()[0] = key;
    }

    @Redirect(method = "deserialize(Lcom/google/gson/JsonElement;Ljava/lang/reflect/Type;Lcom/google/gson/JsonDeserializationContext;)Lnet/minecraftforge/client/model/ForgeBlockStateV1$Variant;", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
        target = "Lnet/minecraftforge/client/model/ForgeBlockStateV1$Variant$Deserializer;simpleSubmodelKey:Ljava/lang/String;"), require = 1)
    private String stellar_core$readSubmodelKey(final ForgeBlockStateV1.Variant.Deserializer parser) {
        return ((VariantSubmodelContext) parser).stellar_core$getSubmodelKey();
    }

    @Redirect(method = "deserialize(Lcom/google/gson/JsonElement;Ljava/lang/reflect/Type;Lcom/google/gson/JsonDeserializationContext;)Lnet/minecraftforge/client/model/ForgeBlockStateV1$Variant;", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD,
        target = "Lnet/minecraftforge/client/model/ForgeBlockStateV1$Variant$Deserializer;simpleSubmodelKey:Ljava/lang/String;"), require = 1)
    private void stellar_core$clearSubmodelKey(final ForgeBlockStateV1.Variant.Deserializer parser, final String key) {
        ((VariantSubmodelContext) parser).stellar_core$setSubmodelKey(key);
    }
}
