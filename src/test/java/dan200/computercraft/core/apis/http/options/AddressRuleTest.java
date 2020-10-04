/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.apis.http.options;

import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AddressRuleTest
{
    @Test
    public void matchesPort()
    {
        Iterable<AddressRule> rules = Collections.singletonList( AddressRule.parse(
            "127.0.0.1", 8080,
            new PartialOptions( Action.ALLOW, null, null, null, null )
        ) );

        assertEquals( apply( rules, "localhost", 8080 ).action, Action.ALLOW );
        assertEquals( apply( rules, "localhost", 8081 ).action, Action.DENY );
    }

    private Options apply( Iterable<AddressRule> rules, String host, int port )
    {
        return AddressRule.apply( rules, host, new InetSocketAddress( host, port ) );
    }
}
