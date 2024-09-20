package github.kasuminova.stellarcore.mixin.minecraft.worldserver;

import github.kasuminova.stellarcore.common.util.LinkedFakeArrayList;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(WorldServer.class)
public class MixinWorldServer {

    @Redirect(
            method = "getPendingBlockUpdates(Lnet/minecraft/world/gen/structure/StructureBoundingBox;Z)Ljava/util/List;",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/google/common/collect/Lists;newArrayList()Ljava/util/ArrayList;",
                    remap = false
            )
    )
    private ArrayList<NextTickListEntry> injectGetPendingBlockUpdates() {
        return new LinkedFakeArrayList<>();
    }

}
