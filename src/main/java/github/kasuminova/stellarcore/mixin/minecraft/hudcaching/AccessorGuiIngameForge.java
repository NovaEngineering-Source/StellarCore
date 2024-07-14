package github.kasuminova.stellarcore.mixin.minecraft.hudcaching;

import net.minecraftforge.client.GuiIngameForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GuiIngameForge.class)
public interface AccessorGuiIngameForge {

    @Invoker(remap = false)
    void callRenderCrosshairs(float partialTicks);

}
