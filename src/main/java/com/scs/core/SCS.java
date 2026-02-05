package com.scs.core;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Main mod class for ExampleMod.
 */
@Mod(SCS.MODID)
public class SCS {

    public static final String MODID = "scs";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SCS(ModContainer modContainer) {
        LOGGER.info("Initializing SCS...");

        // Register configuration
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        LOGGER.info("SCS initialized.");

        NeoForge.EVENT_BUS.addListener(RegisterCommands::onRegisterCommands);
    }
}
