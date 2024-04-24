package github.kasuminova.stellarcore.common.bugfix;

import github.kasuminova.stellarcore.StellarCore;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.util.ContainerTECache;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.List;

@SuppressWarnings("MethodMayBeStatic")
public class TileEntityContainerFixes {

    public static final TileEntityContainerFixes INSTANCE = new TileEntityContainerFixes();

    @SubscribeEvent
    @SuppressWarnings("ConstantValue")
    public void onPlayerTick(final TickEvent.PlayerTickEvent event) {
        if (!StellarCoreConfig.BUG_FIXES.container.containerTileEntityFixes) {
            return;
        }
        if (event.side != Side.SERVER) {
            return;
        }
        if (event.phase != TickEvent.Phase.START) {
            return;
        }

        EntityPlayer player = event.player;
        Container current = player.openContainer;
        if (current == null) {
            return;
        }

        List<TileEntity> teList = ContainerTECache.getTileEntityList(current);
        for (final TileEntity te : teList) {
            BlockPos pos = te.getPos();
            World world = te.getWorld();
            if (te.isInvalid() || world == null || !world.isBlockLoaded(pos) || world.getTileEntity(pos) != te) {
                player.closeScreen();
                StellarCore.log.warn(String.format(
                        "Detected invalid TileEntity GUI, World: %s, Pos: %s, GUI Class: %s, TileEntity Class: %s",
                        world == null ? "null" : world.getWorldInfo().getWorldName(),
                        posToString(pos),
                        current.getClass().getName(),
                        te.getClass().getName()
                ));
                break;
            }
        }
    }

    public static String posToString(Vec3i pos) {
        return String.format("X:%s Y:%s Z:%s", pos.getX(), pos.getY(), pos.getZ());
    }

}
