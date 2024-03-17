package github.kasuminova.stellarcore.mixin.enderioconduits;

import com.enderio.core.common.util.NNList;
import crazypants.enderio.conduits.conduit.item.ItemConduit;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.mixin.util.CachedItemConduit;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemConduit.class)
public abstract class MixinItemConduit implements CachedItemConduit {

    @Unique
    private ItemStack stellarcore$cachedStack = ItemStack.EMPTY;

    @Inject(method = "writeToNBT", at = @At("TAIL"), remap = false)
    public void onWriteToNBT(final NBTTagCompound nbtRoot, final CallbackInfo ci) {
        if (!StellarCoreConfig.BUG_FIXES.enderIOConduits.cachedItemConduit) {
            return;
        }
        if (!stellarcore$cachedStack.isEmpty()) {
            NBTTagCompound cachedStackTag = new NBTTagCompound();

            stellarcore$cachedStack.writeToNBT(cachedStackTag);
            if (stellarcore$cachedStack.getCount() > 64) {
                cachedStackTag.setInteger("Count", stellarcore$cachedStack.getCount());
            }

            nbtRoot.setTag("cachedStack", cachedStackTag);
        }
    }

    @Inject(method = "readFromNBT", at = @At("TAIL"), remap = false)
    public void onReadFromNBT(final NBTTagCompound nbtRoot, final CallbackInfo ci) {
        if (!StellarCoreConfig.BUG_FIXES.enderIOConduits.cachedItemConduit) {
            return;
        }
        if (nbtRoot.hasKey("cachedStack")) {
            NBTTagCompound cachedStackTag = nbtRoot.getCompoundTag("cachedStack");
            stellarcore$cachedStack = new ItemStack(cachedStackTag);
            stellarcore$cachedStack.setCount(cachedStackTag.getInteger("Count"));
        }
    }

    @Inject(method = "getDrops", at = @At("RETURN"), remap = false)
    public void onGetDrops(final CallbackInfoReturnable<NNList<ItemStack>> cir) {
        if (!StellarCoreConfig.BUG_FIXES.enderIOConduits.cachedItemConduit) {
            return;
        }
        if (!stellarcore$cachedStack.isEmpty()) {
            cir.getReturnValue().add(stellarcore$cachedStack);
        }
    }

    @Override
    @SuppressWarnings("AddedMixinMembersNamePattern")
    public ItemStack getCachedStack() {
        return stellarcore$cachedStack;
    }

    @Override
    @SuppressWarnings("AddedMixinMembersNamePattern")
    public void setCachedStack(final ItemStack cachedStack) {
        this.stellarcore$cachedStack = cachedStack;
    }

}
