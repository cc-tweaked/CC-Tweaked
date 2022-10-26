/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.pocket;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.computer.core.ComputerState;
import dan200.computercraft.shared.network.client.TerminalState;
import dan200.computercraft.shared.pocket.core.PocketServerComputer;

import javax.annotation.Nonnull;

/**
 * Clientside data about a pocket computer.
 * <p>
 * Normal computers don't store any state long-term on the client - everything is tied to the container and only synced
 * while the UI is open. Pocket computers are a little more complex, as their on/off state is visible on the item's
 * texture, and the terminal can be viewed at any time. This class is what holds this needed data clientside.
 *
 * @see ClientPocketComputers The registry which holds pocket computers.
 * @see PocketServerComputer The server-side pocket computer.
 */
public class PocketComputerData
{
    private final Terminal terminal;
    private ComputerState state = ComputerState.OFF;
    private int lightColour = -1;

    public PocketComputerData( boolean colour )
    {
        terminal = new Terminal( ComputerCraft.pocketTermWidth, ComputerCraft.pocketTermHeight, colour );
    }

    public int getLightState()
    {
        return state != ComputerState.OFF ? lightColour : -1;
    }

    @Nonnull
    public Terminal getTerminal()
    {
        return terminal;
    }

    public ComputerState getState()
    {
        return state;
    }

    public void setState( ComputerState state, int lightColour )
    {
        this.state = state;
        this.lightColour = lightColour;
    }

    public void setTerminal( TerminalState state )
    {
        state.apply( terminal );
    }
}
