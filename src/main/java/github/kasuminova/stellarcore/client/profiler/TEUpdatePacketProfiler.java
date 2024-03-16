package github.kasuminova.stellarcore.client.profiler;

import hellfirepvp.modularmachinery.common.util.MiscUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TEUpdatePacketProfiler {
    public static final ConcurrentHashMap<Class<?>, Tuple<Long, Long>> TE_UPDATE_PACKET_TOTAL_SIZE = new ConcurrentHashMap<>();

    public static void onPacketReceived(SPacketUpdateTileEntity packet, int packetSize) {
        WorldClient world = Minecraft.getMinecraft().world;
        BlockPos pos = packet.getPos();
        if (world.isBlockLoaded(pos)) {
            TileEntity te = world.getTileEntity(pos);
            if (te == null) {
                return;
            }

            TE_UPDATE_PACKET_TOTAL_SIZE.compute(te.getClass(), (key, value) -> value == null
                    ? new Tuple<>(1L, (long) packetSize)
                    : new Tuple<>(value.getFirst() + 1, value.getSecond() + packetSize));
        }
    }

    public static List<String> getProfilerMessages(final int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }

        List<String> messages = new LinkedList<>();

        @SuppressWarnings("SimplifyStreamApiCallChains")
        List<Map.Entry<Class<?>, Tuple<Long, Long>>> teSorted = TEUpdatePacketProfiler.TE_UPDATE_PACKET_TOTAL_SIZE.entrySet().stream()
                .sorted((o1, o2) -> Long.compare(o2.getValue().getSecond(), o1.getValue().getSecond()))
                .limit(limit)
                .collect(Collectors.toList());

        messages.add("SPacketUpdateTileEntity stat:");
        for (final Map.Entry<Class<?>, Tuple<Long, Long>> entry : teSorted) {
            Class<?> tClass = entry.getKey();
            long packetTotalAmount = entry.getValue().getFirst();
            long packetTotalSize = entry.getValue().getSecond();

            messages.add(
                    String.format("TE Class: %s",
                            TextFormatting.BLUE + tClass.getName() + TextFormatting.WHITE)
            );
            messages.add(
                    String.format("Amt: %s, Size Total: %s, Size Avg: %s",
                            TextFormatting.GOLD + MiscUtils.formatDecimal(packetTotalAmount) + TextFormatting.WHITE,
                            TextFormatting.RED + MiscUtils.formatNumber(packetTotalSize) + 'B' + TextFormatting.WHITE,
                            TextFormatting.YELLOW + MiscUtils.formatNumber(packetTotalSize / packetTotalAmount)) + 'B' + TextFormatting.WHITE
            );
        }

        return messages;
    }

}
