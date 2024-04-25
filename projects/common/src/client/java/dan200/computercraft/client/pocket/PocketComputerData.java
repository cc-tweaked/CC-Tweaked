// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.pocket;

import dan200.computercraft.shared.computer.core.ComputerState;
import dan200.computercraft.shared.computer.terminal.NetworkedTerminal;
import dan200.computercraft.shared.computer.terminal.TerminalState;
import dan200.computercraft.shared.pocket.core.PocketServerComputer;

import javax.annotation.Nullable;

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
public final class PocketComputerData {
    private @Nullable NetworkedTerminal terminal;
    private ComputerState state;
    private int lightColour;

    PocketComputerData(ComputerState state, int lightColour, @Nullable TerminalState terminalData) {
        this.state = state;
        this.lightColour = lightColour;
        if (terminalData != null) terminal = terminalData.create();
    }

    public int getLightState() {
        return state != ComputerState.OFF ? lightColour : -1;
    }

    public @Nullable NetworkedTerminal getTerminal() {
        return terminal;
    }

    public ComputerState getState() {
        return state;
    }

    void setState(ComputerState state, int lightColour, @Nullable TerminalState terminalData) {
        this.state = state;
        this.lightColour = lightColour;

        if (terminalData != null) {
            if (terminal == null) {
                terminal = terminalData.create();
            } else {
                terminalData.apply(terminal);
            }
        }
    }
}
