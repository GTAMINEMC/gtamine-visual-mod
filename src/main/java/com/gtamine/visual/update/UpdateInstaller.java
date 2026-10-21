package com.gtamine.visual.update;

import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;

/** Standalone JDK-only helper: never loads Minecraft or the running mod JAR. */
public final class UpdateInstaller {
    private UpdateInstaller() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 5) throw new IllegalArgumentException("Expected parent, target, staged, old hash, new hash");
        ProcessHandle parent = ProcessHandle.of(Long.parseLong(args[0])).orElse(null);
        if (parent != null) parent.onExit().get(24, TimeUnit.HOURS);
        // Serialise helpers from multiple game instances using the same installed file.
        Path target = Path.of(args[1]);
        Path lockPath = target.resolveSibling(target.getFileName() + ".update-lock");
        try (FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             var lock = channel.lock()) {
            for (int attempt = 0; ; attempt++) {
                try {
                    install(target, Path.of(args[2]), args[3], args[4]);
                    System.out.println("GTAMINE Visual update installed");
                    return;
                } catch (java.nio.file.FileSystemException locked) {
                    if (attempt >= 59) throw locked;
                    Thread.sleep(1000);
                }
            }
        }
    }

    static void install(Path target, Path staged, String oldHash, String newHash) throws Exception {
        if (!Files.isRegularFile(target) || !Files.isRegularFile(staged)) {
            throw new java.io.IOException("Missing installed or downloaded JAR");
        }
        if (!sha256(target).equals(oldHash) || !sha256(staged).equals(newHash)) {
            throw new java.io.IOException("JAR changed since verification; refusing replacement");
        }
        // Keep a complete, durable backup outside the *.jar discovery pattern.
        Path backup = target.resolveSibling(target.getFileName() + ".bak");
        Files.copy(target, backup, StandardCopyOption.REPLACE_EXISTING);
        try (FileChannel channel = FileChannel.open(backup, StandardOpenOption.WRITE)) { channel.force(true); }
        // Stage alongside the target so replacement is on the same filesystem.
        Path replacement = Files.createTempFile(target.getParent(), ".gtamine-update-", ".tmp");
        try {
            Files.copy(staged, replacement, StandardCopyOption.REPLACE_EXISTING);
            try (FileChannel channel = FileChannel.open(replacement, StandardOpenOption.WRITE)) { channel.force(true); }
            // No non-atomic fallback: an interrupted installation must leave a complete JAR.
            Files.move(replacement, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            // Installation has committed; a cleanup failure must not report a failed replacement.
            try { Files.deleteIfExists(staged); } catch (java.io.IOException ignored) { }
        } finally {
            Files.deleteIfExists(replacement);
        }
    }

    public static String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var input = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
        }
        return HexFormat.of().formatHex(digest.digest());
    }
}
