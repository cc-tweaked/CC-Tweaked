// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.gui;

import dan200.computercraft.client.network.ClientNetworking;
import dan200.computercraft.core.input.ComputerInput;
import dan200.computercraft.shared.computer.menu.ComputerMenu;
import dan200.computercraft.shared.network.server.KeyEventServerMessage;
import dan200.computercraft.shared.network.server.MouseEventServerMessage;
import dan200.computercraft.shared.network.server.PasteEventComputerMessage;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.nio.ByteBuffer;

/**
 * An {@link ComputerInput} for use on the client.
 * <p>
 * This queues events on the player's open {@link ComputerMenu}.
 */
public final class ClientComputerInput implements ComputerInput {
    private final AbstractContainerMenu menu;

    public ClientComputerInput(AbstractContainerMenu menu) {
        this.menu = menu;
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
    public void charTyped(byte chr) {
        ClientNetworking.sendToServer(new KeyEventServerMessage(menu, KeyEventServerMessage.Action.CHAR, chr));
    }

    @Override
    public void paste(ByteBuffer contents) {
        ClientNetworking.sendToServer(new PasteEventComputerMessage(menu, contents));
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
