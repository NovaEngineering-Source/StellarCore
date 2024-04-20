package github.kasuminova.stellarcore.mixin.minecraft.noglerror;

import net.minecraft.client.renderer.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(GlStateManager.class)
public class MixinGlStateManager {

    /**
     * @author Kasumi_Nova
     * @reason NoGLError
     */
    @Overwrite(remap = false)
    public static int glGetError() {
        return 0;
    }

}
