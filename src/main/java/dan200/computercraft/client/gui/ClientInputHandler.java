/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.gui;

import dan200.computercraft.shared.computer.core.InputHandler;
import dan200.computercraft.shared.computer.menu.ComputerMenu;
import dan200.computercraft.shared.network.NetworkHandler;
import dan200.computercraft.shared.network.server.ComputerActionServerMessage;
import dan200.computercraft.shared.network.server.KeyEventServerMessage;
import dan200.computercraft.shared.network.server.MouseEventServerMessage;
import dan200.computercraft.shared.network.server.QueueEventServerMessage;
import net.minecraft.inventory.container.Container;

import javax.annotation.Nullable;

/**
 * An {@link InputHandler} which for use on the client.
 * <p>
 * This queues events on the remote player's open {@link ComputerMenu}
 */
public final class ClientInputHandler implements InputHandler
{
    private final Container menu;

    public ClientInputHandler( Container menu )
    {
        this.menu = menu;
    }

    @Override
    public void turnOn()
    {
        NetworkHandler.sendToServer( new ComputerActionServerMessage( menu, ComputerActionServerMessage.Action.TURN_ON ) );
    }

    @Override
    public void shutdown()
    {
        NetworkHandler.sendToServer( new ComputerActionServerMessage( menu, ComputerActionServerMessage.Action.SHUTDOWN ) );
    }

    @Override
    public void reboot()
    {
        NetworkHandler.sendToServer( new ComputerActionServerMessage( menu, ComputerActionServerMessage.Action.REBOOT ) );
    }

    @Override
    public void queueEvent( String event, @Nullable Object[] arguments )
    {
        NetworkHandler.sendToServer( new QueueEventServerMessage( menu, event, arguments ) );
    }

    @Override
    public void keyDown( int key, boolean repeat )
    {
        NetworkHandler.sendToServer( new KeyEventServerMessage( menu, repeat ? KeyEventServerMessage.TYPE_REPEAT : KeyEventServerMessage.TYPE_DOWN, key ) );
    }

    @Override
    public void keyUp( int key )
    {
        NetworkHandler.sendToServer( new KeyEventServerMessage( menu, KeyEventServerMessage.TYPE_UP, key ) );
    }

    @Override
    public void mouseClick( int button, int x, int y )
    {
        NetworkHandler.sendToServer( new MouseEventServerMessage( menu, MouseEventServerMessage.TYPE_CLICK, button, x, y ) );
    }

    @Override
    public void mouseUp( int button, int x, int y )
    {
        NetworkHandler.sendToServer( new MouseEventServerMessage( menu, MouseEventServerMessage.TYPE_UP, button, x, y ) );
    }

    @Override
    public void mouseDrag( int button, int x, int y )
    {
        NetworkHandler.sendToServer( new MouseEventServerMessage( menu, MouseEventServerMessage.TYPE_DRAG, button, x, y ) );
    }

    @Override
    public void mouseScroll( int direction, int x, int y )
    {
        NetworkHandler.sendToServer( new MouseEventServerMessage( menu, MouseEventServerMessage.TYPE_SCROLL, direction, x, y ) );
    }
}
