package github.kasuminova.stellarcore.mixin.minecraft.stitcher;

import github.kasuminova.stellarcore.mixin.util.AccessorStitcherSlot;
import net.minecraft.client.renderer.texture.Stitcher;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@SuppressWarnings("AddedMixinMembersNamePattern")
@Mixin(Stitcher.Slot.class)
public class MixinStitcherSlot implements AccessorStitcherSlot {

    @Final
    @Shadow
    private int width;

    @Final
    @Shadow
    private int height;

    @Shadow
    private List<Stitcher.Slot> subSlots;

    @Shadow
    private Stitcher.Holder holder;

    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return height;
    }

    @Override
    public List<Stitcher.Slot> subSlots() {
        return subSlots;
    }

    @Override
    public Stitcher.Holder holder() {
        return holder;
    }

    @Override
    public void setHolder(final Stitcher.Holder holder) {
        this.holder = holder;
    }

    @Override
    public void setSubSlots(final List<Stitcher.Slot> subSlots) {
        this.subSlots = subSlots;
    }

}
