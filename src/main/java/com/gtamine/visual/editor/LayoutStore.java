package com.gtamine.visual.editor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.LoggerFactory;

/** Stores normalized offsets. Written after a drag or an explicit layout reset. */
public final class LayoutStore {
    public record Offset(double x, double y) {
        public boolean valid() { return Double.isFinite(x) && Double.isFinite(y) && Math.abs(x) <= 2 && Math.abs(y) <= 2; }
    }
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    private final Map<String, Offset> offsets = new HashMap<>();

    public LayoutStore(Path file) {
        this.file = file;
        if (Files.isRegularFile(file)) {
            try (var reader = Files.newBufferedReader(file)) {
                Map<String, Offset> loaded = GSON.fromJson(reader, new TypeToken<Map<String, Offset>>() { }.getType());
                if (loaded != null) loaded.forEach((key, value) -> {
                    if (key != null && value != null && value.valid()) offsets.put(key, value);
                });
            } catch (IOException | RuntimeException ex) {
                LoggerFactory.getLogger("gtamine_visual").warn("Cannot read HUD layout; keeping original file", ex);
            }
        }
    }
    public Offset get(String key) { return offsets.getOrDefault(key, new Offset(0, 0)); }
    public boolean hasProfile(String prefix) {
        return offsets.entrySet().stream().anyMatch(entry -> entry.getKey().startsWith(prefix + "/")
                && (entry.getValue().x() != 0 || entry.getValue().y() != 0));
    }
    public void put(String key, Offset offset) { if (offset.valid()) offsets.put(key, offset); }
    public void resetAll() {
        offsets.clear();
        save();
    }

    public void save() {
        try {
            Files.createDirectories(file.getParent());
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(temporary, GSON.toJson(offsets));
            try {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            LoggerFactory.getLogger("gtamine_visual").warn("Cannot save HUD layout", ex);
        }
    }
}
