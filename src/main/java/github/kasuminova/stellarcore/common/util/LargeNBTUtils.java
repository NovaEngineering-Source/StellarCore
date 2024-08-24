package github.kasuminova.stellarcore.common.util;

import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagEnd;
import net.minecraft.util.ReportedException;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.io.*;
import java.util.zip.GZIPInputStream;

@SuppressWarnings("deprecation")
public class LargeNBTUtils {

    public static NBTTagCompound readCompressed(InputStream is) throws IOException {
        DataInputStream datainputstream = new DataInputStream(new BufferedInputStream(new GZIPInputStream(is)));
        NBTTagCompound nbttagcompound;

        try {
            nbttagcompound = readInfinityDepth(datainputstream, NBTSizeTracker.INFINITE);
        } finally {
            datainputstream.close();
        }

        return nbttagcompound;
    }

    public static NBTTagCompound readInfinityDepth(DataInput input, NBTSizeTracker accounter) throws IOException {
        NBTBase nbtbase = readInfinityDepth(input, Integer.MIN_VALUE /* XD */, accounter);

        if (nbtbase instanceof NBTTagCompound) {
            return (NBTTagCompound) nbtbase;
        } else {
            throw new IOException("Root tag must be a named compound tag");
        }
    }

    private static NBTBase readInfinityDepth(DataInput input, int depth, NBTSizeTracker accounter) throws IOException {
        byte b0 = input.readByte();
        accounter.read(8); // Forge: Count everything!

        if (b0 == 0) {
            return new NBTTagEnd();
        }

        NBTSizeTracker.readUTF(accounter, input.readUTF()); // Forge: Count this string.
        accounter.read(32); // Forge: 4 extra bytes for the object allocation.
        NBTBase nbtbase = invokeCreate(b0);

        try {
            invokeRead(nbtbase, input, depth, accounter);
            return nbtbase;
        } catch (IOException ioexception) {
            CrashReport crashreport = CrashReport.makeCrashReport(ioexception, "Loading NBT data");
            CrashReportCategory crashreportcategory = crashreport.makeCategory("NBT Tag");
            crashreportcategory.addCrashSection("Tag type", b0);
            throw new ReportedException(crashreport);
        }
    }

    private static NBTBase invokeCreate(byte id) {
        try {
            return (NBTBase) ReflectionHelper.findMethod(NBTBase.class, "create", "func_150284_a", byte.class).invoke(null, id);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static void invokeRead(NBTBase instance, DataInput input, int depth, NBTSizeTracker sizeTracker) throws IOException {
        try {
            ReflectionHelper.findMethod(NBTBase.class, "read", "func_152446_a", DataInput.class, int.class, NBTSizeTracker.class).invoke(instance, input, depth, sizeTracker);
        } catch (Throwable ex) {
            //noinspection InstanceofCatchParameter
            if (ex instanceof IOException) {
                throw (IOException) ex;
            }
            throw new RuntimeException(ex);
        }
    }

}
