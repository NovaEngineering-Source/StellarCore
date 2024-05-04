package github.kasuminova.stellarcore.mixin.endercore;

import github.kasuminova.stellarcore.mixin.util.IIThing;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import javax.annotation.Nullable;

@Mixin(targets = "com.enderio.core.common.util.stackable.IThing")
public interface InvokerIThing extends IIThing {

    @Override
    @Invoker(remap = false)
    boolean callIs(@Nullable Item var1);

    @Override
    @Invoker(remap = false)
    boolean callIs(@Nullable ItemStack var1);

    @Override
    @Invoker(remap = false)
    boolean callIs(@Nullable Block var1);

}
