package github.kasuminova.stellarcore.mixin.ctm;

import com.google.gson.JsonElement;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.shaded.org.jctools.maps.NonBlockingHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.common.model.IModelState;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import team.chisel.ctm.api.texture.ICTMTexture;
import team.chisel.ctm.client.model.ModelCTM;

import java.util.Map;
import java.util.function.Function;

@Mixin(value = ModelCTM.class, remap = false)
public class MixinModelCTM {

    @Shadow
    @Final
    private Int2ObjectMap<JsonElement> overrides;

    @Shadow
    protected Int2ObjectMap<TextureAtlasSprite> spriteOverrides;

    @Shadow
    protected Map<Pair<Integer, String>, ICTMTexture<?>> textureOverrides;

    @Shadow
    private Map<String, ICTMTexture<?>> textures;

    @Shadow
    private byte layers;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void injectInit(final ModelBlock modelinfo, final IModel vanillamodel, final Int2ObjectMap<JsonElement> overrides, final CallbackInfo ci) {
        if (!StellarCoreConfig.PERFORMANCE.vanilla.parallelModelLoader) {
            return;
        }
        this.spriteOverrides = Int2ObjectMaps.synchronize(new Int2ObjectOpenHashMap<>(this.overrides.size()));
        this.textureOverrides = new NonBlockingHashMap<>();
        this.textures = new NonBlockingHashMap<>();
    }

    @Inject(method = "bake", at = @At("RETURN"))
    private void injectBake(final IModelState state, final VertexFormat format, final Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter, final CallbackInfoReturnable<IBakedModel> cir) {
        if (!StellarCoreConfig.PERFORMANCE.vanilla.parallelModelLoader) {
            return;
        }
        byte collected = 0;
        for (final ICTMTexture<?> texture : this.textures.values()) {
            collected |= stellar_core$layerBit(texture);
        }
        for (final ICTMTexture<?> texture : this.textureOverrides.values()) {
            collected |= stellar_core$layerBit(texture);
        }
        this.layers |= collected;
    }

    @Unique
    private static byte stellar_core$layerBit(final ICTMTexture<?> texture) {
        final BlockRenderLayer layer = texture.getLayer();
        return (byte) (1 << (layer == null ? 7 : layer.ordinal()));
    }

}
