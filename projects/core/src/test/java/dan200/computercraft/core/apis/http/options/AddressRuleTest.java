// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.http.options;

import dan200.computercraft.core.CoreConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AddressRuleTest {
    @Test
    public void matchesPort() {
        Iterable<AddressRule> rules = Collections.singletonList(AddressRule.parse(
            "127.0.0.1", OptionalInt.of(8080),
            Action.ALLOW.toPartial()
        ));

        assertEquals(apply(rules, "localhost", 8080).action, Action.ALLOW);
        assertEquals(apply(rules, "localhost", 8081).action, Action.DENY);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "0.0.0.0", "[::]",
        "localhost", "127.0.0.1.nip.io", "127.0.0.1", "[::1]",
        "172.17.0.1", "192.168.1.114", "[0:0:0:0:0:ffff:c0a8:172]", "10.0.0.1"
    })
    public void blocksLocalDomains(String domain) {
        assertEquals(apply(CoreConfig.httpRules, domain, 80).action, Action.DENY);
    }

    private Options apply(Iterable<AddressRule> rules, String host, int port) {
        return AddressRule.apply(rules, host, new InetSocketAddress(host, port));
    }
}
