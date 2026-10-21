package com.gtamine.visual.update;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import static org.junit.jupiter.api.Assertions.*;

class UpdateInstallerTest {
    @TempDir Path directory;

    @Test
    void replacesSameFilenameAndKeepsBackup() throws Exception {
        Path target = Files.writeString(directory.resolve("gtamine-visual-1.0.0.jar"), "old");
        Path staged = Files.writeString(directory.resolve("download.pending"), "new");
        UpdateInstaller.install(target, staged, UpdateInstaller.sha256(target), UpdateInstaller.sha256(staged));
        assertEquals("new", Files.readString(target));
        assertEquals("old", Files.readString(directory.resolve("gtamine-visual-1.0.0.jar.bak")));
        assertFalse(Files.exists(staged));
        try (var files = Files.list(directory)) {
            assertEquals(1, files.filter(path -> path.toString().endsWith(".jar")).count());
        }
    }

    @Test
    void refusesTamperedDownloadAndLeavesInstalledFile() throws Exception {
        Path target = Files.writeString(directory.resolve("mod.jar"), "old");
        Path staged = Files.writeString(directory.resolve("download.pending"), "new");
        String checksum = UpdateInstaller.sha256(staged);
        Files.writeString(staged, "tampered");
        assertThrows(IOException.class, () -> UpdateInstaller.install(target, staged, UpdateInstaller.sha256(target), checksum));
        assertEquals("old", Files.readString(target));
        assertFalse(Files.exists(directory.resolve("mod.jar.bak")));
    }

    @Test
    void refusesToOverwriteAnotherInstallation() throws Exception {
        Path target = Files.writeString(directory.resolve("mod.jar"), "old");
        Path staged = Files.writeString(directory.resolve("download.pending"), "new");
        String original = UpdateInstaller.sha256(target);
        Files.writeString(target, "another update");
        assertThrows(IOException.class, () -> UpdateInstaller.install(target, staged, original, UpdateInstaller.sha256(staged)));
        assertEquals("another update", Files.readString(target));
    }

    @Test
    void missingDownloadDoesNotTouchInstalledFile() throws Exception {
        Path target = Files.writeString(directory.resolve("mod.jar"), "old");
        assertThrows(IOException.class, () -> UpdateInstaller.install(target, directory.resolve("missing"), "", ""));
        assertEquals("old", Files.readString(target));
    }

    @Test
    void helperRunsInSeparateJvmWithoutMinecraftOrFabric() throws Exception {
        Path target = Files.writeString(directory.resolve("mod with spaces.jar"), "old");
        Path staged = Files.writeString(directory.resolve("download.pending"), "new");
        Path helper = directory.resolve("helper.jar");
        String resource = UpdateInstaller.class.getName().replace('.', '/') + ".class";
        try (var output = new JarOutputStream(Files.newOutputStream(helper));
             var input = UpdateInstaller.class.getResourceAsStream("/" + resource)) {
            assertNotNull(input);
            output.putNextEntry(new JarEntry(resource));
            input.transferTo(output);
            output.closeEntry();
        }
        Path java = Path.of(System.getProperty("java.home"), "bin", "java");
        // A short-lived real parent exercises ProcessHandle.onExit before installation.
        Path parentSource = directory.resolve("UpdateTestParent.java");
        Files.writeString(parentSource, "class UpdateTestParent { public static void main(String[] args) throws Exception { Thread.sleep(3000); } }");
        Process parent = new ProcessBuilder(java.toString(), parentSource.toString()).redirectErrorStream(true).start();
        Path log = directory.resolve("installer.log");
        Process process = new ProcessBuilder(java.toString(), "-cp", helper.toString(), UpdateInstaller.class.getName(),
                Long.toString(parent.pid()), target.toString(), staged.toString(),
                UpdateInstaller.sha256(target), UpdateInstaller.sha256(staged))
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            assertFalse(process.waitFor(500, TimeUnit.MILLISECONDS));
            assertEquals("old", Files.readString(target));
            assertTrue(parent.waitFor(20, TimeUnit.SECONDS));
            assertEquals(0, parent.exitValue());
            assertTrue(process.waitFor(20, TimeUnit.SECONDS));
            assertEquals(0, process.exitValue(), Files.readString(log));
            assertEquals("new", Files.readString(target));
            assertEquals("old", Files.readString(directory.resolve("mod with spaces.jar.bak")));
        } finally {
            parent.destroyForcibly();
            process.destroyForcibly();
        }
    }
}
