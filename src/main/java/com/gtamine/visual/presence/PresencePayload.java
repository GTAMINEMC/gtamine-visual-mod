package com.gtamine.visual.presence;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Marker channel only: unsolicited message bodies are discarded without allocation. */
public record PresencePayload() implements CustomPayload {
    public static final Id<PresencePayload> ID = new Id<>(Identifier.of("gtamine", "visual"));
    private static final PresencePayload INSTANCE = new PresencePayload();
    public static final PacketCodec<RegistryByteBuf, PresencePayload> CODEC = new PacketCodec<>() {
        @Override public PresencePayload decode(RegistryByteBuf buffer) {
            buffer.skipBytes(buffer.readableBytes());
            return INSTANCE;
        }
        @Override public void encode(RegistryByteBuf buffer, PresencePayload payload) { }
    };
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
