package github.kasuminova.stellarcore.common.pool;

import github.kasuminova.stellarcore.common.mod.Mods;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import mirror.normalasm.api.NormalStringPool;
import net.minecraftforge.fml.common.Optional;
import zone.rong.loliasm.api.LoliStringPool;

import javax.annotation.Nullable;
import java.util.Locale;

public class LowerCaseStringPool extends AsyncCanonicalizePoolBase<String> {

    public static final LowerCaseStringPool INSTANCE = new LowerCaseStringPool();

    private final Object2ObjectOpenHashMap<String, String> lowerCasePool = new Object2ObjectOpenHashMap<>();
    private volatile long processedCount = 0;

    private LowerCaseStringPool() {
        Thread thread = getWorker().getThread();
        thread.setPriority(Thread.MAX_PRIORITY);
    }

    @Override
    public String canonicalize(@Nullable final String target) {
        if (target == null) {
            return null;
        }
        synchronized (lowerCasePool) {
            processedCount++;
            return lowerCasePool.computeIfAbsent(target, key -> {
                String value = key.toLowerCase(Locale.ROOT);
                if (Mods.CENSORED_ASM.loaded()) {
                    canonicalizeFromLoliStringPool(key, value);
                } else if (Mods.FERMIUM_OR_BLAHAJ_ASM.loaded()) {
                    canonicalizeFromNormalStringPool(key, value);
                }
                return value;
            });
        }
    }

    @Override
    public long getProcessedCount() {
        return processedCount;
    }

    @Override
    public int getUniqueCount() {
        return lowerCasePool.size();
    }

    @Override
    protected String getName() {
        return "LowerCaseStringPool";
    }

    @Override
    public void clear() {
        synchronized (lowerCasePool) {
            processedCount = 0;
            lowerCasePool.clear();
        }
        Thread thread = getWorker().getThread();
        thread.setPriority(Thread.NORM_PRIORITY);
    }

    @Optional.Method(modid = "loliasm")
    protected void canonicalizeFromLoliStringPool(final String t, final String ret) {
        worker.offer(new CanonicalizeTask<>(() -> {
            String key = canonicalizeFromLoliStringPool(t);
            String value = canonicalizeFromLoliStringPool(ret);
            synchronized (lowerCasePool) {
                lowerCasePool.put(key, value);
            }
            // just a async task.
            return null;
        }, null));
    }

    @Optional.Method(modid = "normalasm")
    protected void canonicalizeFromNormalStringPool(final String t, final String ret) {
        worker.offer(new CanonicalizeTask<>(() -> {
            String key = canonicalizeFromNormalStringPool(t);
            String value = canonicalizeFromNormalStringPool(ret);
            synchronized (lowerCasePool) {
                lowerCasePool.put(key, value);
            }
            // just a async task.
            return null;
        }, null));
    }

    @Optional.Method(modid = "loliasm")
    private static String canonicalizeFromLoliStringPool(final String target) {
        return LoliStringPool.canonicalize(target);
    }

    @Optional.Method(modid = "normalasm")
    private static String canonicalizeFromNormalStringPool(final String target) {
        return NormalStringPool.canonicalize(target);
    }

}
