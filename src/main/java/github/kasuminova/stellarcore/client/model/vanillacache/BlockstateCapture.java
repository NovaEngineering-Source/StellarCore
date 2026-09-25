package github.kasuminova.stellarcore.client.model.vanillacache;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public final class BlockstateCapture {

    private static final ThreadLocal<List<byte[]>> CURRENT = new ThreadLocal<>();

    private BlockstateCapture() {
    }

    public static void begin() {
        CURRENT.set(new ArrayList<>(2));
    }

    public static byte[][] collect() {
        final List<byte[]> parts = CURRENT.get();
        CURRENT.remove();
        if (parts == null || parts.isEmpty()) {
            return null;
        }
        return parts.toArray(new byte[0][]);
    }

    public static void discard() {
        CURRENT.remove();
    }

    public static InputStream tee(final InputStream delegate) {
        final List<byte[]> sink = CURRENT.get();
        return sink == null ? delegate : new TeeStream(delegate, sink);
    }

    private static final class TeeStream extends FilterInputStream {
        private final List<byte[]> sink;
        private final ByteArrayOutputStream captured = new ByteArrayOutputStream(8192);
        private boolean committed;

        TeeStream(final InputStream delegate, final List<byte[]> sink) {
            super(delegate);
            this.sink = sink;
        }

        @Override
        public int read() throws IOException {
            final int value = super.read();
            if (value >= 0) {
                captured.write(value);
            }
            return value;
        }

        @Override
        public int read(@Nonnull final byte[] buffer, final int offset, final int length) throws IOException {
            final int read = super.read(buffer, offset, length);
            if (read > 0) {
                captured.write(buffer, offset, read);
            }
            return read;
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                if (!committed) {
                    committed = true;
                    if (captured.size() > 0) {
                        sink.add(captured.toByteArray());
                    }
                }
            }
        }
    }
}
