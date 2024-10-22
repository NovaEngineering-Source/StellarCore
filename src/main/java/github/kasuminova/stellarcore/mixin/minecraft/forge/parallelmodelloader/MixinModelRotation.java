package github.kasuminova.stellarcore.mixin.minecraft.forge.parallelmodelloader;

import net.minecraft.client.renderer.block.model.ModelRotation;
import net.minecraftforge.common.model.TRSRTransformation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import javax.vecmath.Matrix4f;

@Mixin(ModelRotation.class)
public class MixinModelRotation {

    /**
     * @author Kasumi_Nova
     * @reason Optifine fix
     */
    @Overwrite(remap = false)
    public Matrix4f getMatrix() {
        return TRSRTransformation.from((ModelRotation) (Object) this).getMatrix();
    }

}
