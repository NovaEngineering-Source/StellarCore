package github.kasuminova.stellarcore.client.memory;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.junit.jupiter.api.Test;

import java.nio.IntBuffer;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RenderBufferCapacityTest {
    @Test
    void growingBufferPreservesEveryVertexWord() {
        BufferBuilder builder = new BufferBuilder(RenderBufferCapacity.initialInts(2097152));
        int initialBytes = builder.getByteBuffer().capacity();
        int[] words = new int[(initialBytes / Integer.BYTES / 3 + 1) * 6];
        Random random = new Random(12345);
        for (int i = 0; i < words.length; i++) words[i] = random.nextInt();
        builder.begin(7, DefaultVertexFormats.POSITION);
        builder.addVertexData(words);
        builder.finishDrawing();
        assertEquals(words.length / 3, builder.getVertexCount());
        IntBuffer contents = builder.getByteBuffer().asIntBuffer();
        int[] actual = new int[contents.remaining()];
        contents.get(actual);
        assertArrayEquals(words, actual);
    }
}
