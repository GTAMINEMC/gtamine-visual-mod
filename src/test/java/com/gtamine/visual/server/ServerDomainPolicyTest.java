package com.gtamine.visual.server;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class ServerDomainPolicyTest {
    @ParameterizedTest
    @ValueSource(strings = {"gtamine.com", "gtamine.ru", "needmine.ru", "playgo.pro",
            "mc.gtamine.com", "a.b.needmine.ru", "MC.GTAMINE.RU", "playgo.pro."})
    void allowsApprovedDomainsAndSubdomains(String host) {
        assertTrue(ServerDomainPolicy.allows(host));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "localhost", "127.0.0.1", "::1", "fakegtamine.com",
            "gtamine.com.example.org", "mc.gtamine.com.evil.ru", "https://gtamine.com",
            "gtamine.com:25565", ".gtamine.com", "a..gtamine.com", "bad_host.gtamine.com"})
    void rejectsOtherAndMalformedHosts(String host) {
        assertFalse(ServerDomainPolicy.allows(host));
    }
}
