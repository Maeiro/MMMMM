package com.scs.core;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.nio.file.Path;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for computing file checksums.
 */
public class Checksum {

    private static final Logger LOGGER = LoggerFactory.getLogger(Checksum.class);
    private static final Gson GSON = new Gson();
    private static final HexFormat HEX_FORMAT = HexFormat.of();
    private static final int BUFFER_SIZE = 8192;

    public static final class ChecksumDiff {
        private final List<String> added;
        private final List<String> modified;
        private final List<String> removed;

        private ChecksumDiff(List<String> added, List<String> modified, List<String> removed) {
            this.added = added;
            this.modified = modified;
            this.removed = removed;
        }

        public List<String> getAdded() {
            return added;
        }

        public List<String> getModified() {
            return modified;
        }

        public List<String> getRemoved() {
            return removed;
        }

        public boolean isEmpty() {
            return added.isEmpty() && modified.isEmpty() && removed.isEmpty();
        }

        public int totalChanges() {
            return added.size() + modified.size() + removed.size();
        }
    }

    public static final class ChecksumResult {
        private final ChecksumDiff diff;
        private final Map<String, String> newChecksums;

        private ChecksumResult(ChecksumDiff diff, Map<String, String> newChecksums) {
            this.diff = diff;
            this.newChecksums = newChecksums;
        }

        public ChecksumDiff getDiff() {
            return diff;
        }

        public Map<String, String> getNewChecksums() {
            return newChecksums;
        }
    }

    public interface ProgressListener {
        void onProgress(int current, int total, String fileName);
    }

