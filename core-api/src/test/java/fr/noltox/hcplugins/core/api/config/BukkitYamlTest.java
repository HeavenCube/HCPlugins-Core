package fr.noltox.hcplugins.core.api.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("java:S5960")
class BukkitYamlTest {

    @TempDir
    Path directory;

    @Test
    void closesDefaultsWhenAdministratorYamlIsInvalid() throws Exception {
        Path config = directory.resolve("config.yml");
        Files.writeString(config, "value: first\nvalue: duplicate\n");
        TrackingStream defaults = new TrackingStream("value: default\n");
        assertThrows(IllegalStateException.class, () -> BukkitYaml.load(config, defaults));
        assertTrue(defaults.closed);
    }

    @Test
    void closesDefaultsWhenAdministratorFileIsMissing() {
        TrackingStream defaults = new TrackingStream("value: default\n");
        assertThrows(IllegalStateException.class,
                () -> BukkitYaml.load(directory.resolve("missing.yml"), defaults));
        assertTrue(defaults.closed);
    }

    @Test
    void usesDefaultsOnlyForMissingKeysAndClosesStream() throws Exception {
        Path config = directory.resolve("config.yml");
        Files.writeString(config, "value: configured\n");
        TrackingStream defaults = new TrackingStream("value: default\nadded: fallback\n");
        var loaded = BukkitYaml.load(config, defaults);
        assertEquals("configured", loaded.getString("value"));
        assertEquals("fallback", loaded.getString("added"));
        assertTrue(defaults.closed);
    }

    private static final class TrackingStream extends ByteArrayInputStream {
        private boolean closed;

        private TrackingStream(String content) {
            super(content.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
