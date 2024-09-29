package github.kasuminova.stellarcore.mixin.minecraft.itemstack;

import github.kasuminova.stellarcore.common.itemstack.ItemStackCapInitTask;
import github.kasuminova.stellarcore.common.itemstack.ItemStackCapInitializer;
import github.kasuminova.stellarcore.mixin.util.StellarItemStackCapLoader;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityDispatcher;
import net.minecraftforge.registries.IRegistryDelegate;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(ItemStack.class)
public abstract class MixinItemStack implements StellarItemStackCapLoader {

    @Nullable
    @Shadow(remap = false)
    protected abstract Item getItemRaw();

    @Shadow(remap = false)
    private IRegistryDelegate<Item> delegate;

    @Shadow(remap = false)
    private CapabilityDispatcher capabilities;

    @Shadow(remap = false)
    private NBTTagCompound capNBT;

    @Final
    @Shadow
    private Item item;

    @Unique
    private ItemStackCapInitTask stellar_core$capInitTask = null;

    @Unique
    private boolean stellar_core$capabilityLoading = false;

    // ===========================================================================
    // Capability Init Injection
    // ===========================================================================

    /**
     * @author Kasumi_Nova
     * @reason Async capability loading.
     */
    @Overwrite(remap = false)
    private void forgeInit() {
        Item item = getItemRaw();
        if (item != null) {
            this.delegate = item.delegate;
            this.stellar_core$capInitTask = new ItemStackCapInitTask((ItemStack) (Object) this);
            ItemStackCapInitializer.INSTANCE.addTask(this.stellar_core$capInitTask);
        }
    }

    // ===========================================================================
    // Interface Implementation
    // ===========================================================================

    @Override
    @SuppressWarnings("DataFlowIssue")
    public void stellar_core$initCap() {
        net.minecraftforge.common.capabilities.ICapabilityProvider provider = item.initCapabilities((ItemStack) (Object) this, this.capNBT);
        this.capabilities = net.minecraftforge.event.ForgeEventFactory.gatherCapabilities((ItemStack) (Object) this, provider);
        if (this.capNBT != null && capabilities != null) {
            this.capabilities.deserializeNBT(this.capNBT);
        }
    }

    // ===========================================================================
    // Capability Load Injection
    // ===========================================================================

    @Inject(method = "writeToNBT", at = @At("HEAD"))
    private void injectWriteToNBT(final NBTTagCompound nbt, final CallbackInfoReturnable<NBTTagCompound> cir) {
        stellar_core$validateCapInitialized();
    }

    @Inject(method = "copy", at = @At("HEAD"))
    private void injectCopy(final CallbackInfoReturnable<ItemStack> cir) {
        stellar_core$validateCapInitialized();
    }

    @Inject(method = "hasCapability", at = @At("HEAD"), remap = false)
    private void injectHasCapability(final Capability<?> capability, final EnumFacing facing, final CallbackInfoReturnable<Boolean> cir) {
        stellar_core$validateCapInitialized();
    }

    @Inject(method = "getCapability", at = @At("HEAD"), remap = false)
    private void injectGetCapability(final Capability<?> capability, final EnumFacing facing, final CallbackInfoReturnable<Object> cir) {
        stellar_core$validateCapInitialized();
    }

    @Inject(method = "areCapsCompatible", at = @At("HEAD"), remap = false)
    private void injectAreCapsCompatible(final ItemStack other, final CallbackInfoReturnable<Boolean> cir) {
        stellar_core$validateCapInitialized();
    }

    @Unique
    private void stellar_core$validateCapInitialized() {
        if (this.stellar_core$capabilityLoading) {
            return;
        }
        if (this.capabilities == null && this.stellar_core$capInitTask != null) {
            this.stellar_core$capabilityLoading = true;
            this.stellar_core$capInitTask.join();
            this.stellar_core$capInitTask = null;
            this.stellar_core$capabilityLoading = false;
        }
    }

}