    /**
     * Computes the SHA-256 checksum of a file.
     *
     * @param filePath The path to the file.
     * @return The computed checksum as a hexadecimal string.
     * @throws Exception If an error occurs while reading the file or computing the checksum.
     */
    public static String computeChecksum(Path filePath) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var in = Files.newInputStream(filePath)) {
            return computeChecksum(in, digest);
        }
    }

    public static String computeChecksum(java.io.InputStream inputStream) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return computeChecksum(inputStream, digest);
    }

    private static String computeChecksum(java.io.InputStream inputStream, MessageDigest digest) throws Exception {
        byte[] buffer = new byte[BUFFER_SIZE];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            digest.update(buffer, 0, read);
        }
        byte[] hashBytes = digest.digest();
        return HEX_FORMAT.formatHex(hashBytes);
    }

    private static String toRelativeKey(Path root, Path file) {
        String relative = root.relativize(file).toString();
        return relative.replace('\\', '/');
    }

    public static void saveChecksums(Path targetDirectory, Path checksumFile) throws Exception {
        saveChecksums(checksumFile, computeChecksums(targetDirectory));
    }

    public static void saveChecksums(Path checksumFile, Map<String, String> checksums) throws Exception {
        Files.writeString(checksumFile, GSON.toJson(checksums));
    }

    public static ChecksumResult compareChecksums(Path targetDirectory, Path checksumFile) throws Exception {
        return compareChecksums(targetDirectory, checksumFile, null);
    }

    public static ChecksumResult compareChecksums(
            Path targetDirectory,
            Path checksumFile,
            ProgressListener listener
    ) throws Exception {
        Map<String, String> oldChecksums = loadChecksums(checksumFile);
        Map<String, String> newChecksums = computeChecksums(targetDirectory, listener);
        ChecksumDiff diff = buildDiff(oldChecksums, newChecksums);
        logDiff(diff);
        return new ChecksumResult(diff, newChecksums);
    }

    public static ChecksumResult compareChecksums(Path checksumFile, Map<String, String> newChecksums) throws Exception {
        Map<String, String> oldChecksums = loadChecksums(checksumFile);
        ChecksumDiff diff = buildDiff(oldChecksums, newChecksums);
        logDiff(diff);
        return new ChecksumResult(diff, newChecksums);
    }

    public static ChecksumResult compareChecksumsForPaths(
            Path targetDirectory,
            Path checksumFile,
            ProgressListener listener,
            java.util.Collection<String> relativePaths
    ) throws Exception {
        Map<String, String> oldChecksums = loadChecksums(checksumFile);
        if (relativePaths == null || relativePaths.isEmpty()) {
            if (listener != null) {
                listener.onProgress(0, 0, "No files found");
            }
            ChecksumDiff diff = buildDiff(Map.of(), Map.of());
            logDiff(diff);
            return new ChecksumResult(diff, Map.of());
        }

        List<String> sorted = new ArrayList<>(relativePaths);
        Collections.sort(sorted, String.CASE_INSENSITIVE_ORDER);

        Map<String, String> filteredOld = oldChecksums.entrySet().stream()
                .filter(entry -> relativePaths.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Map<String, String> newChecksums = new java.util.LinkedHashMap<>();
        int total = sorted.size();
        int current = 0;
        for (String rel : sorted) {
            current++;
            if (listener != null) {
                listener.onProgress(current, total, rel);
            }
            Path resolved = targetDirectory.resolve(rel).normalize();
            if (!resolved.startsWith(targetDirectory) || !Files.isRegularFile(resolved)) {
                continue;
            }
            try {
                newChecksums.put(rel, computeChecksum(resolved));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        ChecksumDiff diff = buildDiff(filteredOld, newChecksums);
        logDiff(diff);
        return new ChecksumResult(diff, newChecksums);
    }

    public static ChecksumResult compareChecksumsFlat(
            Path targetDirectory,
            Path checksumFile,
            ProgressListener listener,
            java.util.function.Predicate<Path> fileFilter
    ) throws Exception {
        Map<String, String> oldChecksums = loadChecksums(checksumFile);
        Map<String, String> filteredOld = oldChecksums.entrySet().stream()
                .filter(entry -> entry.getKey().toLowerCase(Locale.ROOT).endsWith(".jar")
                        && !entry.getKey().contains("/"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        List<Path> files;
        try (var stream = Files.list(targetDirectory)) {
            files = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> fileFilter == null || fileFilter.test(path))
                    .collect(Collectors.toList());
        }

        int total = files.size();
        if (listener != null) {
            if (total == 0) {
                listener.onProgress(0, 0, "No files found");
            } else {
                listener.onProgress(0, total, "Starting...");
            }
        }

        Map<String, String> newChecksums = new java.util.LinkedHashMap<>();
        int current = 0;
        for (Path path : files) {
            current++;
            String key = toRelativeKey(targetDirectory, path);
            if (listener != null) {
                listener.onProgress(current, total, key);
            }
            try {
                newChecksums.put(key, computeChecksum(path));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        ChecksumDiff diff = buildDiff(filteredOld, newChecksums);
        logDiff(diff);
        return new ChecksumResult(diff, newChecksums);
    }

    public static ChecksumDiff filterDiff(ChecksumDiff diff, java.util.Set<String> allowedNames) {
        if (diff == null || allowedNames == null) {
            return diff;
        }

        List<String> added = diff.getAdded().stream()
                .filter(allowedNames::contains)
                .collect(java.util.stream.Collectors.toList());
        List<String> modified = diff.getModified().stream()
                .filter(allowedNames::contains)
                .collect(java.util.stream.Collectors.toList());
        List<String> removed = diff.getRemoved().stream()
                .filter(allowedNames::contains)
                .collect(java.util.stream.Collectors.toList());

        Collections.sort(added, String.CASE_INSENSITIVE_ORDER);
        Collections.sort(modified, String.CASE_INSENSITIVE_ORDER);
        Collections.sort(removed, String.CASE_INSENSITIVE_ORDER);

        return new ChecksumDiff(added, modified, removed);
    }

    private static Map<String, String> loadChecksums(Path checksumFile) throws Exception {
        if (!Files.exists(checksumFile)) {
            return Map.of();
        }

        String content = Files.readString(checksumFile);
        if (content.isBlank()) {
            return Map.of();
        }

        Type type = new TypeToken<Map<String, String>>() {}.getType();
        Map<String, String> checksums = GSON.fromJson(content, type);
        return checksums == null ? Map.of() : checksums;
    }

    private static Map<String, String> computeChecksums(Path targetDirectory, ProgressListener listener) throws Exception {
        if (listener == null) {
            Map<String, String> checksums = new java.util.LinkedHashMap<>();
            try (var stream = Files.walk(targetDirectory)) {
                stream.filter(Files::isRegularFile).forEach(path -> {
                    try {
                        checksums.put(toRelativeKey(targetDirectory, path), computeChecksum(path));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }
            return checksums;
        }

        Map<String, String> checksums = new java.util.LinkedHashMap<>();
        int current = 0;
        try (var stream = Files.walk(targetDirectory)) {
            var iterator = stream.filter(Files::isRegularFile).iterator();
            if (!iterator.hasNext()) {
                listener.onProgress(0, 0, "No files found");
                return checksums;
            }
            listener.onProgress(0, -1, "Starting...");

            while (iterator.hasNext()) {
                Path path = iterator.next();
                current++;
                String key = toRelativeKey(targetDirectory, path);
                listener.onProgress(current, -1, key);
                try {
                    checksums.put(key, computeChecksum(path));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return checksums;
    }

    private static Map<String, String> computeChecksums(Path targetDirectory) throws Exception {
        return computeChecksums(targetDirectory, null);
    }

    private static ChecksumDiff buildDiff(Map<String, String> oldChecksums, Map<String, String> newChecksums) {
        List<String> added = new ArrayList<>();
        List<String> modified = new ArrayList<>();
        List<String> removed = new ArrayList<>();

        for (Map.Entry<String, String> entry : newChecksums.entrySet()) {
            String fileName = entry.getKey();
            String newHash = entry.getValue();
            String oldHash = oldChecksums.get(fileName);
            if (oldHash == null) {
                added.add(fileName);
            } else if (!newHash.equals(oldHash)) {
                modified.add(fileName);
            }
        }

        for (String fileName : oldChecksums.keySet()) {
            if (!newChecksums.containsKey(fileName)) {
                removed.add(fileName);
            }
        }

        Collections.sort(added, String.CASE_INSENSITIVE_ORDER);
        Collections.sort(modified, String.CASE_INSENSITIVE_ORDER);
        Collections.sort(removed, String.CASE_INSENSITIVE_ORDER);

        return new ChecksumDiff(added, modified, removed);
    }

    private static void logDiff(ChecksumDiff diff) {
        for (String fileName : diff.getAdded()) {
            LOGGER.info("Checksum diff (since last snapshot) - Added: {}", fileName);
        }
        for (String fileName : diff.getModified()) {
            LOGGER.info("Checksum diff (since last snapshot) - Modified: {}", fileName);
        }
        for (String fileName : diff.getRemoved()) {
            LOGGER.info("Checksum diff (since last snapshot) - Removed: {}", fileName);
        }
    }
}
