package github.kasuminova.stellarcore.common.itemstack;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;

import java.util.UUID;

public class SharedEmptyTag {
    
    public static final NBTTagCompound EMPTY_TAG = new NBTTagCompound() {

        @Override
        public void setTag(final String key, final NBTBase value) {
        }

        @Override
        public void setByte(final String key, final byte value) {
        }

        @Override
        public void setShort(final String key, final short value) {
        }

        @Override
        public void setInteger(final String key, final int value) {
        }

        @Override
        public void setLong(final String key, final long value) {
        }

        @Override
        public void setUniqueId(final String key, final UUID value) {
        }

        @Override
        public void setFloat(final String key, final float value) {
        }

        @Override
        public void setDouble(final String key, final double value) {
        }

        @Override
        public void setString(final String key, final String value) {
        }

        @Override
        public void setByteArray(final String key, final byte[] value) {
        }

        @Override
        public void setIntArray(final String key, final int[] value) {
        }

        @Override
        public void setBoolean(final String key, final boolean value) {
        }

    };

}
