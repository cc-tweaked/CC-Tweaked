/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.container;

import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.network.client.TerminalState;
import net.minecraft.network.PacketBuffer;

public class ComputerContainerData implements ContainerData
{
    private final ComputerFamily family;
    private final TerminalState terminal;

    public ComputerContainerData( ServerComputer computer )
    {
        family = computer.getFamily();
        terminal = computer.getTerminalState();
    }

    public ComputerContainerData( PacketBuffer buf )
    {
        family = buf.readEnum( ComputerFamily.class );
        terminal = new TerminalState( buf );
    }

    @Override
    public void toBytes( PacketBuffer buf )
    {
        buf.writeEnum( family );
        terminal.write( buf );
    }

    public ComputerFamily family()
    {
        return family;
    }

    public TerminalState terminal()
    {
        return terminal;
    }
}
