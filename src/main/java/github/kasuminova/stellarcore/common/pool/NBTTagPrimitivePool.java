package github.kasuminova.stellarcore.common.pool;

import github.kasuminova.stellarcore.common.util.StellarLog;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.nbt.NBTTagShort;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class NBTTagPrimitivePool {

    private static final NBTTagByte  [] TAG_BYTES   = new NBTTagByte  [256];
    private static final AtomicReferenceArray<NBTTagShort> TAG_SHORTS = new AtomicReferenceArray<>(65536);
    private static final AtomicReferenceArray<NBTTagInt> TAG_INTS = new AtomicReferenceArray<>(65536);
    private static final AtomicReferenceArray<NBTTagLong> TAG_LONGS = new AtomicReferenceArray<>(65536);
    private static final AtomicReferenceArray<NBTTagFloat> TAG_FLOATS = new AtomicReferenceArray<>(65536);
    private static final AtomicReferenceArray<NBTTagDouble> TAG_DOUBLES = new AtomicReferenceArray<>(65536);

    private static final float EPSILON = 0.0000001F;

    public static final int BYTE_OFFSET  = 128;
    public static final int SHORT_OFFSET = 32768;
    public static final int MINIMUM = -32768;
    public static final int MAXIMUM = 32767;

    static {
        final long start = System.currentTimeMillis();
        Arrays.setAll(TAG_BYTES,   i -> new NBTTagByte  ((byte)  (i - BYTE_OFFSET)));
        StellarLog.LOG.info("[StellarCore-NBTTagPrimitivePool] PrimitiveType NBTTagPrimitivePool initialized, took {}ms.", System.currentTimeMillis() - start);
    }

    public static NBTTagByte getTagByte(final NBTTagByte tag) {
        return TAG_BYTES[tag.getByte() + 128];
    }

    public static NBTTagByte getTagByte(final byte b) {
        return TAG_BYTES[b + 128];
    }

    public static NBTTagShort getTagShort(final NBTTagShort tag) {
        final short value = tag.getShort();
        final int index = value + SHORT_OFFSET;
        final NBTTagShort cached = TAG_SHORTS.get(index);
        return cached == null ? publish(TAG_SHORTS, index, new NBTTagShort(value)) : cached;
    }

    public static NBTTagShort getTagShort(final short s) {
        final int index = s + SHORT_OFFSET;
        final NBTTagShort cached = TAG_SHORTS.get(index);
        return cached == null ? publish(TAG_SHORTS, index, new NBTTagShort(s)) : cached;
    }

    public static NBTTagInt getTagInt(final NBTTagInt tag) {
        final int value = tag.getInt();
        if (value < MINIMUM || value > MAXIMUM) return tag;
        final int index = value + SHORT_OFFSET;
        final NBTTagInt cached = TAG_INTS.get(index);
        return cached == null ? publish(TAG_INTS, index, new NBTTagInt(value)) : cached;
    }

    public static NBTTagInt getTagInt(final int i) {
        if (i < MINIMUM || i > MAXIMUM) return new NBTTagInt(i);
        final int index = i + SHORT_OFFSET;
        final NBTTagInt cached = TAG_INTS.get(index);
        return cached == null ? publish(TAG_INTS, index, new NBTTagInt(i)) : cached;
    }

    public static NBTTagLong getTagLong(final NBTTagLong tag) {
        final long value = tag.getLong();
        if (value < MINIMUM || value > MAXIMUM) return tag;
        final int index = (int) value + SHORT_OFFSET;
        final NBTTagLong cached = TAG_LONGS.get(index);
        return cached == null ? publish(TAG_LONGS, index, new NBTTagLong(value)) : cached;
    }

    public static NBTTagLong getTagLong(final long l) {
        if (l < MINIMUM || l > MAXIMUM) return new NBTTagLong(l);
        final int index = (int) l + SHORT_OFFSET;
        final NBTTagLong cached = TAG_LONGS.get(index);
        return cached == null ? publish(TAG_LONGS, index, new NBTTagLong(l)) : cached;
    }

    public static NBTTagFloat getTagFloat(final NBTTagFloat tag) {
        final float value = tag.getFloat();
        if (!isFloatInteger(value) || value < MINIMUM || value > MAXIMUM) return tag;
        final int index = (int) value + SHORT_OFFSET;
        final NBTTagFloat cached = TAG_FLOATS.get(index);
        return cached == null ? publish(TAG_FLOATS, index, new NBTTagFloat((int) value)) : cached;
    }

    public static NBTTagFloat getTagFloat(final float f) {
        if (!isFloatInteger(f) || f < MINIMUM || f > MAXIMUM) return new NBTTagFloat(f);
        final int index = (int) f + SHORT_OFFSET;
        final NBTTagFloat cached = TAG_FLOATS.get(index);
        return cached == null ? publish(TAG_FLOATS, index, new NBTTagFloat((int) f)) : cached;
    }

    public static NBTTagDouble getTagDouble(final NBTTagDouble tag) {
        final double value = tag.getDouble();
        if (!isDoubleInteger(value) || value < MINIMUM || value > MAXIMUM) return tag;
        final int index = (int) value + SHORT_OFFSET;
        final NBTTagDouble cached = TAG_DOUBLES.get(index);
        return cached == null ? publish(TAG_DOUBLES, index, new NBTTagDouble((int) value)) : cached;
    }

    public static NBTTagDouble getTagDouble(final double d) {
        if (!isDoubleInteger(d) || d < MINIMUM || d > MAXIMUM) return new NBTTagDouble(d);
        final int index = (int) d + SHORT_OFFSET;
        final NBTTagDouble cached = TAG_DOUBLES.get(index);
        return cached == null ? publish(TAG_DOUBLES, index, new NBTTagDouble((int) d)) : cached;
    }

    private static <T> T publish(final AtomicReferenceArray<T> pool, final int index, final T tag) {
        // Startup NBT parsing can run concurrently. Publish only the requested
        // constants, preserving one shared instance and safe visibility of its data.
        return pool.compareAndSet(index, null, tag) ? tag : pool.get(index);
    }

    public static boolean isFloatInteger(float value) {
        return Math.abs(value - Math.round(value)) < EPSILON;
    }

    public static boolean isDoubleInteger(double value) {
        return Math.abs(value - Math.round(value)) < EPSILON;
    }

}
