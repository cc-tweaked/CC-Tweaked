// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.http;

import dan200.computercraft.test.core.ReplaceUnderscoresDisplayNameGenerator;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayNameGeneration(ReplaceUnderscoresDisplayNameGenerator.class)
class NetworkUtilsTest {
    @Test
    public void test_getAddress_with_scoped_address() {
        var err = assertThrows(HTTPRequestException.class, () -> NetworkUtils.getAddress("[::1%1]", 80, false));
        assertEquals("Scoped address not permitted", err.getMessage());
    }
}
