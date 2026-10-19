package com.gtamine.visual.server;

import java.net.IDN;
import java.util.Locale;
import java.util.Set;

/** Checks the user-supplied hostname, never a resolved IP or SRV target. */
public final class ServerDomainPolicy {
    private static final Set<String> DOMAINS = Set.of(
            "gtamine.com", "gtamine.ru", "needmine.ru", "playgo.pro");

    private ServerDomainPolicy() { }

    public static boolean allows(String hostname) {
        if (hostname == null || hostname.isBlank()) {
            return false;
        }
        try {
            String host = IDN.toASCII(hostname, IDN.USE_STD3_ASCII_RULES)
                    .toLowerCase(Locale.ROOT);
            if (host.endsWith(".")) {
                host = host.substring(0, host.length() - 1);
            }
            if (host.length() > 253) {
                return false;
            }
            for (String domain : DOMAINS) {
                if (host.equals(domain) || host.endsWith("." + domain)) {
                    return true;
                }
            }
        } catch (IllegalArgumentException ignored) {
            // Invalid hostnames must never activate visual modules.
        }
        return false;
    }
}
