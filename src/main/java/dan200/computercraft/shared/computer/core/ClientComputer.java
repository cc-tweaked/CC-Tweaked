/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.core;

import dan200.computercraft.shared.common.ClientTerminal;
import dan200.computercraft.shared.network.NetworkHandler;
import dan200.computercraft.shared.network.server.*;
import net.minecraft.nbt.CompoundTag;

public class ClientComputer extends ClientTerminal implements IComputer
{
    private final int instanceID;

    private boolean on = false;
    private boolean blinking = false;
    private CompoundTag userData = null;

    public ClientComputer( int instanceID )
    {
        super( false );
        this.instanceID = instanceID;
    }

    public CompoundTag getUserData()
    {
        return userData;
    }

    public void requestState()
    {
        // Request state from server
        NetworkHandler.sendToServer( new RequestComputerMessage( getInstanceID() ) );
    }

    // IComputer

    @Override
    public int getInstanceID()
    {
        return instanceID;
    }

    @Override
    public boolean isOn()
    {
        return on;
    }

    @Override
    public boolean isCursorDisplayed()
    {
        return on && blinking;
    }

    @Override
    public void turnOn()
    {
        // Send turnOn to server
        NetworkHandler.sendToServer( new ComputerActionServerMessage( instanceID, ComputerActionServerMessage.Action.TURN_ON ) );
    }

    @Override
    public void shutdown()
    {
        // Send shutdown to server
        NetworkHandler.sendToServer( new ComputerActionServerMessage( instanceID, ComputerActionServerMessage.Action.SHUTDOWN ) );
    }

    @Override
    public void reboot()
    {
        // Send reboot to server
        NetworkHandler.sendToServer( new ComputerActionServerMessage( instanceID, ComputerActionServerMessage.Action.REBOOT ) );
    }

    @Override
    public void queueEvent( String event, Object[] arguments )
    {
        // Send event to server
        NetworkHandler.sendToServer( new QueueEventServerMessage( instanceID, event, arguments ) );
    }

    @Override
    public void keyDown( int key, boolean repeat )
    {
        NetworkHandler.sendToServer( new KeyEventServerMessage( instanceID, repeat ? KeyEventServerMessage.TYPE_REPEAT : KeyEventServerMessage.TYPE_DOWN, key ) );
    }

    @Override
    public void keyUp( int key )
    {
        NetworkHandler.sendToServer( new KeyEventServerMessage( instanceID, KeyEventServerMessage.TYPE_UP, key ) );
    }

    @Override
    public void mouseClick( int button, int x, int y )
    {
        NetworkHandler.sendToServer( new MouseEventServerMessage( instanceID, MouseEventServerMessage.TYPE_CLICK, button, x, y ) );
    }

    @Override
    public void mouseUp( int button, int x, int y )
    {
        NetworkHandler.sendToServer( new MouseEventServerMessage( instanceID, MouseEventServerMessage.TYPE_UP, button, x, y ) );
    }

    @Override
    public void mouseDrag( int button, int x, int y )
    {
        NetworkHandler.sendToServer( new MouseEventServerMessage( instanceID, MouseEventServerMessage.TYPE_DRAG, button, x, y ) );
    }

    @Override
    public void mouseScroll( int direction, int x, int y )
    {
        NetworkHandler.sendToServer( new MouseEventServerMessage( instanceID, MouseEventServerMessage.TYPE_SCROLL, direction, x, y ) );
    }

    public void setState( ComputerState state, CompoundTag userData )
    {
        on = state != ComputerState.OFF;
        blinking = state == ComputerState.BLINKING;
        this.userData = userData;
    }
}
