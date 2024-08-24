package github.kasuminova.stellarcore.mixin.util;

import net.minecraft.client.renderer.texture.Stitcher;

import java.util.List;

public interface AccessorStitcherSlot {

    int width();

    int height();

    List<Stitcher.Slot> subSlots();

    Stitcher.Holder holder();

    void setHolder(Stitcher.Holder holder);

    void setSubSlots(List<Stitcher.Slot> subSlots);

}
