package github.kasuminova.stellarcore.common.pool;

import github.kasuminova.stellarcore.common.mod.Mods;
import github.kasuminova.stellarcore.shaded.org.jctools.maps.NonBlockingHashMap;
import mirror.normalasm.api.NormalStringPool;
import net.minecraftforge.fml.common.Optional;
import zone.rong.loliasm.api.LoliStringPool;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

public class LowerCaseStringPool extends AsyncCanonicalizePoolBase<String> {

    public static final LowerCaseStringPool INSTANCE = new LowerCaseStringPool();

    private final NonBlockingHashMap<String, String> lowerCasePool = new NonBlockingHashMap<>();
    private final AtomicLong processedCount = new AtomicLong();

    private LowerCaseStringPool() {
        Thread thread = getWorker().getThread();
        thread.setPriority(Thread.MAX_PRIORITY);
    }

    @Override
    public String canonicalize(@Nullable final String target) {
        if (target == null) {
            return null;
        }
        processedCount.incrementAndGet();

        final String cached = lowerCasePool.get(target);
        if (cached != null) {
            return cached;
        }

        final String value = target.toLowerCase(Locale.ROOT);
        final String existing = lowerCasePool.putIfAbsent(target, value);
        if (existing != null) {
            return existing;
        }
        if (Mods.CENSORED_ASM.loaded()) {
            canonicalizeFromLoliStringPool(target, value);
        } else if (Mods.FERMIUM_OR_BLAHAJ_ASM.loaded()) {
            canonicalizeFromNormalStringPool(target, value);
        }
        return value;
    }

    @Override
    public long getProcessedCount() {
        return processedCount.get();
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
        processedCount.set(0);
        lowerCasePool.clear();
        Thread thread = getWorker().getThread();
        thread.setPriority(Thread.NORM_PRIORITY);
    }

    @Optional.Method(modid = "loliasm")
    protected void canonicalizeFromLoliStringPool(final String t, final String ret) {
        worker.offer(new CanonicalizeTask<>(() -> {
            String key = canonicalizeFromLoliStringPool(t);
            String value = canonicalizeFromLoliStringPool(ret);
            lowerCasePool.put(key, value);
            // just a async task.
            return null;
        }, null));
    }

    @Optional.Method(modid = "normalasm")
    protected void canonicalizeFromNormalStringPool(final String t, final String ret) {
        worker.offer(new CanonicalizeTask<>(() -> {
            String key = canonicalizeFromNormalStringPool(t);
            String value = canonicalizeFromNormalStringPool(ret);
            lowerCasePool.put(key, value);
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
