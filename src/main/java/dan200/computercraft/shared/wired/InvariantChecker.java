/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.wired;

import dan200.computercraft.ComputerCraft;

/**
 * Verifies certain elements of a network are "well formed".
 *
 * This adds substantial overhead to network modification, and so should only be enabled
 * in a development environment.
 */
public final class InvariantChecker
{
    private static final boolean ENABLED = false;

    private InvariantChecker() {}

    public static void checkNode( WiredNode node )
    {
        if( !ENABLED ) return;

        WiredNetwork network = node.network;
        if( network == null )
        {
            ComputerCraft.log.error( "Node's network is null", new Exception() );
            return;
        }

        if( network.nodes == null || !network.nodes.contains( node ) )
        {
            ComputerCraft.log.error( "Node's network does not contain node", new Exception() );
        }

        for( WiredNode neighbour : node.neighbours )
        {
            if( !neighbour.neighbours.contains( node ) )
            {
                ComputerCraft.log.error( "Neighbour is missing node", new Exception() );
            }
        }
    }

    public static void checkNetwork( WiredNetwork network )
    {
        if( !ENABLED ) return;

        for( WiredNode node : network.nodes ) checkNode( node );
    }
}
