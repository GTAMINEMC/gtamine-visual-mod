package com.gtamine.visual.update;

import net.fabricmc.loader.api.Version;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import static org.junit.jupiter.api.Assertions.*;

class UpdateValidationTest {
    @TempDir Path directory;

    private Map<String, Version> installed() throws Exception {
        return Map.of("minecraft", Version.parse("1.21.11"), "java", Version.parse("21"),
                "fabricloader", Version.parse("0.19.5"), "fabric-api", Version.parse("0.141.6+1.21.11"));
    }

    private String metadata() {
        return """
                {"id":"gtamine_visual","version":"1.0.2","environment":"client",
                 "depends":{"minecraft":"1.21.11","java":">=21","fabricloader":">=0.19.5",
                 "fabric-api":[">=0.141.6+1.21.11"]}}
                """;
    }

    private Path jar(String metadata) throws IOException {
        Path path = Files.createTempFile(directory, "mod-", ".jar");
        try (var output = new JarOutputStream(Files.newOutputStream(path))) {
            output.putNextEntry(new JarEntry("fabric.mod.json"));
            output.write(metadata.getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
        }
        return path;
    }

    @Test void acceptsCompatibleFabricRelease() throws Exception {
        UpdateValidation.validateJar(jar(metadata()), Version.parse("1.0.2"), installed());
    }

    @Test void rejectsWrongGameModVersionAndDependencies() throws Exception {
        for (String invalid : new String[]{metadata().replace("1.21.11\"", "1.22\""),
                metadata().replace("gtamine_visual", "other_mod"), metadata().replace("1.0.2", "1.0.3"),
                metadata().replace(">=21", ">=25"), metadata().replace(">=0.19.5", ">=0.20.0"),
                metadata().replace("\"fabric-api\":", "\"missing-mod\":"),
                metadata().replace("\"client\"", "\"server\"")}) {
            Path file = jar(invalid);
            assertThrows(IOException.class, () -> UpdateValidation.validateJar(file, Version.parse("1.0.2"), installed()));
        }
    }

    @Test void rejectsDeclaredConflict() throws Exception {
        String metadata = metadata().stripTrailing();
        Path file = jar(metadata.substring(0, metadata.length() - 1) + ",\"breaks\":{\"minecraft\":\"1.21.11\"}}");
        assertThrows(IOException.class, () -> UpdateValidation.validateJar(file, Version.parse("1.0.2"), installed()));
    }

    @Test void onlyAcceptsNumberedStableVersions() throws Exception {
        assertTrue(UpdateValidation.releaseVersion("v1.0.10").compareTo(Version.parse("1.0.9")) > 0);
        assertEquals(0, UpdateValidation.releaseVersion("1.0.2").compareTo(Version.parse("1.0.2")));
        for (String tag : new String[]{"v1.0.2-beta.1", "latest", "v1.0", "../1.0.2"}) {
            assertThrows(IOException.class, () -> UpdateValidation.releaseVersion(tag));
        }
    }
}
