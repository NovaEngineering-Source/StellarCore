package github.kasuminova.stellarcore.mixin.minecraft.forge.parallelmodelloader;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import github.kasuminova.stellarcore.StellarCore;
import net.minecraft.client.renderer.block.model.ModelBlockDefinition;
import net.minecraft.client.renderer.block.model.ModelRotation;
import net.minecraft.client.renderer.block.model.Variant;
import net.minecraft.client.renderer.block.model.VariantList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.BlockStateLoader;
import net.minecraftforge.client.model.ForgeBlockStateV1;
import net.minecraftforge.common.model.TRSRTransformation;
import org.apache.commons.io.IOUtils;
import org.spongepowered.asm.mixin.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"StaticVariableUsedBeforeInitialization", "StaticVariableMayNotBeInitialized", "NonConstantFieldWithUpperCaseName", "FieldAccessedSynchronizedAndUnsynchronized"})
@Mixin(value = BlockStateLoader.class, remap = false)
public class MixinBlockStateLoader {
//
//    @Shadow
//    @Final
//    private static Gson GSON;
//
//    @Unique
//    private static Constructor<?> stellar_core$constructor = null;
//
//    /**
//     * @author Kasumi_Nova
//     * @reason Thread safe.
//     */
//    @Overwrite
//    public static ModelBlockDefinition load(Reader reader, ResourceLocation location, final Gson vanillaGSON) {
//        if (stellar_core$constructor == null) {
//            stellar_core$initConstructor();
//        }
//
//        synchronized (ModelBlockDefinition.class) {
//            try {
//                byte[] data = IOUtils.toByteArray(reader, StandardCharsets.UTF_8);
//                reader = new InputStreamReader(new ByteArrayInputStream(data), StandardCharsets.UTF_8);
//
//                BlockStateLoader.Marker marker = GSON.fromJson(new String(data, StandardCharsets.UTF_8), BlockStateLoader.Marker.class); // Read "forge_marker" to determine what to load.
//
//                // Unknown version.. try loading it as normal.
//                if (marker.forge_marker != 1) {
//                    return vanillaGSON.fromJson(reader, ModelBlockDefinition.class);
//                }
//
//                // Version 1
//                ForgeBlockStateV1 v1 = GSON.fromJson(reader, ForgeBlockStateV1.class);
//
//                Map<String, VariantList> variants = Maps.newLinkedHashMap();
//                AccessorForgeBlockStateV1 accessorV1 = (AccessorForgeBlockStateV1) v1;
//
//                for (Map.Entry<String, Collection<ForgeBlockStateV1.Variant>> entry : accessorV1.getVariants().asMap().entrySet()) { // Convert Version1 variants into vanilla variants for the ModelBlockDefinition.
//                    List<Variant> mcVars = Lists.newArrayList();
//                    for (ForgeBlockStateV1.Variant var : entry.getValue()) {
//                        boolean uvLock = var.getUvLock().orElse(false);
//                        int weight = var.getWeight().orElse(1);
//
//                        if (((AccessorForgeBlockStateV1Variant) var).invokeIsVanillaCompatible()) {
//                            mcVars.add(new Variant(var.getModel(), (ModelRotation) var.getState().orElse(ModelRotation.X0_Y0), uvLock, weight));
//                            continue;
//                        }
//
//                        Variant variant = stellar_core$createForgeVariant(location, var, uvLock, weight);
//                        mcVars.add(variant);
//                    }
//                    variants.put(entry.getKey(), new VariantList(mcVars));
//                }
//
//                return new ModelBlockDefinition(variants, null);
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        }
//    }
//
//    @Unique
//    private static Variant stellar_core$createForgeVariant(final ResourceLocation location, final ForgeBlockStateV1.Variant var, final boolean uvLock, final int weight) {
//        try {
//            return (Variant) stellar_core$constructor.newInstance(
//                    location, var.getModel(), var.getState().orElse(TRSRTransformation.identity()),
//                    uvLock, var.getSmooth(), var.getGui3d(), weight,
//                    var.getTextures(), var.getOnlyPartsVariant(), var.getCustomData()
//            );
//        } catch (Throwable e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    @Unique
//    private static synchronized void stellar_core$initConstructor() {
//        if (stellar_core$constructor != null) {
//            return;
//        }
//        try {
//            stellar_core$constructor = Class.forName("net.minecraftforge.client.model.BlockStateLoader$ForgeVariant").getDeclaredConstructors()[0];
//            stellar_core$constructor.setAccessible(true);
//        } catch (ClassNotFoundException e) {
//            throw new RuntimeException(e);
//        }
//    }

}
