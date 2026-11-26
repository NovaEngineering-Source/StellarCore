package github.kasuminova.stellarcore.client.integration.enderutilities;

import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.model.TRSRTransformation;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class NullifierCompositorTest {
    private static final TextureAtlasSprite SPRITE = new TextureAtlasSprite("nullifier-test") { };

    private static NullifierBakedModel model(final IBakedModel base, final IBakedModel locked) {
        return new NullifierBakedModel(base, locked, ImmutableMap.of(), TRSRTransformation.identity());
    }

    private static BakedQuad quad(final float x) {
        final int stride = DefaultVertexFormats.ITEM.getIntegerSize();
        final int[] words = new int[stride * 4];
        for (int i = 0; i < 4; i++) {
            final int at = i * stride;
            words[at] = Float.floatToRawIntBits(x + (i % 2) * .1F);
            words[at + 1] = Float.floatToRawIntBits(i / 2F);
            words[at + 2] = Float.floatToRawIntBits(.3F);
            words[at + 3] = 0xBBAA9988;
            words[at + 4] = Float.floatToRawIntBits(.23F);
            words[at + 5] = Float.floatToRawIntBits(.79F);
            words[at + 6] = 0x2A007F00;
        }
        return new BakedQuad(words, 3, EnumFacing.UP, SPRITE, true, DefaultVertexFormats.ITEM);
    }

    @Test
    void composesLockedAndUnlockedModelsAndOwnsItsPublishedLists() {
        final BakedQuad shell = quad(.05F), overlay = quad(.15F), item = quad(.75F);
        final int[] unchanged = item.getVertexData().clone();
        final NullifierBakedModel model = model(new Stub(shell), new Stub(overlay));
        final Stub source = new Stub(item);
        final List<BakedQuad> unlocked = model.resolve(false, source).getQuads(null, null, 0);
        final List<BakedQuad> locked = model.resolve(true, source).getQuads(null, null, 0);
        assertEquals(2, unlocked.size());
        assertEquals(3, locked.size());
        assertSame(shell, unlocked.get(0));
        assertSame(shell, locked.get(0));
        assertSame(overlay, locked.get(1));
        assertSame(unlocked.get(1), locked.get(2));
        assertNotSame(item, unlocked.get(1));
        assertArrayEquals(unchanged, item.getVertexData());
        assertEquals(item.getTintIndex(), unlocked.get(1).getTintIndex());
        assertSame(item.getSprite(), unlocked.get(1).getSprite());
        assertEquals(item.shouldApplyDiffuseLighting(), unlocked.get(1).shouldApplyDiffuseLighting());
        assertEquals(item.getFormat(), unlocked.get(1).getFormat());
        assertThrows(UnsupportedOperationException.class, unlocked::clear);
        assertThrows(UnsupportedOperationException.class, locked::clear);
    }

    @Test
    void capturesExternalShellListsAndReusesOwnedBackingArraysWithoutArrayTypeCasts() {
        final BakedQuad first = quad(.1F), second = quad(.2F);
        final Stub shell = new Stub(first, second);
        final NullifierBakedModel model = model(shell, new Stub());
        shell.sides[0].clear();
        final List<BakedQuad> empty = model.resolve(false, null).getQuads(null, null, 0);
        assertEquals(2, empty.size());
        assertSame(first, empty.get(0));
        assertSame(second, empty.get(1));
        final List<BakedQuad> composed = model.resolve(false, new Stub(first, second)).getQuads(null, null, 0);
        assertEquals(4, composed.size());
        assertEquals(first.getVertexData().length, composed.get(2).getVertexData().length);
        assertEquals(second.getVertexData().length, composed.get(3).getVertexData().length);
    }

    @Test
    void invalidatesSameObjectModelsAfterGeometryChangesWithoutMutatingEarlierOutputs() {
        final BakedQuad sourceQuad = quad(.3F);
        final Stub source = new Stub(sourceQuad);
        final NullifierBakedModel model = model(new Stub(), new Stub());
        final IBakedModel first = model.resolve(false, source);
        final int[] before = first.getQuads(null, null, 0).get(0).getVertexData().clone();
        assertSame(first, model.resolve(false, source));
        sourceQuad.getVertexData()[0] = Float.floatToRawIntBits(.9F);
        final IBakedModel second = model.resolve(false, source);
        assertNotSame(first, second);
        assertArrayEquals(before, first.getQuads(null, null, 0).get(0).getVertexData());
        assertNotEquals(before[0], second.getQuads(null, null, 0).get(0).getVertexData()[0]);
    }

    @Test
    void readsEachProducerOnlyOnceOnACacheMissAndKeepsCacheAdmissionBounded() throws Exception {
        final BakedQuad original = quad(.3F);
        final AtomicInteger reads = new AtomicInteger();
        final BakedQuad counted = new BakedQuad(original.getVertexData(), original.getTintIndex(), original.getFace(),
            SPRITE, original.shouldApplyDiffuseLighting(), original.getFormat()) {
            @Override
            public int[] getVertexData() {
                reads.incrementAndGet();
                return super.getVertexData();
            }
        };
        final NullifierBakedModel model = model(new Stub(), new Stub());
        final Stub source = new Stub(counted);
        model.resolve(false, source);
        assertEquals(1, reads.get());
        model.resolve(false, source);
        assertEquals(2, reads.get());
        final List<IBakedModel> producers = new ObjectArrayList<>();
        for (int i = 0; i < NullifierBakedModel.MAX_CACHE_ENTRIES + 17; i++) {
            final IBakedModel producer = new Stub(quad(i / 100F));
            producers.add(producer);
            model.resolve(false, producer);
        }
        final Field cache = NullifierBakedModel.class.getDeclaredField("cache");
        cache.setAccessible(true);
        int retained = 0;
        for (final Object entry : (Object[]) cache.get(model)) if (entry != null) retained++;
        assertTrue(retained <= NullifierBakedModel.MAX_CACHE_ENTRIES);
        final Field words = NullifierBakedModel.class.getDeclaredField("words");
        words.setAccessible(true);
        assertTrue(words.getInt(model) <= NullifierBakedModel.MAX_CACHE_WORDS);
        final Field quads = NullifierBakedModel.class.getDeclaredField("quads");
        quads.setAccessible(true);
        assertTrue(quads.getInt(model) <= NullifierBakedModel.MAX_CACHE_QUADS);
    }

    private static final class Stub implements IBakedModel {
        private final List<BakedQuad>[] sides;

        @SuppressWarnings("unchecked")
        private Stub(final BakedQuad... quads) {
            sides = new List[7];
            Arrays.fill(sides, ObjectLists.emptyList());
            sides[0] = new ObjectArrayList<>(quads);
        }

        @Override
        public List<BakedQuad> getQuads(final IBlockState state, final EnumFacing side, final long seed) {
            return sides[side == null ? 0 : side.ordinal() + 1];
        }

        @Override public boolean isAmbientOcclusion() { return true; }
        @Override public boolean isGui3d() { return false; }
        @Override public boolean isBuiltInRenderer() { return false; }
        @Override public TextureAtlasSprite getParticleTexture() { return SPRITE; }
        @Override public ItemCameraTransforms getItemCameraTransforms() { return ItemCameraTransforms.DEFAULT; }
        @Override public ItemOverrideList getOverrides() { return ItemOverrideList.NONE; }
    }
}
