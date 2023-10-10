// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.gui;

import dan200.computercraft.client.platform.ClientPlatformHelper;
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
        ClientPlatformHelper.get().sendToServer(new ComputerActionServerMessage(menu, ComputerActionServerMessage.Action.TURN_ON));
    }

    @Override
    public void shutdown() {
        ClientPlatformHelper.get().sendToServer(new ComputerActionServerMessage(menu, ComputerActionServerMessage.Action.SHUTDOWN));
    }

    @Override
    public void reboot() {
        ClientPlatformHelper.get().sendToServer(new ComputerActionServerMessage(menu, ComputerActionServerMessage.Action.REBOOT));
    }

    @Override
    public void queueEvent(String event, @Nullable Object[] arguments) {
        ClientPlatformHelper.get().sendToServer(new QueueEventServerMessage(menu, event, arguments));
    }

    @Override
    public void keyDown(int key, boolean repeat) {
        ClientPlatformHelper.get().sendToServer(new KeyEventServerMessage(menu, repeat ? KeyEventServerMessage.TYPE_REPEAT : KeyEventServerMessage.TYPE_DOWN, key));
    }

    @Override
    public void keyUp(int key) {
        ClientPlatformHelper.get().sendToServer(new KeyEventServerMessage(menu, KeyEventServerMessage.TYPE_UP, key));
    }

    @Override
    public void mouseClick(int button, int x, int y) {
        ClientPlatformHelper.get().sendToServer(new MouseEventServerMessage(menu, MouseEventServerMessage.TYPE_CLICK, button, x, y));
    }

    @Override
    public void mouseUp(int button, int x, int y) {
        ClientPlatformHelper.get().sendToServer(new MouseEventServerMessage(menu, MouseEventServerMessage.TYPE_UP, button, x, y));
    }

    @Override
    public void mouseDrag(int button, int x, int y) {
        ClientPlatformHelper.get().sendToServer(new MouseEventServerMessage(menu, MouseEventServerMessage.TYPE_DRAG, button, x, y));
    }

    @Override
    public void mouseScroll(int direction, int x, int y) {
        ClientPlatformHelper.get().sendToServer(new MouseEventServerMessage(menu, MouseEventServerMessage.TYPE_SCROLL, direction, x, y));
    }
}
