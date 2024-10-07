package github.kasuminova.stellarcore.mixin.minecraft.itemstack;

import github.kasuminova.stellarcore.common.itemstack.ItemStackCapInitTask;
import github.kasuminova.stellarcore.common.itemstack.ItemStackCapInitializer;
import github.kasuminova.stellarcore.mixin.util.StellarItemStackCapLoader;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityDispatcher;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.event.ForgeEventFactory;
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

    @Shadow
    private int stackSize;

    @Shadow
    int itemDamage;

    @Shadow
    private NBTTagCompound stackTagCompound;

    @Shadow
    private boolean isEmpty;

    @Shadow
    public abstract int getAnimationsToGo();

    @Shadow
    public abstract Item getItem();

    @Unique
    private volatile ItemStackCapInitTask stellar_core$capInitTask = null;

    @Unique
    private volatile boolean stellar_core$capabilityLoading = false;

    @Unique
    private volatile CapabilityDispatcher stellar_core$capabilities = null;

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
            if (item != Items.AIR) {
                this.stellar_core$capInitTask = new ItemStackCapInitTask((ItemStack) (Object) this);
                ItemStackCapInitializer.INSTANCE.addTask(this.stellar_core$capInitTask);
            }
        }
    }

    // ===========================================================================
    // Interface Implementation
    // ===========================================================================

    @Override
    @SuppressWarnings("DataFlowIssue")
    public void stellar_core$initCap() {
        Item item = getItem();
        if (item != Items.AIR) {
            ICapabilityProvider provider = item.initCapabilities((ItemStack) (Object) this, this.capNBT);
            this.stellar_core$capabilities = ForgeEventFactory.gatherCapabilities((ItemStack) (Object) this, provider);
        }
    }

    @Override
    public void stellar_core$joinCapInit() {
        this.capabilities = this.stellar_core$capabilities;
        this.stellar_core$capabilities = null;
        if (this.capNBT != null && this.capabilities != null) {
            this.capabilities.deserializeNBT(this.capNBT);
        }
    }

    // ===========================================================================
    // Capability Load Injection
    // ===========================================================================

    @Inject(method = "writeToNBT", at = @At("HEAD"))
    private void injectWriteToNBT(final NBTTagCompound nbt, final CallbackInfoReturnable<NBTTagCompound> cir) {
        stellar_core$ensureCapInitialized();
    }

    /**
     * @author Kasumi_Nova
     * @reason Async cap init.
     */
    @Overwrite
    public ItemStack copy() {
//        stellar_core$ensureCapInitialized();
        ItemStack stack = new ItemStack(this.item, this.stackSize, this.itemDamage, this.capabilities != null ? this.capabilities.serializeNBT() : capNBT);
        stack.setAnimationsToGo(this.getAnimationsToGo());

        if (this.stackTagCompound != null) {
            stack.setTagCompound(this.stackTagCompound.copy());
        }

        return stack;
    }

    @Inject(method = "hasCapability", at = @At("HEAD"), remap = false, cancellable = true)
    private void injectHasCapability(final Capability<?> capability, final EnumFacing facing, final CallbackInfoReturnable<Boolean> cir) {
        if (isEmpty) {
            cir.setReturnValue(false);
        }
        stellar_core$ensureCapInitialized();
    }

    @Inject(method = "getCapability", at = @At("HEAD"), remap = false, cancellable = true)
    private void injectGetCapability(final Capability<?> capability, final EnumFacing facing, final CallbackInfoReturnable<Object> cir) {
        if (isEmpty) {
            cir.setReturnValue(null);
        }
        stellar_core$ensureCapInitialized();
    }

    @Inject(method = "areCapsCompatible", at = @At("HEAD"), remap = false)
    private void injectAreCapsCompatible(final ItemStack other, final CallbackInfoReturnable<Boolean> cir) {
        stellar_core$ensureCapInitialized();
        ((StellarItemStackCapLoader) (Object) other).stellar_core$ensureCapInitialized();
    }

    @SuppressWarnings("RedundantCast")
    @Unique
    public void stellar_core$ensureCapInitialized() {
        if (this.stellar_core$capabilityLoading || this.capabilities != null) {
            return;
        }

        if (this.stellar_core$capInitTask != null) {
            synchronized ((Object) this) {
                if (this.stellar_core$capInitTask == null) {
                    return;
                }

                this.stellar_core$capabilityLoading = true;
                try {
                    this.stellar_core$capInitTask.join();
                    this.stellar_core$capInitTask = null;
                } catch (NullPointerException ignored) { // If task is already done?
                }
                this.stellar_core$capabilityLoading = false;
            }
        }
    }

}
