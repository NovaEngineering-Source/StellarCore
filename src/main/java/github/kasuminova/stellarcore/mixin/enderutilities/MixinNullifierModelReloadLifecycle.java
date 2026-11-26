package github.kasuminova.stellarcore.mixin.enderutilities;

import fi.dy.masa.enderutilities.client.renderer.model.ModelNullifierBaked;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.registries.IRegistryDelegate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

/**
 * The replacement compositor uses the runtime mesher; legacy global model state must stay empty.
 */
@Mixin(value = ModelNullifierBaked.class, remap = false)
public abstract class MixinNullifierModelReloadLifecycle {
    @Shadow
    private static ModelLoader MODEL_LOADER;
    @Shadow
    private static Map<ModelResourceLocation, IModel> STATE_MODELS;
    @Shadow
    private static Map<IRegistryDelegate<Item>, Int2ObjectMap<ModelResourceLocation>> LOCATIONS;
    @Shadow
    private static Map<Item, ItemMeshDefinition> SHAPERS;
    @Shadow
    @Final
    private static Map<?, ?> NULLIFIER_MODEL_CACHE;
    @Shadow
    @Final
    private static Map<?, ?> ITEM_MODEL_CACHE;

    /** @author StellarCore @reason Keep only the runtime compositor across model generations. */
    @SubscribeEvent
    @Overwrite
    public static void onModelbakeEvent(final ModelBakeEvent event) {
        MODEL_LOADER = null;
        STATE_MODELS = null;
        LOCATIONS = null;
        SHAPERS = null;
        NULLIFIER_MODEL_CACHE.clear();
        ITEM_MODEL_CACHE.clear();
    }
}
