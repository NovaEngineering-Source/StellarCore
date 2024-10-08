package github.kasuminova.stellarcore.common.pool;

import github.kasuminova.stellarcore.common.util.StellarLog;
import github.kasuminova.stellarcore.mixin.util.StellarPooledNBT;
import net.minecraft.nbt.*;

import java.util.Arrays;

public class NBTTagPrimitivePool {

    private static final NBTTagByte  [] TAG_BYTES   = new NBTTagByte  [256];
    private static final NBTTagShort [] TAG_SHORTS  = new NBTTagShort [65536];
    private static final NBTTagInt   [] TAG_INTS    = new NBTTagInt   [65536];
    private static final NBTTagLong  [] TAG_LONGS   = new NBTTagLong  [65536];
    private static final NBTTagFloat [] TAG_FLOATS  = new NBTTagFloat [65536];
    private static final NBTTagDouble[] TAG_DOUBLES = new NBTTagDouble[65536];

    private static final float EPSILON = 0.0000001F;

    public static final int BYTE_OFFSET  = 128;
    public static final int SHORT_OFFSET = 32768;
    public static final int MINIMUM = -32768;
    public static final int MAXIMUM = 32767;

    static {
        final long start = System.currentTimeMillis();
        Arrays.setAll(TAG_BYTES,   i -> new NBTTagByte  ((byte)  (i - BYTE_OFFSET)));
        Arrays.setAll(TAG_SHORTS,  i -> new NBTTagShort ((short) (i - SHORT_OFFSET)));
        Arrays.setAll(TAG_INTS,    i -> new NBTTagInt   (i - SHORT_OFFSET));
        Arrays.setAll(TAG_LONGS,   i -> new NBTTagLong  (i - SHORT_OFFSET));
        Arrays.setAll(TAG_FLOATS,  i -> new NBTTagFloat (i - SHORT_OFFSET));
        Arrays.setAll(TAG_DOUBLES, i -> new NBTTagDouble(i - SHORT_OFFSET));
        Arrays.stream(TAG_BYTES)  .forEach(tag -> ((StellarPooledNBT) tag).stellar_core$setPooled(true));
        Arrays.stream(TAG_SHORTS) .forEach(tag -> ((StellarPooledNBT) tag).stellar_core$setPooled(true));
        Arrays.stream(TAG_INTS)   .forEach(tag -> ((StellarPooledNBT) tag).stellar_core$setPooled(true));
        Arrays.stream(TAG_LONGS)  .forEach(tag -> ((StellarPooledNBT) tag).stellar_core$setPooled(true));
        Arrays.stream(TAG_FLOATS) .forEach(tag -> ((StellarPooledNBT) tag).stellar_core$setPooled(true));
        Arrays.stream(TAG_DOUBLES).forEach(tag -> ((StellarPooledNBT) tag).stellar_core$setPooled(true));
        StellarLog.LOG.info("[StellarCore-NBTTagPrimitivePool] PrimitiveType NBTTagPrimitivePool initialized, took {}ms.", System.currentTimeMillis() - start);
    }

    public static NBTTagByte getTagByte(final NBTTagByte tag) {
        if (((StellarPooledNBT) tag).stellar_core$isPooled()) {
            return tag;
        }
        return TAG_BYTES[tag.getByte() + 128];
    }

    public static NBTTagByte getTagByte(final byte b) {
        return TAG_BYTES[b + 128];
    }

    public static NBTTagShort getTagShort(final NBTTagShort tag) {
        if (((StellarPooledNBT) tag).stellar_core$isPooled()) {
            return tag;
        }
        return TAG_SHORTS[tag.getShort() + SHORT_OFFSET];
    }

    public static NBTTagShort getTagShort(final short s) {
        return TAG_SHORTS[s + SHORT_OFFSET];
    }

    public static NBTTagInt getTagInt(final NBTTagInt tag) {
        if (((StellarPooledNBT) tag).stellar_core$isPooled()) {
            return tag;
        }
        final int value = tag.getInt();
        return value < MINIMUM || value > MAXIMUM
                ? tag
                : TAG_INTS[value + SHORT_OFFSET];
    }

    public static NBTTagInt getTagInt(final int i) {
        return i < MINIMUM || i > MAXIMUM
                ? new NBTTagInt(i)
                : TAG_INTS[i + SHORT_OFFSET];
    }

    public static NBTTagLong getTagLong(final NBTTagLong tag) {
        if (((StellarPooledNBT) tag).stellar_core$isPooled()) {
            return tag;
        }
        final long value = tag.getLong();
        return value < MINIMUM || value > MAXIMUM
                ? tag
                : TAG_LONGS[(int) (value + SHORT_OFFSET)];
    }

    public static NBTTagLong getTagLong(final long l) {
        return l < MINIMUM || l > MAXIMUM
                ? new NBTTagLong(l)
                : TAG_LONGS[(int) (l + SHORT_OFFSET)];
    }

    public static NBTTagFloat getTagFloat(final NBTTagFloat tag) {
        if (((StellarPooledNBT) tag).stellar_core$isPooled()) {
            return tag;
        }
        final float value = tag.getFloat();
        return isFloatInteger(value)
                ? value < MINIMUM || value > MAXIMUM ? tag : TAG_FLOATS[((int) value + SHORT_OFFSET)]
                : tag;
    }

    public static NBTTagFloat getTagFloat(final float f) {
        return isFloatInteger(f) 
                ? f < MINIMUM || f > MAXIMUM ? new NBTTagFloat(f) : TAG_FLOATS[((int) f + SHORT_OFFSET)] 
                : new NBTTagFloat(f);
    }

    public static NBTTagDouble getTagDouble(final NBTTagDouble tag) {
        if (((StellarPooledNBT) tag).stellar_core$isPooled()) {
            return tag;
        }
        final double value = tag.getDouble();
        return isDoubleInteger(value) 
                ? value < MINIMUM || value > MAXIMUM ? tag : TAG_DOUBLES[((int) value + SHORT_OFFSET)]
                : tag;
    }

    public static NBTTagDouble getTagDouble(final double d) {
        return isDoubleInteger(d)
                ? d < MINIMUM || d > MAXIMUM ? new NBTTagDouble(d)
                : TAG_DOUBLES[((int) d + SHORT_OFFSET)] : new NBTTagDouble(d);
    }

    public static boolean isFloatInteger(float value) {
        return Math.abs(value - Math.round(value)) < EPSILON;
    }

    public static boolean isDoubleInteger(double value) {
        return Math.abs(value - Math.round(value)) < EPSILON;
    }

}
