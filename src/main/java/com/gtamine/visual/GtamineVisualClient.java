package com.gtamine.visual;

import com.gtamine.visual.module.ModuleManager;
import com.gtamine.visual.editor.InterfaceEditorModule;
import com.gtamine.visual.presence.PresenceModule;
import com.gtamine.visual.server.ServerDomainPolicy;
import com.gtamine.visual.update.AutoUpdater;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.network.ServerAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public final class GtamineVisualClient implements ClientModInitializer {
    public static final String MOD_ID = "gtamine_visual";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        new AutoUpdater().start();
        ModuleManager modules = new ModuleManager(List.of(new InterfaceEditorModule(), new PresenceModule()));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ServerInfo server = client.getCurrentServerEntry();
            boolean allowed = !client.isInSingleplayer() && server != null
                    && ServerDomainPolicy.allows(ServerAddress.parse(server.address).getAddress());
            modules.setActive(allowed);
            LOGGER.debug("GTAMINE Visual session active: {}", allowed);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> modules.setActive(false));
        LOGGER.info("GTAMINE Visual initialized");
    }
}
