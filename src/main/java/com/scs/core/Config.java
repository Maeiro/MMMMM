package com.scs.core;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Config class to handle mod settings and updates.
 */
@EventBusSubscriber(modid = SCS.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.ConfigValue<Integer> FILE_SERVER_PORT = BUILDER
            .comment(
                    "Port number for the file server to run on",
                    "Default: 25566"
            )
            .define("fileServerPort", 25566);
    private static final ModConfigSpec.ConfigValue<Boolean> UPDATE_CONFIG = BUILDER
            .comment(
                    "If true, the client will also update the config folder when pressing the update button.",
                    "This downloads config.zip from the server and extracts it into /config.",
                    "Default: true"
            )
            .define("updateConfig", true);

    private static final ModConfigSpec.ConfigValue<Boolean> MIRROR_MODS = BUILDER
            .comment(
                    "If true, the client mods folder will be mirrored to mods.zip.",
                    "Any mod jar not present in mods.zip will be removed during update.",
                    "Default: false"
            )
            .define("mirrorMods", false);

    private static final ModConfigSpec.ConfigValue<Boolean> MIRROR_CONFIG = BUILDER
            .comment(
                    "If true, the client config folder will be mirrored to config.zip.",
                    "Any config file not present in config.zip will be removed during update.",
                    "Default: false"
            )
            .define("mirrorConfig", false);

    /**
     * Compile the final specification.
     */
    static final ModConfigSpec SPEC = BUILDER.build();

    public static int fileServerPort;

    public static boolean updateConfig;
    public static boolean mirrorMods;
    public static boolean mirrorConfig;

    /**
     * Called when the configuration is loaded or updated. This ensures runtime
     * variables always hold accurate, current values.
     */
    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        // Ensure the correct config type (COMMON) is loaded.
        if (!event.getConfig().getSpec().equals(SPEC)) {
            return;
        }

        // Update static values with configuration values
        fileServerPort = FILE_SERVER_PORT.get();

        updateConfig = UPDATE_CONFIG.get();
        mirrorMods = MIRROR_MODS.get();
        mirrorConfig = MIRROR_CONFIG.get();

        // Log configuration load
        SCS.LOGGER.info("Configuration loaded:");
        SCS.LOGGER.info("File Server Port: {}", fileServerPort);
        SCS.LOGGER.info("Update Config: {}", updateConfig);
        SCS.LOGGER.info("Mirror Mods: {}", mirrorMods);
        SCS.LOGGER.info("Mirror Config: {}", mirrorConfig);

        if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) {
            try {
                com.scs.server.FileHostingServer.restartIfPortChanged();
            } catch (Exception e) {
                SCS.LOGGER.error("Failed to apply file server config changes.", e);
            }
        }
    }
}
