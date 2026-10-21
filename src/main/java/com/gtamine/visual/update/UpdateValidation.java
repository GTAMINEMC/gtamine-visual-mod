package com.gtamine.visual.update;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.jar.JarFile;

final class UpdateValidation {
    private UpdateValidation() { }

    static Version releaseVersion(String tag) throws Exception {
        String version = tag.startsWith("v") ? tag.substring(1) : tag;
        // Only numbered stable releases; never install an arbitrary string or prerelease.
        if (!version.matches("[0-9]+\\.[0-9]+\\.[0-9]+")) throw new IOException("Not a stable version: " + tag);
        return Version.parse(version);
    }

    static void validateJar(Path path, Version expected, Map<String, Version> installed) throws Exception {
        try (JarFile jar = new JarFile(path.toFile())) {
            var entry = jar.getJarEntry("fabric.mod.json");
            if (entry == null) throw new IOException("Missing Fabric metadata");
            JsonObject metadata;
            try (var input = jar.getInputStream(entry)) {
                byte[] bytes = input.readNBytes(65537);
                if (bytes.length > 65536) throw new IOException("Fabric metadata too large");
                metadata = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
            }
            if (!"gtamine_visual".equals(metadata.get("id").getAsString())
                    || Version.parse(metadata.get("version").getAsString()).compareTo(expected) != 0
                    || !"client".equals(metadata.get("environment").getAsString())) {
                throw new IOException("Wrong mod, version or environment");
            }
            JsonObject depends = metadata.getAsJsonObject("depends");
            for (String required : new String[]{"minecraft", "fabricloader", "java", "fabric-api"}) {
                if (!depends.has(required)) throw new IOException("Missing compatibility declaration: " + required);
            }
            for (var dependency : depends.entrySet()) {
                Version present = installed.get(dependency.getKey());
                if (present == null || !matches(dependency.getValue(), present)) {
                    throw new IOException("Incompatible dependency: " + dependency.getKey());
                }
            }
            if (metadata.has("breaks")) {
                for (var conflict : metadata.getAsJsonObject("breaks").entrySet()) {
                    Version present = installed.get(conflict.getKey());
                    if (present != null && matches(conflict.getValue(), present)) {
                        throw new IOException("Conflicting mod: " + conflict.getKey());
                    }
                }
            }
        }
    }

    private static boolean matches(JsonElement constraint, Version installed) throws Exception {
        if (constraint.isJsonArray()) {
            for (JsonElement alternative : constraint.getAsJsonArray()) {
                if (VersionPredicate.parse(alternative.getAsString()).test(installed)) return true;
            }
            return false;
        }
        return VersionPredicate.parse(constraint.getAsString()).test(installed);
    }
}
