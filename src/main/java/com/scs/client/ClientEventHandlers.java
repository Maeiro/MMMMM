package com.scs.client;

import com.scs.core.SCS;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent.Init.Post;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EventBusSubscriber(modid = SCS.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class ClientEventHandlers {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientEventHandlers.class);
    private static final int BUTTON_WIDTH = 80;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_X_OFFSET = 125;
    private static final int BUTTON_Y_START = 50;
    private static final int BUTTON_SPACING = 36;
    private static final int BOTTOM_MARGIN = 50;

    private ClientEventHandlers() {
    }

    @SubscribeEvent
    public static void onMultiplayerScreenInit(Post event) {
        if (!(event.getScreen() instanceof JoinMultiplayerScreen screen)) {
            return;
        }

        LOGGER.info("Multiplayer screen initialized. Adding server buttons.");
        ServerList serverList = new ServerList(Minecraft.getInstance());
        serverList.load();

        int buttonX = screen.width - BUTTON_X_OFFSET;
        int maxHeight = screen.height - BOTTOM_MARGIN;
        boolean metadataUpdated = false;

        for (int i = 0; i < serverList.size(); i++) {
            int y = BUTTON_Y_START + (i * BUTTON_SPACING);
            if (y + BUTTON_HEIGHT > maxHeight) {
                break;
            }

            ServerData server = serverList.get(i);
            if (ServerMetadata.setDefaultIfMissing(server.ip)) {
                metadataUpdated = true;
            }
            event.addListener(createUpdateButton(buttonX, y, server, screen));
        }

        if (metadataUpdated) {
            ServerMetadata.saveMetadata();
        }
    }

    private static Button createUpdateButton(int x, int y, ServerData server, JoinMultiplayerScreen returnScreen) {
        return Button.builder(
                Component.translatable("gui.scs.update"),
                button -> {
                    String updateBaseUrl = ServerMetadata.getMetadata(server.ip);
                    LOGGER.info("Update button clicked for server: {}", updateBaseUrl);
                    Minecraft.getInstance().setScreen(new UpdateActionScreen(returnScreen, server.ip, updateBaseUrl));
                }
        ).bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT).build();
    }
}