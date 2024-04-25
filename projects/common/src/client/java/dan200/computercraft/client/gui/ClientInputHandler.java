// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.gui;

import dan200.computercraft.client.network.ClientNetworking;
import dan200.computercraft.shared.computer.core.InputHandler;
import dan200.computercraft.shared.computer.menu.ComputerMenu;
import dan200.computercraft.shared.network.server.ComputerActionServerMessage;
import dan200.computercraft.shared.network.server.KeyEventServerMessage;
import dan200.computercraft.shared.network.server.MouseEventServerMessage;
import dan200.computercraft.shared.network.server.QueueEventServerMessage;
import net.minecraft.world.inventory.AbstractContainerMenu;

import javax.annotation.Nullable;

/**
 * An {@link InputHandler} for use on the client.
 * <p>
 * This queues events on the remote player's open {@link ComputerMenu}.
 */
public final class ClientInputHandler implements InputHandler {
    private final AbstractContainerMenu menu;

    public ClientInputHandler(AbstractContainerMenu menu) {
        this.menu = menu;
    }

    @Override
    public void turnOn() {
        ClientNetworking.sendToServer(new ComputerActionServerMessage(menu, ComputerActionServerMessage.Action.TURN_ON));
    }

    @Override
    public void shutdown() {
        ClientNetworking.sendToServer(new ComputerActionServerMessage(menu, ComputerActionServerMessage.Action.SHUTDOWN));
    }

    @Override
    public void reboot() {
        ClientNetworking.sendToServer(new ComputerActionServerMessage(menu, ComputerActionServerMessage.Action.REBOOT));
    }

    @Override
    public void queueEvent(String event, @Nullable Object[] arguments) {
        ClientNetworking.sendToServer(new QueueEventServerMessage(menu, event, arguments));
    }

    @Override
    public void keyDown(int key, boolean repeat) {
        ClientNetworking.sendToServer(new KeyEventServerMessage(menu, repeat ? KeyEventServerMessage.Action.REPEAT : KeyEventServerMessage.Action.DOWN, key));
    }

    @Override
    public void keyUp(int key) {
        ClientNetworking.sendToServer(new KeyEventServerMessage(menu, KeyEventServerMessage.Action.UP, key));
    }

    @Override
    public void mouseClick(int button, int x, int y) {
        ClientNetworking.sendToServer(new MouseEventServerMessage(menu, MouseEventServerMessage.Action.CLICK, button, x, y));
    }

    @Override
    public void mouseUp(int button, int x, int y) {
        ClientNetworking.sendToServer(new MouseEventServerMessage(menu, MouseEventServerMessage.Action.UP, button, x, y));
    }

    @Override
    public void mouseDrag(int button, int x, int y) {
        ClientNetworking.sendToServer(new MouseEventServerMessage(menu, MouseEventServerMessage.Action.DRAG, button, x, y));
    }

    @Override
    public void mouseScroll(int direction, int x, int y) {
        ClientNetworking.sendToServer(new MouseEventServerMessage(menu, MouseEventServerMessage.Action.SCROLL, direction, x, y));
    }
}
