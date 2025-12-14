// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.gui;

import dan200.computercraft.client.network.ClientNetworking;
import dan200.computercraft.shared.network.server.ComputerActionServerMessage;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Actions that can be applied to a computer.
 *
 * @see ComputerActionServerMessage
 */
public final class ClientComputerActions {
    private final AbstractContainerMenu menu;

    public ClientComputerActions(AbstractContainerMenu menu) {
        this.menu = menu;
    }

    public void terminate() {
        ClientNetworking.sendToServer(new ComputerActionServerMessage(menu, ComputerActionServerMessage.Action.TERMINATE));
    }

    public void turnOn() {
        ClientNetworking.sendToServer(new ComputerActionServerMessage(menu, ComputerActionServerMessage.Action.TURN_ON));
    }

    public void shutdown() {
        ClientNetworking.sendToServer(new ComputerActionServerMessage(menu, ComputerActionServerMessage.Action.SHUTDOWN));
    }

    public void reboot() {
        ClientNetworking.sendToServer(new ComputerActionServerMessage(menu, ComputerActionServerMessage.Action.REBOOT));
    }
}
