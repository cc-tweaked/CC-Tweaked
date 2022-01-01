/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.client;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import javax.annotation.Nonnull;

public class ComputerTerminalClientMessage extends ComputerClientMessage
{
    private final TerminalState state;

    public ComputerTerminalClientMessage( int instanceId, TerminalState state )
    {
        super( instanceId );
        this.state = state;
    }

    public ComputerTerminalClientMessage( @Nonnull PacketBuffer buf )
    {
        super( buf );
        state = new TerminalState( buf );
    }

    @Override
    public void toBytes( @Nonnull PacketBuffer buf )
    {
        super.toBytes( buf );
        state.write( buf );
    }

    @Override
    public void handle( NetworkEvent.Context context )
    {
        getComputer().read( state );
    }
}
