package com.scs.client;

import com.scs.client.update.UpdateCoordinator;
import com.scs.core.SCS;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
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
    private static final int BUTTON_WIDTH = 70;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_X_OFFSET = 75;
    private static final int BUTTON_Y_START = 50;
    private static final int BUTTON_SPACING = 48;
    private static final int SECOND_BUTTON_OFFSET = BUTTON_HEIGHT + 4;
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

        for (int i = 0; i < serverList.size(); i++) {
            int y = BUTTON_Y_START + (i * BUTTON_SPACING);
            if (y + SECOND_BUTTON_OFFSET + BUTTON_HEIGHT > maxHeight) {
                break;
            }

            ServerData server = serverList.get(i);
            event.addListener(createUpdateButton(buttonX, y, server));
            event.addListener(createClearCacheButton(buttonX, y + SECOND_BUTTON_OFFSET, screen));
        }
    }

    private static Button createUpdateButton(int x, int y, ServerData server) {
        return Button.builder(
                Component.literal("Update"),
                button -> {
                    String updateBaseUrl = ServerMetadata.getMetadata(server.ip);
                    LOGGER.info("Update button clicked for server: {}", updateBaseUrl);
                    UpdateCoordinator.startUpdate(updateBaseUrl);
                }
        ).bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT).build();
    }

    private static Button createClearCacheButton(int x, int y, JoinMultiplayerScreen returnScreen) {
        return Button.builder(
                Component.literal("Clear cache"),
                button -> {
                    Minecraft minecraft = Minecraft.getInstance();
                    ConfirmScreen confirmScreen = new ConfirmScreen(
                            confirmed -> {
                                if (confirmed) {
                                    UpdateCoordinator.clearCache(returnScreen);
                                } else {
                                    minecraft.setScreen(returnScreen);
                                }
                            },
                            Component.literal("Clear SCS cache?"),
                            Component.literal("This will delete shared-files and checksum cache files."),
                            Component.literal("Clear"),
                            Component.literal("Cancel")
                    );
                    minecraft.setScreen(confirmScreen);
                }
        ).bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT).build();
    }
}
