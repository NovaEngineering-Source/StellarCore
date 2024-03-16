package github.kasuminova.stellarcore.client.profiler;

import hellfirepvp.modularmachinery.common.util.MiscUtils;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.Tuple;
import net.minecraft.util.text.TextFormatting;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class PacketProfiler {
    public static final ConcurrentHashMap<Class<?>, Tuple<Long, Long>> PACKET_TOTAL_SIZE = new ConcurrentHashMap<>();
    public static final AtomicLong TOTAL_RECEIVED_DATA_SIZE = new AtomicLong(0);

    public static boolean enabled = false;

    public static long profilerStartTime = 0;
    public static long profilerStopTime = 0;

    public static void onPacketReceived(Object packet, int packetSize) {
        if (!enabled) {
            return;
        }

        if (PACKET_TOTAL_SIZE.isEmpty()) {
            profilerStartTime = System.currentTimeMillis();
        }

        PACKET_TOTAL_SIZE.compute(packet.getClass(), (key, value) -> value == null
                ? new Tuple<>(1L, (long) packetSize)
                : new Tuple<>(value.getFirst() + 1, value.getSecond() + packetSize));

        if (packet instanceof SPacketUpdateTileEntity) {
            TEUpdatePacketProfiler.onPacketReceived((SPacketUpdateTileEntity) packet, packetSize);
        }
    }

    public static void onPacketDecoded(final int length) {
        if (!enabled) {
            return;
        }

        TOTAL_RECEIVED_DATA_SIZE.addAndGet(length);
    }

    public static List<String> getProfilerMessages(final int limit) {
        List<String> messages = new LinkedList<>();

        long totalPacketSize = TOTAL_RECEIVED_DATA_SIZE.get();

        long profileTimeExisted = enabled ? System.currentTimeMillis() - profilerStartTime : profilerStopTime - profilerStartTime;
        double networkBandwidthPerSec = profileTimeExisted <= 0 ? 0 : (totalPacketSize / ((double) profileTimeExisted / 1000D));

        messages.add(
                String.format("Network BandWidth Per Second: %s",
                        TextFormatting.GREEN + MiscUtils.formatNumber((long) networkBandwidthPerSec) + "B/s" + TextFormatting.WHITE)
        );

        if (limit <= 0) {
            return messages;
        }

        @SuppressWarnings("SimplifyStreamApiCallChains")
        List<Map.Entry<Class<?>, Tuple<Long, Long>>> sorted = PacketProfiler.PACKET_TOTAL_SIZE.entrySet().stream()
                .sorted((o1, o2) -> Long.compare(o2.getValue().getSecond(), o1.getValue().getSecond()))
                .limit(limit)
                .collect(Collectors.toList());

        for (final Map.Entry<Class<?>, Tuple<Long, Long>> entry : sorted) {
            Class<?> pClass = entry.getKey();
            long packetTotalAmount = entry.getValue().getFirst();
            long packetTotalSize = entry.getValue().getSecond();

            if (pClass.isAssignableFrom(Packet.class)) {
                messages.add(
                        String.format("Pkt Class: %s",
                                TextFormatting.BLUE + pClass.getSimpleName() + TextFormatting.WHITE
                        )
                );
            } else {
                messages.add(
                        String.format("Mod Pkt Class: %s",
                                TextFormatting.BLUE + pClass.getName() + TextFormatting.WHITE
                        )
                );
            }

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
