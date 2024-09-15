package github.kasuminova.stellarcore.mixin.tlm;

import com.github.tartaricacid.touhoulittlemaid.client.model.pojo.CubesItem;
import github.kasuminova.stellarcore.client.pool.TLMCubesItemPool;
import github.kasuminova.stellarcore.mixin.util.CanonicalizationCubesItem;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

@Mixin(value = CubesItem.class, remap = false)
public class MixinCubesItem implements CanonicalizationCubesItem {

    @Shadow
    private List<Float> uv;

    @Shadow
    private List<Float> size;

    @Shadow
    private List<Float> origin;

    @Unique
    @Override
    public void stellar_core$canonicalize() {
        uv = TLMCubesItemPool.canonicalize(uv);
        size = TLMCubesItemPool.canonicalize(size);
        origin = TLMCubesItemPool.canonicalize(origin);
        // Ver 1.2.5
        stellar_core$canonicalizeRotation();
        stellar_core$canonicalizePivot();
    }

    @Unique
    @SuppressWarnings("DataFlowIssue")
    private void stellar_core$canonicalizeRotation() {
        try {
            List<Float> rotation = ObfuscationReflectionHelper.getPrivateValue(CubesItem.class, (CubesItem) ((Object) this), "rotation");
            ObfuscationReflectionHelper.setPrivateValue(CubesItem.class, (CubesItem) ((Object) this), TLMCubesItemPool.canonicalize(rotation), "rotation");
        } catch (Throwable ignored) {
        }
    }

    @Unique
    @SuppressWarnings("DataFlowIssue")
    private void stellar_core$canonicalizePivot() {
        try {
            List<Float> pivot = ObfuscationReflectionHelper.getPrivateValue(CubesItem.class, (CubesItem) ((Object) this), "pivot");
            ObfuscationReflectionHelper.setPrivateValue(CubesItem.class, (CubesItem) ((Object) this), TLMCubesItemPool.canonicalize(pivot), "pivot");
        } catch (Throwable ignored) {
        }
    }

}
