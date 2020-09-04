/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.core;

import dan200.computercraft.shared.common.ClientTerminal;
import dan200.computercraft.shared.network.NetworkHandler;
import dan200.computercraft.shared.network.server.ComputerActionServerMessage;
import dan200.computercraft.shared.network.server.KeyEventServerMessage;
import dan200.computercraft.shared.network.server.MouseEventServerMessage;
import dan200.computercraft.shared.network.server.QueueEventServerMessage;
import dan200.computercraft.shared.network.server.RequestComputerMessage;

import net.minecraft.nbt.CompoundTag;

public class ClientComputer extends ClientTerminal implements IComputer {
    private final int m_instanceID;

    private boolean m_on = false;
    private boolean m_blinking = false;
    private CompoundTag m_userData = null;


    public ClientComputer(int instanceID) {
        super(false);
        this.m_instanceID = instanceID;
    }

    public CompoundTag getUserData() {
        return this.m_userData;
    }

    public void requestState() {
        // Request state from server
        NetworkHandler.sendToServer(new RequestComputerMessage(this.getInstanceID()));
    }

    // IComputer

    @Override
    public int getInstanceID() {
        return this.m_instanceID;
    }

    @Override
    public void turnOn() {
        // Send turnOn to server
        NetworkHandler.sendToServer(new ComputerActionServerMessage(this.m_instanceID, ComputerActionServerMessage.Action.TURN_ON));
    }

    @Override
    public void shutdown() {
        // Send shutdown to server
        NetworkHandler.sendToServer(new ComputerActionServerMessage(this.m_instanceID, ComputerActionServerMessage.Action.SHUTDOWN));
    }

    @Override
    public void reboot() {
        // Send reboot to server
        NetworkHandler.sendToServer(new ComputerActionServerMessage(this.m_instanceID, ComputerActionServerMessage.Action.REBOOT));
    }

    @Override
    public void queueEvent(String event, Object[] arguments) {
        // Send event to server
        NetworkHandler.sendToServer(new QueueEventServerMessage(this.m_instanceID, event, arguments));
    }

    @Override
    public boolean isOn() {
        return this.m_on;
    }

    @Override
    public boolean isCursorDisplayed() {
        return this.m_on && this.m_blinking;
    }

    @Override
    public void keyDown(int key, boolean repeat) {
        NetworkHandler.sendToServer(new KeyEventServerMessage(this.m_instanceID,
                                                              repeat ? KeyEventServerMessage.TYPE_REPEAT : KeyEventServerMessage.TYPE_DOWN,
                                                              key));
    }

    @Override
    public void keyUp(int key) {
        NetworkHandler.sendToServer(new KeyEventServerMessage(this.m_instanceID, KeyEventServerMessage.TYPE_UP, key));
    }

    @Override
    public void mouseClick(int button, int x, int y) {
        NetworkHandler.sendToServer(new MouseEventServerMessage(this.m_instanceID, MouseEventServerMessage.TYPE_CLICK, button, x, y));
    }

    @Override
    public void mouseUp(int button, int x, int y) {
        NetworkHandler.sendToServer(new MouseEventServerMessage(this.m_instanceID, MouseEventServerMessage.TYPE_UP, button, x, y));
    }

    @Override
    public void mouseDrag(int button, int x, int y) {
        NetworkHandler.sendToServer(new MouseEventServerMessage(this.m_instanceID, MouseEventServerMessage.TYPE_DRAG, button, x, y));
    }

    @Override
    public void mouseScroll(int direction, int x, int y) {
        NetworkHandler.sendToServer(new MouseEventServerMessage(this.m_instanceID, MouseEventServerMessage.TYPE_SCROLL, direction, x, y));
    }

    public void setState(ComputerState state, CompoundTag userData) {
        this.m_on = state != ComputerState.OFF;
        this.m_blinking = state == ComputerState.BLINKING;
        this.m_userData = userData;
    }
}
