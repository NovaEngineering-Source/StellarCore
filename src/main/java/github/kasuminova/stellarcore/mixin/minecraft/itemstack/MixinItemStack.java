package github.kasuminova.stellarcore.mixin.minecraft.itemstack;

import github.kasuminova.stellarcore.common.itemstack.ItemStackCapInitTask;
import github.kasuminova.stellarcore.common.itemstack.ItemStackCapInitializer;
import github.kasuminova.stellarcore.common.itemstack.SharedEmptyTag;
import github.kasuminova.stellarcore.mixin.util.StellarItemStack;
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
public abstract class MixinItemStack implements StellarItemStack {

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
        if (this.stellar_core$capabilities != null) {
            return;
        }

        Item item = getItem();
        if (item != Items.AIR) {
            ICapabilityProvider provider = item.initCapabilities((ItemStack) (Object) this, this.capNBT);
            this.stellar_core$capabilities = ForgeEventFactory.gatherCapabilities((ItemStack) (Object) this, provider);
        }
    }

    @Override
    public void stellar_core$joinCapInit() {
        if (this.capabilities != null || this.stellar_core$capabilities == null) {
            return;
        }

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
        stellar_core$ensureCapNBTInitialized();
        ItemStack stack = new ItemStack(this.item, this.stackSize, this.itemDamage, this.capNBT);
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

        Object cap = this.capabilities == null ? null : this.capabilities.getCapability(capability, facing);
        cir.setReturnValue(cap);
    }

    @SuppressWarnings("DataFlowIssue")
    @Inject(method = "areCapsCompatible", at = @At("HEAD"), remap = false, cancellable = true)
    private void injectAreCapsCompatible(final ItemStack other, final CallbackInfoReturnable<Boolean> cir) {
        stellar_core$ensureCapInitialized();
        StellarItemStack otherAccessor = (StellarItemStack) (Object) other;
        otherAccessor.stellar_core$ensureCapInitialized();

        NBTTagCompound capNBT = stellar_core$getCapNBT();
        NBTTagCompound otherCapNBT = otherAccessor.stellar_core$getCapNBT();
        cir.setReturnValue(capNBT == otherCapNBT || capNBT.equals(otherCapNBT));
    }

    @Unique
    public void stellar_core$ensureCapInitialized() {
        if (this.capabilities != null) {
            return;
        }

        if (this.stellar_core$capInitTask != null) {
            if (this.stellar_core$capInitTask == null) {
                return;
            }

            try {
                if (this.stellar_core$capInitTask.join()) {
                    this.stellar_core$capInitTask = null;
                }
            } catch (NullPointerException ignored) { // If task is already done?
            }
        }
    }

    @Unique
    public void stellar_core$ensureCapNBTInitialized() {
        if (this.capabilities != null) {
            this.capNBT = this.capabilities.serializeNBT();
        }
    }

    @Override
    public NBTTagCompound stellar_core$getCapNBT() {
        stellar_core$ensureCapNBTInitialized();
        return this.capNBT == null || this.capNBT.isEmpty() ? SharedEmptyTag.EMPTY_TAG : this.capNBT;
    }

    @Override
    public CapabilityDispatcher stellar_core$getCap() {
        return capabilities;
    }

}
