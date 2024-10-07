package github.kasuminova.stellarcore.mixin.minecraft.forge.registry;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.util.StellarLog;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(value = ForgeRegistry.class, remap = false)
public class MixinForgeRegistry {

    @Unique
    private static final Set<ResourceLocation> stellar_core$BLACK_LIST = new ObjectOpenHashSet<>();

    @Unique
    private static boolean stellar_core$blackListInitialized = false;

    @SuppressWarnings("MethodMayBeStatic")
    @Inject(method = "register", at = @At("HEAD"), cancellable = true)
    private void injectRegister(final IForgeRegistryEntry<?> value, final CallbackInfo ci) {
        if (stellar_core$isInBlackList(value.getRegistryName())) {
            StellarLog.LOG.warn("[StellarCore] Removed forge registry object {}.", value.getRegistryName());
            ci.cancel();
        }
    }

    @Unique
    private static boolean stellar_core$isInBlackList(final ResourceLocation key) {
        if (!stellar_core$blackListInitialized) {
            String[] removeList = StellarCoreConfig.FEATURES.vanilla.forgeRegistryRemoveList;
            for (String s : removeList) {
                stellar_core$BLACK_LIST.add(new ResourceLocation(s));
            }
            stellar_core$blackListInitialized = true;
        }
        return !stellar_core$BLACK_LIST.isEmpty() && stellar_core$BLACK_LIST.contains(key);
    }

}
