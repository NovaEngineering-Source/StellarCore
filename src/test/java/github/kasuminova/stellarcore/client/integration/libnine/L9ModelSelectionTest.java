package github.kasuminova.stellarcore.client.integration.libnine;

import com.google.gson.stream.JsonReader;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class L9ModelSelectionTest {
    private final ResourceLocation key = new ResourceLocation("custompack", "models/item/probe.json");
    private final Object missing = new Object();

    @AfterEach void disable() { L9ModelSelection.disable(); }

    @Test void archiveAndDirectoryOverlayAreReopenedForTheNextSelection(@TempDir Path dir) throws Exception {
        Path zip = dir.resolve("models.zip");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip))) {
            out.putNextEntry(new ZipEntry("models/item/probe.json"));
            out.write("{\"9s\":\"pi\"}".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
        Path overlay = dir.resolve("probe.json");
        AtomicInteger opens = new AtomicInteger();
        try (ZipFile archive = new ZipFile(zip.toFile())) {
            Function<ResourceLocation, Object> read = location -> {
                opens.incrementAndGet();
                try (InputStream in = Files.exists(overlay) ? Files.newInputStream(overlay)
                        : archive.getInputStream(archive.getEntry(location.getPath()));
                     JsonReader json = new JsonReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    return L9ModelTypeReader.read(json);
                } catch (Exception e) { throw new AssertionError(e); }
            };
            L9ModelSelection.enable();
            try (L9ModelSelection ignored = L9ModelSelection.open()) {
                assertEquals("pi", L9ModelSelection.read(key, read));
                assertEquals("pi", L9ModelSelection.read(new ResourceLocation(key.toString()), read));
            }
            Files.write(overlay, "{\"9s\":\"sum\"}".getBytes(StandardCharsets.UTF_8));
            try (L9ModelSelection ignored = L9ModelSelection.open()) {
                assertEquals("sum", L9ModelSelection.read(key, read));
                assertEquals("sum", L9ModelSelection.read(key, read));
            }
            Files.delete(overlay);
            try (L9ModelSelection ignored = L9ModelSelection.open()) {
                assertEquals("pi", L9ModelSelection.read(key, read));
            }
            assertEquals(3, opens.get());
        }
    }

    @Test void missingFileIsSharedOnlyForItsSelectionAndInvalidReadsAreRetried() {
        L9ModelSelection.enable();
        try (L9ModelSelection ignored = L9ModelSelection.open()) {
            assertSame(missing, L9ModelSelection.read(key, loc -> missing));
            assertSame(missing, L9ModelSelection.read(key, loc -> fail("second search")));
        }
        try (L9ModelSelection ignored = L9ModelSelection.open()) {
            assertNull(L9ModelSelection.read(key, loc -> null));
            assertEquals("sum", L9ModelSelection.read(key, loc -> "sum"));
        }
    }

    @Test void differentKeysNestedSelectionsAndOtherThreadsAreIsolated() {
        ResourceLocation other = new ResourceLocation("different", "item");
        L9ModelSelection.enable();
        try (L9ModelSelection outer = L9ModelSelection.open()) {
            assertEquals("pi", L9ModelSelection.read(key, loc -> "pi"));
            assertEquals("other", L9ModelSelection.read(other, loc -> "other"));
            assertThrows(IllegalStateException.class, () -> {
                try (L9ModelSelection inner = L9ModelSelection.open()) {
                    assertEquals("sum", L9ModelSelection.read(key, loc -> "sum"));
                    throw new IllegalStateException();
                }
            });
            assertEquals("worker", CompletableFuture.supplyAsync(() -> {
                try (L9ModelSelection worker = L9ModelSelection.open()) {
                    return L9ModelSelection.read(key, loc -> "worker");
                }
            }).join());
            assertEquals("pi", L9ModelSelection.read(key, loc -> fail("outer result lost")));
        }
        assertEquals("fresh", L9ModelSelection.read(key, loc -> "fresh"));
    }

    @Test void lifecycleChangesInvalidateEvenAnInFlightSelection() {
        assertNull(L9ModelSelection.open());
        L9ModelSelection.enable();
        try (L9ModelSelection first = L9ModelSelection.open()) {
            assertSame(missing, L9ModelSelection.read(key, loc -> missing));
            L9ModelSelection.reload();
            assertEquals("new", L9ModelSelection.read(key, loc -> "new"));
            L9ModelSelection.disable();
            assertNull(L9ModelSelection.open());
            assertEquals("off", L9ModelSelection.read(key, loc -> "off"));
            L9ModelSelection.enable();
            assertEquals("on", L9ModelSelection.read(key, loc -> "on"));
        }
    }
}
