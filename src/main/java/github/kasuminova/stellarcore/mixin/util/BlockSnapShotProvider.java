package github.kasuminova.stellarcore.mixin.util;

import net.minecraftforge.common.util.BlockSnapshot;

import java.util.LinkedList;

public interface BlockSnapShotProvider {

    LinkedList<BlockSnapshot> getCapturedBlockSnapshots();

}
