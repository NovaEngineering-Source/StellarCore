package github.kasuminova.stellarcore.mixin.endercore;

import com.enderio.core.common.util.stackable.Things;
import github.kasuminova.stellarcore.mixin.util.IIThing;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * The baked IThing implementations already index their members. Do not duplicate their lookup caches.
 */
@Mixin(Things.class)
public class MixinThings {
    @Final
    @Nonnull
    @Shadow(remap = false)
    private List<Object> things;

    @Shadow(remap = false)
    private NBTTagCompound nbt;

    /**
     * @author StellarCore @reason Query baked member indexes without a second lookup cache.
     */
    @Overwrite(remap = false)
    public boolean contains(final ItemStack stack) {
        if (stack != null && !stack.isEmpty()) {
            for (int i = 0; i < things.size(); i++) {
                if (((IIThing) things.get(i)).callIs(stack) && (nbt == null || nbt.equals(stack.getTagCompound()))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @author StellarCore @reason Query baked member indexes without an iterator.
     */
    @Overwrite(remap = false)
    public boolean contains(final Item item) {
        if (item != Items.AIR) {
            for (int i = 0; i < things.size(); i++) {
                if (((IIThing) things.get(i)).callIs(item)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @author StellarCore @reason Query baked member indexes without an iterator.
     */
    @Overwrite(remap = false)
    public boolean contains(final Block block) {
        if (block != Blocks.AIR) {
            for (int i = 0; i < things.size(); i++) {
                if (((IIThing) things.get(i)).callIs(block)) {
                    return true;
                }
            }
        }
        return false;
    }
}
