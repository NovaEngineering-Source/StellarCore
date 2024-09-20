package github.kasuminova.stellarcore.client.pool;

import github.kasuminova.stellarcore.common.mod.Mods;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraftforge.fml.common.Optional;
import zone.rong.loliasm.api.LoliStringPool;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.function.Consumer;

public class ResourceLocationPool extends AsyncCanonicalizePoolBase<String> {

    public static final ResourceLocationPool INSTANCE = new ResourceLocationPool();

    private final Object2ObjectOpenHashMap<String, String> lowerCasePool = new Object2ObjectOpenHashMap<>();
    private volatile long processedCount = 0;

    private ResourceLocationPool() {
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
                }
                return value;
            });
        }
    }

    @Override
    public void canonicalizeAsync(final String target, final Consumer<String> callback) {
        throw new UnsupportedOperationException("ResourceLocationPool does not supported yet.");
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
        return "ResourceLocationPool";
    }

    @Override
    public void clear() {
        synchronized (lowerCasePool) {
            processedCount = 0;
            lowerCasePool.clear();
        }
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

    @Optional.Method(modid = "loliasm")
    private static String canonicalizeFromLoliStringPool(final String target) {
        return LoliStringPool.canonicalize(target);
    }

}
