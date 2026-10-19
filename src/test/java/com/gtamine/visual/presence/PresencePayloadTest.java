package com.gtamine.visual.presence;

import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.DynamicRegistryManager;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PresencePayloadTest {
    @Test void discardsLegacyMessagesAndDoesNotEncodeAnyData() {
        var buffer = new RegistryByteBuf(Unpooled.buffer(), DynamicRegistryManager.EMPTY);
        try {
            buffer.writeZero(4096);
            var marker = PresencePayload.CODEC.decode(buffer);
            assertEquals(0, buffer.readableBytes());
            buffer.clear();
            PresencePayload.CODEC.encode(buffer, marker);
            assertEquals(0, buffer.readableBytes());
            assertEquals("gtamine:visual", marker.getId().id().toString());
        } finally {
            buffer.release();
        }
    }
}
