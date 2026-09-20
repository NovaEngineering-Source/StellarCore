package github.kasuminova.stellarcore.mixin.ctm;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.shaded.org.jctools.maps.NonBlockingHashMap;
import github.kasuminova.stellarcore.shaded.org.jctools.util.Int2ObjectPair;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.renderer.block.model.BlockPart;
import net.minecraft.client.renderer.block.model.BlockPartFace;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.common.model.IModelState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import team.chisel.ctm.api.model.IModelCTM;
import team.chisel.ctm.api.texture.ICTMTexture;
import team.chisel.ctm.api.util.TextureInfo;
import team.chisel.ctm.client.model.ModelBakedCTM;
import team.chisel.ctm.client.model.ModelCTM;
import team.chisel.ctm.client.texture.IMetadataSectionCTM;
import team.chisel.ctm.client.texture.render.TextureNormal;
import team.chisel.ctm.client.texture.type.TextureTypeNormal;
import team.chisel.ctm.client.util.ResourceUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

@Mixin(value = ModelCTM.class, remap = false, priority = 999)
public abstract class MixinModelCTM implements IModelCTM {

    @Shadow
    @Final
    private Int2ObjectMap<JsonElement> overrides;

    @Shadow
    protected Int2ObjectMap<TextureAtlasSprite> spriteOverrides;

    @Shadow
    protected Map<Int2ObjectPair<String>, ICTMTexture<?>> textureOverrides;

    @Shadow
    private Map<String, ICTMTexture<?>> textures;

    @Shadow
    private byte layers;

    @Shadow
    @Final
    protected Int2ObjectMap<IMetadataSectionCTM> metaOverrides;

    @Shadow
    @Final
    private ModelBlock modelinfo;

    @Shadow
    private IModel vanillamodel;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void injectInit(final ModelBlock modelinfo, final IModel vanillamodel, final Int2ObjectMap<JsonElement> overrides, final CallbackInfo ci) {
        if (!StellarCoreConfig.PERFORMANCE.vanilla.parallelModelLoader) {
            return;
        }
        this.textures = new NonBlockingHashMap<>();
    }

    /**
     * @author circulation
     * @reason 重写方法适应并发
     */
    @SuppressWarnings("DataFlowIssue")
    @Overwrite
    @Nonnull
    public IBakedModel bake(@Nonnull IModelState state, @Nonnull VertexFormat format, @Nonnull Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
        IBakedModel parent = this.vanillamodel.bake(state, format, (rl) -> {
            TextureAtlasSprite sprite = bakedTextureGetter.apply(rl);
            IMetadataSectionCTM chiselmeta = null;

            try {
                chiselmeta = ResourceUtil.getMetadata(sprite);
            } catch (IOException ignored) {
            }

            IMetadataSectionCTM finalChiselmeta = chiselmeta;
            this.textures.computeIfAbsent(sprite.getIconName(), (s) -> {
                ICTMTexture<?> tex;
                if (finalChiselmeta == null) {
                    tex = new TextureNormal(TextureTypeNormal.INSTANCE, new TextureInfo(new TextureAtlasSprite[]{sprite}, Optional.empty(), null));
                } else {
                    tex = finalChiselmeta.makeTexture(sprite, bakedTextureGetter);
                }

                this.layers = (byte) (this.layers | 1 << (tex.getLayer() == null ? 7 : tex.getLayer().ordinal()));
                return tex;
            });
            return sprite;
        });

        if (this.spriteOverrides == null) {
            this.spriteOverrides = new Int2ObjectArrayMap<>();

            for (var e : this.overrides.int2ObjectEntrySet()) {
                if (e.getValue().isJsonPrimitive() && e.getValue().getAsJsonPrimitive().isString()) {
                    TextureAtlasSprite sprite = bakedTextureGetter.apply(new ResourceLocation(e.getValue().getAsString()));
                    this.spriteOverrides.put(e.getIntKey(), sprite);
                } else if (e.getValue().isJsonObject()) {
                    JsonElement texture = e.getValue().getAsJsonObject().get("texture");
                    if (texture != null && texture.isJsonPrimitive()) {
                        this.spriteOverrides.put(e.getIntKey(), bakedTextureGetter.apply(new ResourceLocation(texture.getAsString())));
                    }
                }
            }
        }

        if (this.textureOverrides == null) {
            this.textureOverrides = new NonBlockingHashMap<>();
            if (!metaOverrides.isEmpty()) {
                var l = ImmutableList.copyOf(this.modelinfo.getElements());

                this.metaOverrides.int2ObjectEntrySet().parallelStream().forEach(ctmEntry -> {
                    int targetTint = ctmEntry.getIntKey();
                    IMetadataSectionCTM ctm = ctmEntry.getValue();
                    TextureAtlasSprite overrideSprite = this.getOverrideSprite(targetTint);
                    Set<String> seen = new ObjectOpenHashSet<>();

                    for (BlockPart element : l) {
                        for (BlockPartFace part : element.mapFaces.values()) {
                            if (part.tintIndex != targetTint) continue;

                            String texture = part.texture;
                            String spriteKey = this.modelinfo.textures.getOrDefault(texture.substring(1), texture);

                            if (!seen.add(spriteKey)) continue;

                            ResourceLocation texLoc = new ResourceLocation(spriteKey);
                            TextureAtlasSprite sprite = overrideSprite != null
                                ? overrideSprite
                                : bakedTextureGetter.apply(texLoc);

                            ICTMTexture<?> tex = ctm.makeTexture(sprite, bakedTextureGetter);
                            this.layers |= (byte) (1 << (tex.getLayer() == null ? 7 : tex.getLayer().ordinal()));
                            this.textureOverrides.put(new Int2ObjectPair<>(targetTint, texLoc.toString()), tex);
                        }
                    }
                });
            }
        }

        if (StellarCoreConfig.PERFORMANCE.vanilla.parallelModelLoader) {
            byte collected = 0;
            for (final ICTMTexture<?> texture : this.textures.values()) {
                collected |= stellar_core$layerBit(texture);
            }
            for (final ICTMTexture<?> texture : this.textureOverrides.values()) {
                collected |= stellar_core$layerBit(texture);
            }
            this.layers |= collected;
        }

        return new ModelBakedCTM(this, parent);
    }

    /**
     * @author circulation
     * @reason 使用原始类型的pair
     */
    @Overwrite
    @Nullable
    public ICTMTexture<?> getOverrideTexture(int tintIndex, String sprite) {
        return this.textureOverrides.get(new Int2ObjectPair<>(tintIndex, sprite));
    }

    @Unique
    private static byte stellar_core$layerBit(final ICTMTexture<?> texture) {
        final BlockRenderLayer layer = texture.getLayer();
        return (byte) (1 << (layer == null ? 7 : layer.ordinal()));
    }

}
