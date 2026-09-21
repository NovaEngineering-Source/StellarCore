package github.kasuminova.stellarcore.mixin.tconstruct;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.shaded.org.jctools.maps.NonBlockingHashMap;
import github.kasuminova.stellarcore.shaded.org.jctools.maps.NonBlockingHashSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import slimeknights.tconstruct.library.TinkerRegistry;
import slimeknights.tconstruct.library.client.CustomTextureCreator;
import slimeknights.tconstruct.library.client.model.MaterialModelLoader;
import slimeknights.tconstruct.library.client.texture.TinkerTexture;
import slimeknights.tconstruct.library.materials.Material;
import slimeknights.tconstruct.library.materials.MaterialGUI;
import slimeknights.tconstruct.library.tools.IToolPart;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Mixin(value = CustomTextureCreator.class, remap = false)
public class MixinCustomTextureCreator {

    @Shadow
    public static Map<String, Map<String, TextureAtlasSprite>> sprites;

    @Shadow
    private static Set<ResourceLocation> baseTextures;

    @Shadow
    private static Map<ResourceLocation, Set<IToolPart>> texturePartMapping;

    @Shadow
    @Final
    public static Material guiMaterial;

    @Shadow
    private int createdTextures;

    @Inject(method = "<clinit>", at = @At("RETURN"), remap = false)
    private static void injectCLInit(final CallbackInfo ci) {
        if (!stellar_core$concurrentCollections()) {
            return;
        }
        sprites = new NonBlockingHashMap<>();
        baseTextures = new NonBlockingHashSet<>();
        texturePartMapping = new NonBlockingHashMap<>();
    }

    @Inject(method = "registerTextureForPart", at = @At("HEAD"), cancellable = true, remap = false)
    private static void stellar_core$registerTextureForPart(final ResourceLocation texture, final IToolPart toolPart, final CallbackInfo ci) {
        if (!stellar_core$concurrentCollections()) {
            return;
        }
        ci.cancel();

        texturePartMapping.computeIfAbsent(texture, key -> new NonBlockingHashSet<>()).add(toolPart);
        CustomTextureCreator.registerTexture(texture);
    }

    @Inject(method = "createMaterialTextures", at = @At("HEAD"), cancellable = true, remap = false)
    private void stellar_core$parallelCreateMaterialTextures(final TextureMap map, final CallbackInfo ci) {
        if (!StellarCoreConfig.PERFORMANCE.tConstruct.parallelMaterialTextureGen
                || !StellarCoreConfig.PERFORMANCE.vanilla.resourceExistStateCache) {
            return;
        }
        ci.cancel();

        final ObjectArrayList<ResourceLocation> targets = new ObjectArrayList<>(baseTextures.size());
        for (final ResourceLocation baseTexture : baseTextures) {
            if (!baseTexture.toString().equals("minecraft:missingno")) {
                targets.add(baseTexture);
            }
        }

        final ObjectArrayList<Material> materials = new ObjectArrayList<>(TinkerRegistry.getAllMaterials());
        final Set<String> toolPartBaseTextures = stellar_core$collectToolPartBaseTextures();
        final AtomicInteger created = new AtomicInteger();

        targets.parallelStream().forEach(baseTexture -> {
            final Set<IToolPart> parts = texturePartMapping.get(baseTexture);
            final Map<String, TextureAtlasSprite> builtSprites = new Object2ObjectOpenHashMap<>();

            for (int i = 0; i < materials.size(); i++) {
                final Material material = materials.get(i);
                if (!stellar_core$usable(parts, material)) {
                    continue;
                }
                final TextureAtlasSprite sprite = stellar_core$createTexture(material, baseTexture, map, created);
                if (sprite != null) {
                    builtSprites.put(material.identifier, sprite);
                }
            }

            if (toolPartBaseTextures.contains(baseTexture.toString())) {
                final TextureAtlasSprite sprite = stellar_core$createTexture(guiMaterial, baseTexture, map, created);
                if (sprite != null) {
                    builtSprites.put(guiMaterial.identifier, sprite);
                }
            }

            sprites.put(baseTexture.toString(), builtSprites);
        });

        this.createdTextures += created.get();
    }

    @Unique
    private static boolean stellar_core$concurrentCollections() {
        return StellarCoreConfig.PERFORMANCE.vanilla.parallelModelLoader
                || StellarCoreConfig.PERFORMANCE.tConstruct.parallelMaterialTextureGen;
    }

    @Unique
    private static boolean stellar_core$usable(final Set<IToolPart> parts, final Material material) {
        if (parts == null || material instanceof MaterialGUI) {
            return true;
        }
        for (final IToolPart toolPart : parts) {
            if (toolPart.canUseMaterialForRendering(material)) {
                return true;
            }
        }
        return false;
    }

    @Unique
    private static TextureAtlasSprite stellar_core$createTexture(final Material material, ResourceLocation baseTexture, final TextureMap map, final AtomicInteger created) {
        final String location = baseTexture.toString() + "_" + material.identifier;
        TextureAtlasSprite sprite;

        if (CustomTextureCreator.exists(location)) {
            sprite = map.registerSprite(new ResourceLocation(location));
        } else {
            if (material.renderInfo == null) {
                return null;
            }
            if (material.renderInfo.getTextureSuffix() != null) {
                final String loc2 = baseTexture + "_" + material.renderInfo.getTextureSuffix();
                TextureAtlasSprite base2 = map.getTextureExtry(loc2);
                if (base2 == null && CustomTextureCreator.exists(loc2)) {
                    base2 = TinkerTexture.loadManually(new ResourceLocation(loc2));
                    map.setTextureEntry(base2);
                }
                if (base2 != null) {
                    baseTexture = new ResourceLocation(base2.getIconName());
                }
            }
            sprite = material.renderInfo.getTexture(baseTexture, location);
            created.incrementAndGet();
        }

        if (sprite != null && material.renderInfo.isStitched()) {
            map.setTextureEntry(sprite);
        }
        return sprite;
    }

    @Unique
    private static Set<String> stellar_core$collectToolPartBaseTextures() {
        final ObjectOpenHashSet<String> collected = new ObjectOpenHashSet<>();
        for (final IToolPart toolPart : TinkerRegistry.getToolParts()) {
            if (!(toolPart instanceof Item)) {
                continue;
            }
            try {
                final Optional<ResourceLocation> stored = MaterialModelLoader.getToolPartModelLocation(toolPart);
                if (!stored.isPresent()) {
                    continue;
                }
                final ResourceLocation storedLocation = stored.get();
                final IModel partModel = ModelLoaderRegistry.getModel(
                        new ResourceLocation(storedLocation.getNamespace(), "item/" + storedLocation.getPath())
                );
                collected.add(partModel.getTextures().iterator().next().toString());
            } catch (Exception e) {
                break;
            }
        }
        return collected;
    }

}
