package com.gtamine.visual.update;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/** Infrastructure runs at startup, independently of the server-gated gameplay modules. */
public final class AutoUpdater {
    private static final Logger LOGGER = LoggerFactory.getLogger("gtamine_visual/updater");
    private static final String REPOSITORY = "GTAMINEMC/gtamine-visual-mod";
    private static final long MAX_JAR_BYTES = 32L * 1024 * 1024;
    private Path target;
    private Path staged;
    private String oldHash;
    private String newHash;
    private boolean stopping;

    public void start() {
        try {
            initialize();
        } catch (Exception failure) {
            LOGGER.warn("Auto-update unavailable for this installation; keeping current version", failure);
        }
    }

    private void initialize() throws IOException {
        FabricLoader loader = FabricLoader.getInstance();
        if (loader.isDevelopmentEnvironment()) return;
        var mod = loader.getModContainer("gtamine_visual").orElseThrow();
        List<Path> origins = mod.getOrigin().getPaths();
        if (origins.size() != 1 || !Files.isRegularFile(origins.getFirst())
                || !origins.getFirst().getFileName().toString().endsWith(".jar")) {
            LOGGER.info("Auto-update unavailable for this mod installation");
            return;
        }
        target = origins.getFirst().toAbsolutePath().normalize();
        if (Files.isSymbolicLink(target)
                || !target.getParent().toRealPath().equals(loader.getGameDir().resolve("mods").toRealPath())) {
            LOGGER.info("Auto-update requires a standalone JAR directly in the mods directory");
            return;
        }
        Map<String, Version> installed = new HashMap<>();
        loader.getAllMods().forEach(container -> {
            var metadata = container.getMetadata();
            installed.put(metadata.getId(), metadata.getVersion());
            metadata.getProvides().forEach(alias -> installed.putIfAbsent(alias, metadata.getVersion()));
        });
        Version current = mod.getMetadata().getVersion();
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> launchInstaller());
        Thread worker = new Thread(() -> check(current, installed), "GTAMINE Visual update check");
        worker.setDaemon(true);
        worker.start();
    }

    private void check(Version current, Map<String, Version> installed) {
        Path download = null;
        try (HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL).build()) {
            byte[] response = fetch(http, URI.create("https://api.github.com/repos/" + REPOSITORY + "/releases/latest"), 1024 * 1024);
            JsonObject release = JsonParser.parseString(new String(response, StandardCharsets.UTF_8)).getAsJsonObject();
            if (release.get("draft").getAsBoolean() || release.get("prerelease").getAsBoolean()) return;
            Version next = UpdateValidation.releaseVersion(release.get("tag_name").getAsString());
            if (next.compareTo(current) <= 0) return;
            Path directory = FabricLoader.getInstance().getGameDir().resolve(".gtamine-visual-updates");
            Files.createDirectories(directory);
            // Helpers wait at most 24 hours; old failed downloads can then be safely discarded.
            try (var files = Files.list(directory)) {
                long cutoff = System.currentTimeMillis() - Duration.ofDays(7).toMillis();
                for (Path file : files.toList()) {
                    String filename = file.getFileName().toString();
                    if (filename.startsWith("download-")
                            && (filename.endsWith(".jar.pending") || filename.endsWith(".helper.jar"))
                            && Files.isRegularFile(file) && Files.getLastModifiedTime(file).toMillis() < cutoff) {
                        Files.deleteIfExists(file);
                    }
                }
            }
            for (var element : release.getAsJsonArray("assets")) {
                JsonObject asset = element.getAsJsonObject();
                String name = asset.get("name").getAsString();
                if (!name.startsWith("gtamine-visual-") || !name.endsWith(".jar")
                        || name.endsWith("-sources.jar") || name.endsWith("-dev.jar")) continue;
                if (!asset.has("digest") || asset.get("digest").isJsonNull()) continue;
                String digest = asset.get("digest").getAsString();
                if (!digest.matches("sha256:[a-fA-F0-9]{64}")) continue;
                long size = asset.get("size").getAsLong();
                if (size <= 0 || size > MAX_JAR_BYTES) continue;
                URI uri = URI.create(asset.get("browser_download_url").getAsString());
                if (!"https".equals(uri.getScheme()) || !"github.com".equals(uri.getHost())
                        || uri.getPort() != -1 || uri.getUserInfo() != null
                        || !uri.getPath().startsWith("/" + REPOSITORY + "/releases/download/")) continue;
                download = Files.createTempFile(directory, "download-", ".jar.pending");
                try {
                    String original = UpdateInstaller.sha256(target);
                    byte[] bytes = fetch(http, uri, MAX_JAR_BYTES);
                    if (bytes.length != size) throw new IOException("Unexpected download size");
                    Files.write(download, bytes);
                    String checksum = digest.substring(7).toLowerCase(Locale.ROOT);
                    if (!checksum.equals(UpdateInstaller.sha256(download))) throw new IOException("SHA-256 mismatch");
                    UpdateValidation.validateJar(download, next, installed);
                    // Other mods may in turn require a particular version of GTAMINE Visual.
                    for (var container : FabricLoader.getInstance().getAllMods()) {
                        for (var dependency : container.getMetadata().getDependencies()) {
                            if (!dependency.getModId().equals("gtamine_visual")) continue;
                            var kind = dependency.getKind();
                            if ((kind == net.fabricmc.loader.api.metadata.ModDependency.Kind.DEPENDS && !dependency.matches(next))
                                    || (kind == net.fabricmc.loader.api.metadata.ModDependency.Kind.BREAKS && dependency.matches(next))) {
                                throw new IOException("Installed mod rejects update: " + container.getMetadata().getId());
                            }
                        }
                    }
                    synchronized (this) {
                        if (stopping) return;
                        staged = download;
                        oldHash = original;
                        newHash = checksum;
                        download = null;
                    }
                    LOGGER.info("GTAMINE Visual {} downloaded; it will be installed after the game exits", next.getFriendlyString());
                    MinecraftClient client = MinecraftClient.getInstance();
                    client.execute(() -> SystemToast.add(client.getToastManager(), SystemToast.Type.PERIODIC_NOTIFICATION,
                            Text.literal("GTAMINE Visual " + next.getFriendlyString()),
                            Text.literal("Обновление скачано. Установится после выхода из игры.")));
                    return;
                } catch (Exception incompatible) {
                    LOGGER.warn("Skipping update asset {}: {}", name, incompatible.toString());
                    Files.deleteIfExists(download);
                    download = null;
                }
            }
        } catch (Exception failure) {
            LOGGER.warn("Update check unavailable; keeping current version: {}", failure.toString());
        } finally {
            if (download != null) {
                try { Files.deleteIfExists(download); } catch (IOException ignored) { }
            }
        }
    }

    private static byte[] fetch(HttpClient http, URI uri, long limit) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(30))
                .header("User-Agent", "GTAMINE-Visual-Updater")
                .header("Accept", uri.getHost().equals("api.github.com") ? "application/vnd.github+json" : "application/octet-stream")
                .GET().build();
        var response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
        // The request timeout covers headers; explicitly bound the streaming body too.
        try (var input = response.body(); var timer = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "GTAMINE update timeout");
            thread.setDaemon(true);
            return thread;
        })) {
            var timeout = timer.schedule(() -> { try { input.close(); } catch (IOException ignored) { } }, 60, TimeUnit.SECONDS);
            try {
                if (response.statusCode() != 200) throw new IOException("GitHub HTTP " + response.statusCode());
                byte[] bytes = input.readNBytes(Math.toIntExact(limit + 1));
                if (bytes.length > limit) throw new IOException("Response too large");
                return bytes;
            } finally { timeout.cancel(false); }
        }
    }

    private synchronized void launchInstaller() {
        stopping = true;
        if (staged == null) return;
        try {
            Path helper = staged.resolveSibling(staged.getFileName() + ".helper.jar");
            String resource = UpdateInstaller.class.getName().replace('.', '/') + ".class";
            try (var output = new JarOutputStream(Files.newOutputStream(helper));
                 var input = UpdateInstaller.class.getResourceAsStream("/" + resource)) {
                if (input == null) throw new IOException("Missing update helper");
                output.putNextEntry(new JarEntry(resource));
                input.transferTo(output);
                output.closeEntry();
            }
            boolean windows = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
            Path java = Path.of(System.getProperty("java.home"), "bin", windows ? "javaw.exe" : "java");
            if (!Files.isRegularFile(java)) java = Path.of(System.getProperty("java.home"), "bin", windows ? "java.exe" : "java");
            new ProcessBuilder(java.toString(), "-cp", helper.toString(), UpdateInstaller.class.getName(),
                    Long.toString(ProcessHandle.current().pid()), target.toString(), staged.toString(), oldHash, newHash)
                    .redirectOutput(ProcessBuilder.Redirect.appendTo(staged.getParent().resolve("installer.log").toFile()))
                    .redirectErrorStream(true).start();
        } catch (Exception failure) {
            LOGGER.warn("Could not start update installer; keeping current version", failure);
        }
    }
}
