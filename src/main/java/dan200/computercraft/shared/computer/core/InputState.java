/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.core;

import dan200.computercraft.ComputerCraft;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

/**
 * An {@link InputHandler} which keeps track of the current key and mouse state, and releases them when the container
 * is closed.
 */
public class InputState implements InputHandler
{
    private final IContainerComputer owner;
    private final IntSet keysDown = new IntOpenHashSet( 4 );

    private int lastMouseX;
    private int lastMouseY;
    private int lastMouseDown = -1;

    private int lastMouseMoveX = -1;
    private int lastMouseMoveY = -1;
    private long lastMouseMove = -1;
    private boolean mouseMoved = false;

    public InputState( IContainerComputer owner )
    {
        this.owner = owner;
    }

    @Override
    public void queueEvent( String event, Object[] arguments )
    {
        IComputer computer = owner.getComputer();
        if( computer != null ) computer.queueEvent( event, arguments );
    }

    @Override
    public void keyDown( int key, boolean repeat )
    {
        keysDown.add( key );
        IComputer computer = owner.getComputer();
        if( computer != null ) computer.keyDown( key, repeat );
    }

    @Override
    public void keyUp( int key )
    {
        keysDown.remove( key );
        IComputer computer = owner.getComputer();
        if( computer != null ) computer.keyUp( key );
    }

    @Override
    public void mouseClick( int button, int x, int y )
    {
        lastMouseX = x;
        lastMouseY = y;
        lastMouseDown = button;

        IComputer computer = owner.getComputer();
        if( computer != null ) computer.mouseClick( button, x, y );
    }

    @Override
    public void mouseUp( int button, int x, int y )
    {
        lastMouseX = x;
        lastMouseY = y;
        lastMouseDown = -1;

        IComputer computer = owner.getComputer();
        if( computer != null ) computer.mouseUp( button, x, y );
    }

    @Override
    public void mouseDrag( int button, int x, int y )
    {
        lastMouseX = x;
        lastMouseY = y;
        lastMouseDown = button;

        IComputer computer = owner.getComputer();
        if( computer != null ) computer.mouseDrag( button, x, y );
    }

    @Override
    public void mouseScroll( int direction, int x, int y )
    {
        lastMouseX = x;
        lastMouseY = y;

        IComputer computer = owner.getComputer();
        if( computer != null ) computer.mouseScroll( direction, x, y );
    }

    @Override
    public void mouseMove( int x, int y )
    {
        // Delegate mouse movement to the throttler, which will ensure the most
        // recent mouse movement event is always received.

        // An intentional decision here is to *not* update lastMouseX and lastMouseY,
        // as the mouse_up event currently behaves based on the click and scroll
        // events.

        if( ComputerCraft.mouseMoveThrottle >= 0 && (lastMouseMoveX != x || lastMouseMoveY != y) )
        {
            lastMouseMoveX = x;
            lastMouseMoveY = y;
            mouseMoved = true; // The mouse_move event will be performed next time update() is called.
        }
    }

    @Override
    public void update()
    {
        // Handle mouse_move throttling:
        if( mouseMoved && System.currentTimeMillis() - ComputerCraft.mouseMoveThrottle > lastMouseMove )
        {
            IComputer computer = owner.getComputer();
            if( computer != null )
            {
                computer.mouseMove(
                    lastMouseMoveX == -1 ? -1 : lastMouseMoveX + 1,
                    lastMouseMoveY == -1 ? -1 : lastMouseMoveY + 1
                );
            }

            mouseMoved = false;
            lastMouseMove = System.currentTimeMillis();
        }
    }

    public void close()
    {
        IComputer computer = owner.getComputer();
        if( computer != null )
        {
            IntIterator keys = keysDown.iterator();
            while( keys.hasNext() ) computer.keyUp( keys.nextInt() );

            if( lastMouseDown != -1 ) computer.mouseUp( lastMouseDown, lastMouseX, lastMouseY );
            if( lastMouseMoveX != -1 || lastMouseMoveY != -1 ) computer.mouseMove( -1, -1 );
        }

        keysDown.clear();
        lastMouseDown = -1;
        lastMouseMoveX = -1;
        lastMouseMoveY = -1;
    }
}
