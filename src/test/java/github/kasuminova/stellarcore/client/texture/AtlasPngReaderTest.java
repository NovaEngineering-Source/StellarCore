package github.kasuminova.stellarcore.client.texture;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.SimpleResource;
import net.minecraft.client.resources.data.AnimationMetadataSection;
import net.minecraft.client.resources.data.AnimationMetadataSectionSerializer;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasPngReaderTest {
    @Test
    void matchesGenericReaderForSmallPaletteAnimatedInterlacedAndLargeRgba() throws IOException {
        final byte[] channel = {0, 32, (byte) 185, (byte) 255};
        final byte[] alpha = {0, (byte) 128, (byte) 255, (byte) 255};
        final IndexColorModel palette = new IndexColorModel(2, 4, channel, channel, channel, alpha);
        final BufferedImage indexed = new BufferedImage(16, 32, BufferedImage.TYPE_BYTE_INDEXED, palette);
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 16; x++) indexed.getRaster().setSample(x, y, 0, (x + y) % 4);
        }
        verify(indexed, true, true);
        final BufferedImage argb = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        final Random colors = new Random(77);
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) argb.setRGB(x, y, colors.nextInt());
        }
        verify(argb, false, false);
    }

    private static void verify(final BufferedImage source, final boolean animation, final boolean small) throws IOException {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        final ImageWriter writer = ImageIO.getImageWritersByFormatName("PNG").next();
        try (ImageOutputStream target = ImageIO.createImageOutputStream(bytes)) {
            writer.setOutput(target);
            final ImageWriteParam params = writer.getDefaultWriteParam();
            if (animation) params.setProgressiveMode(ImageWriteParam.MODE_DEFAULT);
            writer.write(null, new IIOImage(source, null, null), params);
        } finally {
            writer.dispose();
        }
        final byte[] encoded = bytes.toByteArray();
        assertEquals(small, encoded.length < 2048);
        final BufferedImage expected = ImageIO.read(new ByteArrayInputStream(encoded));
        final TextureAtlasSprite sprite = new TextureAtlasSprite("test") {};
        final TestResource resource = new TestResource(encoded, animation);
        final AtlasPngReader reader = AtlasPngReader.forAtlas();
        assertNotNull(reader);
        final BufferedImage actual = reader.read(resource, sprite);
        final int width = expected.getWidth(), height = expected.getHeight();
        assertArrayEquals(expected.getRGB(0, 0, width, height, null, 0, width),
            actual.getRGB(0, 0, width, height, null, 0, width));
        assertEquals(width, sprite.getIconWidth());
        assertEquals(animation ? width : height, sprite.getIconHeight());
        assertEquals(1, resource.opens);
        assertTrue(resource.closed);
        assertEquals(animation, resource.getMetadata("animation") != null);
    }

    @Test
    void closesInputAndRejectsBrokenHeader() {
        final TestResource resource = new TestResource(new byte[20], false);
        final AtlasPngReader reader = AtlasPngReader.forAtlas();
        assertNotNull(reader);
        assertThrows(IOException.class, () -> reader.read(resource, new TextureAtlasSprite("invalid") {}));
        assertTrue(resource.closed);
    }

    private static final class TestResource extends SimpleResource {
        final byte[] encoded;
        int opens;
        boolean closed;

        TestResource(final byte[] encoded, final boolean animation) {
            super("test", new ResourceLocation("test", "sprite.png"), null, animation
                ? new ByteArrayInputStream("{\"animation\":{\"frametime\":2}}".getBytes(StandardCharsets.UTF_8)) : null, metadata());
            this.encoded = encoded;
        }

        private static MetadataSerializer metadata() {
            final MetadataSerializer serializer = new MetadataSerializer();
            serializer.registerMetadataSectionType(new AnimationMetadataSectionSerializer(), AnimationMetadataSection.class);
            return serializer;
        }

        @Override
        public InputStream getInputStream() {
            opens++;
            return new ByteArrayInputStream(encoded) {
                @Override public void close() { closed = true; }
            };
        }
    }
}
