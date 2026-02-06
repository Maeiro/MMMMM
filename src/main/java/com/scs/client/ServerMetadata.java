package com.scs.client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.scs.core.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ServerMetadata {
    private static final File METADATA_FILE = new File("SCS/server_metadata.json");
    private static final Gson GSON = new Gson();
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerMetadata.class);
    private static final Type METADATA_TYPE = new TypeToken<Map<String, String>>() {}.getType();
    private static Map<String, String> serverMetadata = new HashMap<>();

    static {
        loadMetadata();
    }

    private static void loadMetadata() {
        if (METADATA_FILE.exists()) {
            try (FileReader reader = new FileReader(METADATA_FILE)) {
                Map<String, String> loadedData = GSON.fromJson(reader, METADATA_TYPE);
                if (loadedData != null) {
                    serverMetadata = loadedData;
                }
                LOGGER.info("Metadata loaded successfully.");
            } catch (Exception e) {
                LOGGER.error("Failed to load metadata.", e);
            }
        } else {
            LOGGER.warn("Metadata file does not exist. Starting with an empty metadata map.");
        }
    }

    public static void saveMetadata() {
        try {
            File parentDir = METADATA_FILE.getParentFile();
            if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                LOGGER.error("Failed to create metadata directory: {}", parentDir.getAbsolutePath());
                return;
            }

            try (FileWriter writer = new FileWriter(METADATA_FILE)) {
                GSON.toJson(serverMetadata, writer);
                LOGGER.info("Metadata saved successfully to {}", METADATA_FILE.getAbsolutePath());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to save metadata.", e);
        }
    }

    public static String getMetadata(String serverIP) {
        String stored = serverMetadata.getOrDefault(serverIP, "");
        if (stored != null && !stored.isBlank()) {
            return stored;
        }
        return buildDefaultUrl(serverIP);
    }

    public static boolean setDefaultIfMissing(String serverIP) {
        if (!isValidServerIP(serverIP)) {
            return false;
        }

        String stored = serverMetadata.getOrDefault(serverIP, "");
        if (stored != null && !stored.isBlank()) {
            return false;
        }

        String defaultUrl = buildDefaultUrl(serverIP);
        if (defaultUrl.isBlank()) {
            return false;
        }

        serverMetadata.put(serverIP, defaultUrl);
        return true;
    }

    public static boolean removeMetadata(String serverIP) {
        if (!isValidServerIP(serverIP)) {
            return false;
        }
        if (serverMetadata.remove(serverIP) != null) {
            saveMetadata();
            LOGGER.info("Metadata removed for server: {}", serverIP);
            return true;
        }
        return false;
    }

    public static void setMetadata(String serverIP, String value) {
        if (isValidServerIP(serverIP) && isValidMetadataValue(value)) {
            serverMetadata.put(serverIP, value);
            saveMetadata();
            LOGGER.info("Metadata updated for server: {}", serverIP);
        } else {
            LOGGER.warn("Invalid server IP or metadata value. Skipping update.");
        }
    }

    private static boolean isValidServerIP(String serverIP) {
        return serverIP != null && !serverIP.isBlank();
    }

    private static boolean isValidMetadataValue(String value) {
        return value != null && !value.isBlank();
    }

    public static Map<String, String> getAllMetadata() {
        return Collections.unmodifiableMap(serverMetadata);
    }

    private static String buildDefaultUrl(String serverIP) {
        if (!isValidServerIP(serverIP)) {
            return "";
        }

        String host = extractHost(serverIP.trim());
        if (host.isBlank()) {
            return "";
        }

        int port = Config.fileServerPort > 0 ? Config.fileServerPort : 25566;
        String hostForUrl = host;
        if (hostForUrl.contains(":") && !(hostForUrl.startsWith("[") && hostForUrl.endsWith("]"))) {
            hostForUrl = "[" + hostForUrl + "]";
        }
        return "http://" + hostForUrl + ":" + port;
    }

    private static String extractHost(String serverIP) {
        String trimmed = serverIP.trim();
        if (trimmed.isBlank()) {
            return "";
        }

        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            try {
                URI uri = URI.create(trimmed);
                String host = uri.getHost();
                if (host != null && !host.isBlank()) {
                    return host;
                }
            } catch (Exception ignored) {
            }
        }

        if (trimmed.startsWith("[") && trimmed.contains("]")) {
            int end = trimmed.indexOf(']');
            if (end > 1) {
                return trimmed.substring(1, end);
            }
        }

        int colonCount = 0;
        for (int i = 0; i < trimmed.length(); i++) {
            if (trimmed.charAt(i) == ':') {
                colonCount++;
            }
        }

        if (colonCount == 1) {
            int lastColon = trimmed.lastIndexOf(':');
            String portPart = trimmed.substring(lastColon + 1);
            if (!portPart.isBlank() && portPart.chars().allMatch(Character::isDigit)) {
                return trimmed.substring(0, lastColon);
            }
        } else if (colonCount > 1) {
            return trimmed;
        }

        return trimmed;
    }
}