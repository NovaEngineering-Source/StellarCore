package github.kasuminova.stellarcore.mixin.minecraft.nbtmap;

import github.kasuminova.stellarcore.mixin.util.AccessorNBTTagCompound;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(NBTTagCompound.class)
public class MixinNBTTagCompound implements AccessorNBTTagCompound {

    @Mutable
    @Shadow
    @Final
    private Map<String, NBTBase> tagMap;

    @Redirect(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/google/common/collect/Maps;newHashMap()Ljava/util/HashMap;",
                    remap = false
            )
    )
    private HashMap<String, NBTBase> injectInitNewHashMap() {
        return null;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void injectInit(final CallbackInfo ci) {
        this.tagMap = new Object2ObjectOpenHashMap<>();
    }

    /**
     * @author Kasumi_Nova
     * @reason Use clone to copy tag faster.
     */
    @Overwrite
    public NBTTagCompound copy() {
        NBTTagCompound copied = new NBTTagCompound();

        AccessorNBTTagCompound accessor = (AccessorNBTTagCompound) copied;

        // Overwrite tagMap.
        Object2ObjectOpenHashMap<String, NBTBase> cloned = ((Object2ObjectOpenHashMap<String, NBTBase>) this.tagMap).clone();
        accessor.stellar_core$setTagMap(cloned);

        // Copy values.
        for (final Object2ObjectMap.Entry<String, NBTBase> entry : cloned.object2ObjectEntrySet()) {
            entry.setValue(entry.getValue().copy());
        }

        return copied;
    }

    @Unique
    @Override
    public Map<String, NBTBase> stellar_core$getTagMap() {
        return tagMap;
    }

    @Unique
    @Override
    public void stellar_core$setTagMap(final Map<String, NBTBase> tagMap) {
        this.tagMap = tagMap;
    }

}
