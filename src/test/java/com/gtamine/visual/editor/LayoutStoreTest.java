package com.gtamine.visual.editor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class LayoutStoreTest {
    @TempDir Path directory;
    @Test void persistsLocalOffsetsAndKeepsProfilesSeparate() {
        Path file = directory.resolve("editor/layout.json");
        var store = new LayoutStore(file);
        store.put("pack-a/block", new LayoutStore.Offset(0.25, -0.1));
        store.save();
        var reloaded = new LayoutStore(file);
        assertEquals(new LayoutStore.Offset(0.25, -0.1), reloaded.get("pack-a/block"));
        assertEquals(new LayoutStore.Offset(0, 0), reloaded.get("pack-b/block"));
    }
    @Test void ignoresInvalidPositions() throws Exception {
        Path file = directory.resolve("layout.json");
        Files.writeString(file, "{\"invalid\":{\"x\":1e100,\"y\":0},\"valid\":{\"x\":0.1,\"y\":0.2}}");
        var store = new LayoutStore(file);
        assertEquals(new LayoutStore.Offset(0, 0), store.get("invalid"));
        assertEquals(new LayoutStore.Offset(0.1, 0.2), store.get("valid"));
    }

    @Test void resetsAllHudAndMinimapProfilesAndPersistsReset() {
        Path file = directory.resolve("layout.json");
        var store = new LayoutStore(file);
        store.put("pack-a/block", new LayoutStore.Offset(0.25, -0.1));
        store.put("pack-b/block", new LayoutStore.Offset(-0.2, 0.3));
        store.put("nminimap/profile", new LayoutStore.Offset(0.4, 0.2));
        store.save();
        store.resetAll();
        var reloaded = new LayoutStore(file);
        for (String key : new String[]{"pack-a/block", "pack-b/block", "nminimap/profile"}) {
            assertEquals(new LayoutStore.Offset(0, 0), store.get(key));
            assertEquals(new LayoutStore.Offset(0, 0), reloaded.get(key));
        }
        assertFalse(reloaded.hasProfile("pack-a"));
        assertFalse(reloaded.hasProfile("nminimap"));
    }
}
