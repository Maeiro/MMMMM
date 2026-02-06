package com.scs.server;

import com.scs.core.SCS;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Handles server-side events.
 */
@EventBusSubscriber(modid = SCS.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.DEDICATED_SERVER)
public class ServerEventHandlers {

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        try {
            SCS.LOGGER.info("Performing common setup tasks.");
            FileHostingServer.start();
        } catch (Exception e) {
            SCS.LOGGER.error("Failed to start file hosting server: ", e);
        }
    }
}
