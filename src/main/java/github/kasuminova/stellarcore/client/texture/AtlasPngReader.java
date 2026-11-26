package github.kasuminova.stellarcore.client.texture;

import net.minecraft.client.renderer.texture.PngSizeInfo;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.IResource;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageInputStreamImpl;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.Iterator;

public final class AtlasPngReader {
    private static final int PREFIX_SIZE = 2048;
    private static final ThreadLocal<byte[]> PREFIX = new ThreadLocal<>();
    private final ImageReaderSpi provider;

    private AtlasPngReader(final ImageReaderSpi provider) {
        this.provider = provider;
    }

    /** Preserve format detection when the registry offers competing PNG decoders. */
    public static AtlasPngReader forAtlas() {
        final Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("PNG");
        if (!readers.hasNext()) return null;
        final ImageReader first = readers.next();
        if (first == null) return null;
        try {
            final ImageReaderSpi provider = first.getOriginatingProvider();
            return provider != null && !readers.hasNext() ? new AtlasPngReader(provider) : null;
        } finally {
            first.dispose();
        }
    }

    public BufferedImage read(final IResource resource, final TextureAtlasSprite sprite) throws IOException {
        byte[] prefix = PREFIX.get();
        if (prefix == null) prefix = new byte[PREFIX_SIZE];
        // An animation metadata hook or custom loadSprite can start another read on this thread.
        PREFIX.set(null);
        try (InputStream raw = resource.getInputStream()) {
            int size = 0;
            while (size < prefix.length) {
                final int count = raw.read(prefix, size, prefix.length - size);
                if (count == -1) break;
                size += count;
            }
            // PngSizeInfo closes its input. Give it just the buffered bytes, leaving the resource in place.
            final PngSizeInfo header = new PngSizeInfo(new ByteArrayInputStream(prefix, 0, size));
            sprite.loadSprite(header, resource.getMetadata("animation") != null);
            try (ImageInputStream input = size < prefix.length
                ? new PrefixImageInput(prefix, size)
                : ImageIO.createImageInputStream(new SequenceInputStream(new ByteArrayInputStream(prefix), raw))) {
                if (input == null) throw new IOException("No ImageInputStream for atlas PNG");
                // ImageReader is mutable. Only its stateless factory is shared by atlas workers.
                final ImageReader reader = provider.createReaderInstance();
                try {
                    reader.setInput(input, true, true);
                    return reader.read(0, reader.getDefaultReadParam());
                } finally {
                    reader.dispose();
                }
            }
        } finally {
            PREFIX.set(prefix);
        }
    }

    /** A small encoded sprite fits entirely in the worker buffer and needs no ImageIO cache blocks. */
    private static final class PrefixImageInput extends ImageInputStreamImpl {
        private final byte[] bytes;
        private final int size;

        PrefixImageInput(final byte[] bytes, final int size) {
            this.bytes = bytes;
            this.size = size;
        }

        @Override
        public int read() throws IOException {
            checkClosed();
            bitOffset = 0;
            return streamPos < size ? bytes[(int) streamPos++] & 0xff : -1;
        }

        @Override
        public int read(final byte[] buffer, final int offset, final int length) throws IOException {
            checkClosed();
            if (offset < 0 || length < 0 || offset > buffer.length - length) throw new IndexOutOfBoundsException();
            bitOffset = 0;
            if (length == 0) return 0;
            if (streamPos >= size) return -1;
            final int count = Math.min(size - (int) streamPos, length);
            System.arraycopy(bytes, (int) streamPos, buffer, offset, count);
            streamPos += count;
            return count;
        }

        @Override
        public long length() {
            return size;
        }
    }
}
