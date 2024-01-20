// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.pocket;

import dan200.computercraft.client.FrameInfo;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.computer.core.ComputerState;
import dan200.computercraft.shared.computer.terminal.NetworkedTerminal;
import dan200.computercraft.shared.computer.terminal.TerminalState;
import dan200.computercraft.shared.config.Config;
import dan200.computercraft.shared.pocket.core.PocketServerComputer;

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
public class PocketComputerData {
    private final NetworkedTerminal terminal;
    private ComputerState state = ComputerState.OFF;
    private int primaryLightColour = -1;
    private int secondaryLightColour = -1;

    public PocketComputerData(boolean colour) {
        terminal = new NetworkedTerminal(Config.pocketTermWidth, Config.pocketTermHeight, colour);
    }

    public int getLightState() {
        if (state != ComputerState.OFF) {
            if (secondaryLightColour == -1) {
                return primaryLightColour;
            } else if (primaryLightColour == -1) {
                return secondaryLightColour;
            } else {
                double weight = ((Math.sin(((double)(FrameInfo.getTick() % 41) / 40)*Math.PI*2)+1)/2);
                return blend(primaryLightColour, secondaryLightColour, weight);
            }
        }
        return -1;
    }

    private static int blend(int a, int b, double weight) {
        int[][] rgb = {
            { a >> 16,  b >> 16 },
            { (a & 0x00ff00) >> 8, (b & 0x00ff00) >> 8 },
            { a & 0x0000ff, b & 0x0000ff }
        };
        int[] channels = new int[3];
        for (int i = 0; i < 3; i++) {
            channels[i] = (int)Math.sqrt(Math.pow(rgb[i][0], 2)*weight + Math.pow(rgb[i][1], 2)*(1-weight));
        }
        int color = 0;
        for (int channel : channels) {
            color = (color << 8) + channel;
        }
        return color;
    }

    public Terminal getTerminal() {
        return terminal;
    }

    public ComputerState getState() {
        return state;
    }

    public void setState(ComputerState state, int primaryLightColour, int secondaryLightColor) {
        this.state = state;
        this.primaryLightColour = primaryLightColour;
        this.secondaryLightColour = secondaryLightColor;
    }

    public void setTerminal(TerminalState state) {
        state.apply(terminal);
    }
}
