package github.kasuminova.stellarcore.mixin.ctm;

import com.google.gson.JsonElement;
import github.kasuminova.stellarcore.client.integration.ctm.CTMModelBakeLifecycle;
import github.kasuminova.stellarcore.shaded.org.jctools.util.Int2ObjectPair;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
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
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
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
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

@Mixin(value = ModelCTM.class, remap = false, priority = 999)
public abstract class MixinModelCTM implements IModelCTM, CTMModelBakeLifecycle.Model {

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
    @Mutable
    private Collection<ResourceLocation> textureDependencies;

    @Shadow
    @Final
    protected Int2ObjectMap<IMetadataSectionCTM> metaOverrides;

    @Shadow
    @Final
    private ModelBlock modelinfo;

    @Shadow
    private IModel vanillamodel;

    @Redirect(method = "<init>", at = @At(value = "FIELD",
        target = "Lteam/chisel/ctm/client/model/ModelCTM;textureDependencies:Ljava/util/Collection;", opcode = Opcodes.PUTFIELD), require = 1)
    private void stellar_core$flatTextureDependencies(final ModelCTM model, final Collection<ResourceLocation> empty) {
        this.textureDependencies = new ObjectOpenHashSet<>(0);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void injectInit(final ModelBlock modelinfo, final IModel vanillamodel, final Int2ObjectMap<JsonElement> overrides, final CallbackInfo ci) {
        this.textures = Object2ObjectMaps.emptyMap();
    }

    /**
     * @author circulation
     * @reason 重写方法适应并发
     */
    @SuppressWarnings("DataFlowIssue")
    @Overwrite
    @Nonnull
    public IBakedModel bake(@Nonnull IModelState state, @Nonnull VertexFormat format, @Nonnull Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
        stellar_core$prepareTextures();
        final Map<String, ICTMTexture<?>> textures = this.textures;
        IBakedModel parent = this.vanillamodel.bake(state, format, (rl) -> {
            TextureAtlasSprite sprite = bakedTextureGetter.apply(rl);
            final String name = sprite.getIconName();
            if (textures.get(name) == null) {
                textures.computeIfAbsent(name, ignored -> {
                    IMetadataSectionCTM metadata = null;
                    try {
                        metadata = ResourceUtil.getMetadata(sprite);
                    } catch (IOException ignoredException) {
                    }
                    final ICTMTexture<?> texture = metadata == null
                        ? new TextureNormal(TextureTypeNormal.INSTANCE, new TextureInfo(new TextureAtlasSprite[]{sprite}, Optional.empty(), null))
                        : metadata.makeTexture(sprite, bakedTextureGetter);
                    stellar_core$addLayer(texture);
                    return texture;
                });
            }
            return sprite;
        });

        if (CTMModelBakeLifecycle.isLoading() || this.textureOverrides == null) {
            stellar_core$initOverrides(bakedTextureGetter);
        }
        return new ModelBakedCTM(this, parent);
    }

    @Unique
    private synchronized void stellar_core$initOverrides(final Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
        if (this.spriteOverrides == null) {
            final Int2ObjectMap<TextureAtlasSprite> sprites = new Int2ObjectArrayMap<>(this.overrides.size());
            for (var e : this.overrides.int2ObjectEntrySet()) {
                if (e.getValue().isJsonPrimitive() && e.getValue().getAsJsonPrimitive().isString()) {
                    TextureAtlasSprite sprite = bakedTextureGetter.apply(new ResourceLocation(e.getValue().getAsString()));
                    sprites.put(e.getIntKey(), sprite);
                } else if (e.getValue().isJsonObject()) {
                    JsonElement texture = e.getValue().getAsJsonObject().get("texture");
                    if (texture != null && texture.isJsonPrimitive()) {
                        sprites.put(e.getIntKey(), bakedTextureGetter.apply(new ResourceLocation(texture.getAsString())));
                    }
                }
            }
            this.spriteOverrides = sprites;
        }

        if (this.textureOverrides == null) {
            if (this.metaOverrides.isEmpty()) {
                this.textureOverrides = ((Object) this).getClass() == ModelCTM.class
                    ? Object2ObjectMaps.emptyMap() : new Object2ObjectOpenHashMap<>(0);
                return;
            }
            final Map<Int2ObjectPair<String>, ICTMTexture<?>> textures = new ConcurrentHashMap<>(2);
            var elements = this.modelinfo.getElements();
            this.metaOverrides.int2ObjectEntrySet().parallelStream().forEach(ctmEntry -> {
                int targetTint = ctmEntry.getIntKey();
                IMetadataSectionCTM ctm = ctmEntry.getValue();
                TextureAtlasSprite overrideSprite = this.getOverrideSprite(targetTint);
                Set<String> seen = new ObjectOpenHashSet<>();

                for (BlockPart element : elements) {
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
                        textures.put(new Int2ObjectPair<>(targetTint, texLoc.toString()), tex);
                    }
                }
            });
            // The local stream has joined; no worker mutates this lookup table afterward.
            this.textureOverrides = textures.isEmpty() && ((Object) this).getClass() == ModelCTM.class
                ? Object2ObjectMaps.emptyMap() : new Object2ObjectOpenHashMap<>(textures);
            for (final ICTMTexture<?> texture : textures.values()) {
                this.layers |= stellar_core$layerBit(texture);
            }
        }
    }

    @Unique
    private void stellar_core$prepareTextures() {
        if (CTMModelBakeLifecycle.isLoading()) {
            if (!(this.textures instanceof ConcurrentMap)) {
                synchronized (this) {
                    if (!(this.textures instanceof ConcurrentMap)) {
                        final Map<String, ICTMTexture<?>> concurrent = new ConcurrentHashMap<>(Math.max(2, this.textures.size()));
                        concurrent.putAll(this.textures);
                        this.textures = concurrent;
                    }
                }
            }
            CTMModelBakeLifecycle.track(this);
        } else if (this.textures == Object2ObjectMaps.EMPTY_MAP) {
            this.textures = new Object2ObjectOpenHashMap<>(2);
        }
    }

    @Override
    public void stellar_core$compactTextureMaps() {
        if (this.textures instanceof ConcurrentMap) {
            this.textures = this.textures.isEmpty() ? Object2ObjectMaps.emptyMap() : new Object2ObjectOpenHashMap<>(this.textures);
        }
    }

    @Unique
    private void stellar_core$addLayer(final ICTMTexture<?> texture) {
        final byte bit = stellar_core$layerBit(texture);
        if (CTMModelBakeLifecycle.isLoading()) {
            synchronized (this) {
                this.layers |= bit;
            }
        } else {
            this.layers |= bit;
        }
    }

    /**
     * @author circulation
     * @reason 使用原始类型的pair
     */
    @Overwrite
    @Nullable
    public ICTMTexture<?> getOverrideTexture(int tintIndex, String sprite) {
        if (this.textureOverrides.isEmpty()) return null;
        return this.textureOverrides.get(new Int2ObjectPair<>(tintIndex, sprite));
    }

    @Unique
    private static byte stellar_core$layerBit(final ICTMTexture<?> texture) {
        final BlockRenderLayer layer = texture.getLayer();
        return (byte) (1 << (layer == null ? 7 : layer.ordinal()));
    }

}
