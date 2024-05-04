package github.kasuminova.stellarcore.mixin.util;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;

public interface IIThing {

    boolean callIs(@Nullable Item var1);

    boolean callIs(@Nullable ItemStack var1);

    boolean callIs(@Nullable Block var1);

}
