package com.gtamine.visual.presence;

import com.gtamine.visual.module.VisualModule;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;

/** Advertises a listening channel; no requests, replies or version exchange. */
public final class PresenceModule implements VisualModule {
    private ClientPlayNetworkHandler session;

    public PresenceModule() {
        PayloadTypeRegistry.playS2C().register(PresencePayload.ID, PresencePayload.CODEC);
    }

    @Override public String id() { return "plugin_presence"; }

    @Override public void enable() {
        session = MinecraftClient.getInstance().getNetworkHandler();
        if (session != null) {
            // Fabric announces receiver registration through minecraft:register.
            ClientPlayNetworking.registerReceiver(PresencePayload.ID, (payload, context) -> { });
        }
    }

    @Override public void disable() {
        if (session != null && MinecraftClient.getInstance().getNetworkHandler() == session) {
            ClientPlayNetworking.unregisterReceiver(PresencePayload.ID.id());
        }
        session = null;
    }
}
