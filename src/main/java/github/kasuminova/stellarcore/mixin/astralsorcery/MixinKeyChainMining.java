package github.kasuminova.stellarcore.mixin.astralsorcery;

import hellfirepvp.astralsorcery.common.constellation.perk.tree.nodes.key.KeyChainMining;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(KeyChainMining.class)
public class MixinKeyChainMining {

    /**
     * @author Kasumi_Nova
     * @reason 配置文件不让禁用，那就把方法爆了。
     */
    @Overwrite(remap = false)
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onBreak(BlockEvent.BreakEvent event) {
    }

}
