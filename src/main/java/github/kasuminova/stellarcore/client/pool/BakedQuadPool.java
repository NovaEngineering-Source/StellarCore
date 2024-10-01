package github.kasuminova.stellarcore.client.pool;

import github.kasuminova.stellarcore.common.pool.AsyncCanonicalizePool;
import github.kasuminova.stellarcore.common.pool.CanonicalizeTask;
import github.kasuminova.stellarcore.common.util.StellarLog;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

public class BakedQuadPool extends AsyncCanonicalizePool<BakedQuad> {

    public static final BakedQuadPool INSTANCE = new BakedQuadPool();

    private final ObjectOpenCustomHashSet<BakedQuad> pool = new ObjectOpenCustomHashSet<>(BakedQuadStrategy.INSTANCE);

    private BakedQuadPool() {
    }

    public void canonicalizeAsync(final Runnable task) {
        worker.offer(new CanonicalizeTask<>(() -> {
            task.run();
            return null;
        }, null));
    }

    @Override
    protected BakedQuad canonicalizeInternal(final BakedQuad target) {
        return pool.addOrGet(target);
    }

    @Override
    public void onClearPre() {
        StellarLog.LOG.info("[StellarCore-BakedQuadPool] {} BakedQuad processed. {} Unique, {} Deduplicated.",
                getProcessedCount(), pool.size(), getProcessedCount() - pool.size()
        );
    }

    @Override
    public void onClearPost() {
        StellarLog.LOG.info("[StellarCore-BakedQuadPool] Pool cleared.");
    }

    @Override
    protected void clearPool() {
        pool.clear();
        pool.trim();
    }

    @Override
    protected Set<BakedQuad> getPoolKeySet() {
        return pool;
    }

    @Override
    protected String getName() {
        return "BakedQuadPool";
    }

    private static class BakedQuadStrategy implements Hash.Strategy<BakedQuad> {

        private static final BakedQuadStrategy INSTANCE = new BakedQuadStrategy();

        private BakedQuadStrategy() {
        }

        @Override
        public int hashCode(final BakedQuad o) {
            int[] vertexData = o.getVertexData();
            int tintIndex = o.getTintIndex();
            EnumFacing face = o.getFace();
            TextureAtlasSprite sprite = o.getSprite();
            VertexFormat format = o.getFormat();
            boolean diffuseLighting = o.shouldApplyDiffuseLighting();
            return Objects.hash(Arrays.hashCode(vertexData), tintIndex, face, sprite, format, diffuseLighting);
        }

        @Override
        public boolean equals(final BakedQuad a, final BakedQuad b) {
            if (a == b) {
                return true;
            }
            if (a == null || b == null) {
                return false;
            }
            if (!Arrays.equals(a.getVertexData(), b.getVertexData())) {
                return false;
            }
            if (a.getTintIndex() != b.getTintIndex()) {
                return false;
            }
            if (a.getFace() != b.getFace()) {
                return false;
            }
            if (a.shouldApplyDiffuseLighting() != b.shouldApplyDiffuseLighting()) {
                return false;
            }
            return a.getSprite().equals(b.getSprite());
        }

    }

}
