package github.kasuminova.stellarcore.mixin.minecraft;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.VertexFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nonnull;

@Mixin(Tessellator.class)
public class MixinTessellator {

    @Redirect(method = "<init>", at = @At(value = "NEW", target = "(I)Lnet/minecraft/client/renderer/BufferBuilder;"))
    private BufferBuilder redirectNewBuffer(final int bufferSizeIn) {
        return new BufferBuilder(bufferSizeIn) {

            Thread thread = null;

            @Override
            public void begin(final int glMode, @Nonnull final VertexFormat format) {
                if (thread != null && thread != Thread.currentThread()) {
                    thread = null;
                    throw new IllegalStateException("Using Tessellator buffer in another thread.");
                }
                thread = Thread.currentThread();
                super.begin(glMode, format);
            }

            @Override
            public void finishDrawing() {
                if (thread != null && thread != Thread.currentThread()) {
                    thread = null;
                    throw new IllegalStateException("Using Tessellator buffer in another thread.");
                }
                thread = null;
                super.finishDrawing();
            }
        };
    }

}
