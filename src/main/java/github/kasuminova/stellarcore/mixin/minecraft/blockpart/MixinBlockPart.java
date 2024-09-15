package github.kasuminova.stellarcore.mixin.minecraft.blockpart;

import net.minecraft.client.renderer.block.model.BlockPart;
import net.minecraft.client.renderer.block.model.BlockPartFace;
import net.minecraft.client.renderer.block.model.BlockPartRotation;
import net.minecraft.util.EnumFacing;
import org.lwjgl.util.vector.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.EnumMap;
import java.util.Map;

@Mixin(BlockPart.class)
public class MixinBlockPart {

    @Final
    @Shadow
    @Mutable
    public Map<EnumFacing, BlockPartFace> mapFaces;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void injectInit(final Vector3f positionFromIn, final Vector3f positionToIn, final Map mapFacesIn, final BlockPartRotation partRotationIn, final boolean shadeIn, final CallbackInfo ci) {
        if (mapFaces != null && !(mapFaces instanceof EnumMap)) {
            mapFaces = new EnumMap<>(mapFaces);
        }
    }

}
