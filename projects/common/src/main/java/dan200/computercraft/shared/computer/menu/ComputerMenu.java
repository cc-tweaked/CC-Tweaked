// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.computer.menu;

import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.terminal.TerminalState;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * An instance of {@link AbstractContainerMenu} which provides a computer. You should implement this if you provide
 * custom computer GUIs.
 */
public interface ComputerMenu {
    /**
     * Get the computer you are interacting with.
     *
     * @return The computer you are interacting with.
     * @throws UnsupportedOperationException When used on the client side.
     */
    ServerComputer getComputer();

    /**
     * Get the input controller for this container. This should be used when receiving events from the client.
     *
     * @return This container's input.
     * @throws UnsupportedOperationException When used on the client side.
     */
    ServerInputHandler getInput();

    /**
     * Set the current terminal state. This is called on the client when the server syncs a computer's terminal
     * contents.
     *
     * @param state The new terminal state.
     * @throws UnsupportedOperationException When used on the server.
     */
    void updateTerminal(TerminalState state);
}
