package github.kasuminova.stellarcore.mixin.minecraft.forge.oredict;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.oredict.OreDictionary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

@Mixin(value = OreDictionary.class, priority = 999,remap = false)
public class MixinOreDictionary {

    @Unique
    private static final int STELLAR_CORE$NO_ID = -1;

    @Shadow
    @Mutable
    private static Map<String, Integer> nameToId;

    @Shadow
    @Mutable
    private static Map<Integer, IntList> stackToId;

    @Shadow
    private static List<String> idToName;

    @Shadow
    private static List<NonNullList<ItemStack>> idToStack;

    @Shadow
    private static List<NonNullList<ItemStack>> idToStackUn;

    @Shadow
    private static NonNullList<ItemStack> getOres(int id) {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    @Inject(method = "<clinit>", at = @At(value = "INVOKE",
        target = "Lnet/minecraftforge/oredict/OreDictionary;initVanillaEntries()V", shift = At.Shift.BEFORE))
    private static void stellar_core$installPrimitiveMaps(final CallbackInfo ci) {
        final Object2IntOpenHashMap<String> names = new Object2IntOpenHashMap<>(128);
        names.defaultReturnValue(STELLAR_CORE$NO_ID);
        nameToId = names;
        stackToId = new Int2ObjectOpenHashMap<>((int) (128 * 0.75));
    }

    @Unique
    private static Object2IntOpenHashMap<String> stellar_core$names() {
        return (Object2IntOpenHashMap<String>) nameToId;
    }

    @Unique
    private static Int2ObjectOpenHashMap<IntList> stellar_core$stacks() {
        return (Int2ObjectOpenHashMap<IntList>) stackToId;
    }

    @Unique
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static IntList stellar_core$getIds(final int hash) {
        final Int2ObjectOpenHashMap rawStacks = stellar_core$stacks();
        final Object value = rawStacks.get(hash);
        if (value == null || value instanceof IntList) {
            return (IntList) value;
        }

        final IntArrayList primitive = new IntArrayList((List<Integer>) value);
        rawStacks.put(hash, primitive);
        return primitive;
    }

    /**
     * @author Circulate233
     * @reason Look up ore ids without allocating an Integer.
     */
    @Overwrite
    public static int getOreID(final String name) {
        final Object2IntOpenHashMap<String> names = stellar_core$names();
        final int cached = names.getInt(name);
        if (cached != STELLAR_CORE$NO_ID) {
            return cached;
        }
        idToName.add(name);
        final int id = idToName.size() - 1;
        names.put(name, id);
        final NonNullList<ItemStack> back = NonNullList.create();
        idToStack.add(back);
        idToStackUn.add(back);
        return id;
    }

    /**
     * @author Circulate233
     * @reason Collect ids into a primitive set instead of boxing through Integer[].
     */
    @Overwrite
    public static int[] getOreIDs(@Nonnull final ItemStack stack) {
        if (stack.isEmpty()) {
            throw new IllegalArgumentException("Stack can not be invalid!");
        }

        final ResourceLocation registryName = stack.getItem().delegate.name();
        if (registryName == null) {
            FMLLog.log.debug("Attempted to find the oreIDs for an unregistered object ({}). This won't work very well.", stack);
            return new int[0];
        }

        final int id = Item.REGISTRY.getIDForObject(stack.getItem().delegate.get());
        final IntList byItem = stellar_core$getIds(id);
        final IntList byMeta = stellar_core$getIds(id | ((stack.getItemDamage() + 1) << 16));
        if (byItem == null || byItem.isEmpty()) {
            return byMeta == null || byMeta.isEmpty() ? new int[0] : byMeta.toIntArray();
        }
        if (byMeta == null || byMeta.isEmpty()) {
            return byItem.toIntArray();
        }

        final IntArraySet set = new IntArraySet(byItem.size() + byMeta.size());
        set.addAll(byItem);
        set.addAll(byMeta);
        return set.toIntArray();
    }

    /**
     * @author Circulate233
     * @reason Test registration without allocating an Integer.
     */
    @Overwrite
    public static boolean doesOreNameExist(final String name) {
        return stellar_core$names().getInt(name) != STELLAR_CORE$NO_ID;
    }

    /**
     * @author Circulate233
     * @reason Probe the primitive name map before creating a new ore entry.
     */
    @Overwrite
    public static NonNullList<ItemStack> getOres(final String name, final boolean alwaysCreateEntry) {
        if (alwaysCreateEntry) {
            return getOres(getOreID(name));
        }
        return stellar_core$names().getInt(name) != STELLAR_CORE$NO_ID
            ? getOres(getOreID(name))
            : OreDictionary.EMPTY_LIST;
    }

    /**
     * @author Circulate233
     * @reason Store baked ore ids in primitive lists from the first registration, avoiding the
     * startup allocation of ArrayList<Integer> and one Integer object per ore id.
     */
    @Overwrite
    private static void registerOreImpl(final String name, @Nonnull ItemStack ore) {
        if ("Unknown".equals(name)) {
            return;
        }
        if (ore.isEmpty()) {
            FMLLog.bigWarning("Invalid registration attempt for an Ore Dictionary item with name {} has occurred. The registration has been denied to prevent crashes. The mod responsible for the registration needs to correct this.", name);
            return;
        }

        final int oreID = getOreID(name);
        final ResourceLocation registryName = ore.getItem().delegate.name();
        int hash;
        if (registryName == null) {
            final ModContainer modContainer = Loader.instance().activeModContainer();
            final String modContainerName = modContainer == null ? null : modContainer.getName();
            FMLLog.bigWarning("A broken ore dictionary registration with name {} has occurred. It adds an item (type: {}) which is currently unknown to the game registry. This dictionary item can only support a single value when"
                + " registered with ores like this, and NO I am not going to turn this spam off. Just register your ore dictionary entries after the GameRegistry.\n"
                + "TO USERS: YES this is a BUG in the mod " + modContainerName + " report it to them!", name, ore.getItem().getClass());
            hash = STELLAR_CORE$NO_ID;
        } else {
            hash = Item.REGISTRY.getIDForObject(ore.getItem().delegate.get());
        }
        if (ore.getItemDamage() != OreDictionary.WILDCARD_VALUE) {
            hash |= ((ore.getItemDamage() + 1) << 16);
        }

        IntList ids = stellar_core$stacks().get(hash);
        if (ids != null && ids.contains(oreID)) {
            return;
        }
        if (ids == null) {
            ids = new IntArrayList();
            stellar_core$stacks().put(hash, ids);
        }
        ids.add(oreID);

        ore = ore.copy();
        idToStack.get(oreID).add(ore);
        MinecraftForge.EVENT_BUS.post(new OreDictionary.OreRegisterEvent(name, ore));
    }

    /**
     * @author Circulate233
     * @reason Rebake straight into primitive lists.
     */
    @Overwrite
    public static void rebakeMap() {
        final Int2ObjectOpenHashMap<IntList> stacks = stellar_core$stacks();
        stacks.clear();
        for (int id = 0; id < idToStack.size(); id++) {
            final NonNullList<ItemStack> ores = idToStack.get(id);
            if (ores == null) {
                continue;
            }
            for (final ItemStack ore : ores) {
                final ResourceLocation name = ore.getItem().delegate.name();
                int hash;
                if (name == null) {
                    FMLLog.log.debug("Defaulting unregistered ore dictionary entry for ore dictionary {}: type {} to -1", OreDictionary.getOreName(id), ore.getItem().getClass());
                    hash = STELLAR_CORE$NO_ID;
                } else {
                    hash = Item.REGISTRY.getIDForObject(ore.getItem().delegate.get());
                }
                if (ore.getItemDamage() != OreDictionary.WILDCARD_VALUE) {
                    hash |= ((ore.getItemDamage() + 1) << 16);
                }
                IntList ids = stacks.get(hash);
                if (ids == null) {
                    ids = new IntArrayList();
                    stacks.put(hash, ids);
                }
                ids.add(id);
            }
        }
    }

}
