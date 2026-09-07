package github.kasuminova.stellarcore.mixin.minecraft.forge.parallelmodelloader;

import github.kasuminova.stellarcore.mixin.util.StellarCoreProgressBar;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.ProgressManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ProgressManager.ProgressBar.class, remap = false)
public abstract class MixinProgressBar implements StellarCoreProgressBar {

    @Shadow
    private volatile int step;

    @Shadow
    private volatile String message;

    @Shadow
    public abstract int getSteps();

    @Shadow
    public abstract String getTitle();

    @Override
    public void stellar_core$stepBatch(final int count, final String message) {
        if (count <= 0) {
            return;
        }
        synchronized (this) {
            final int next = this.step + count;
            if (next > this.getSteps()) {
                throw new IllegalStateException("too much steps for ProgressBar " + this.getTitle());
            }
            this.step = next;
        }
        this.message = FMLCommonHandler.instance().stripSpecialChars(message);
        FMLCommonHandler.instance().processWindowMessages();
    }
}
