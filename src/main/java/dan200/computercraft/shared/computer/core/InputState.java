/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.core;

import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

/**
 * An {@link InputHandler} which keeps track of the current key and mouse state, and releases them when the container is closed.
 */
public class InputState implements InputHandler
{
    private final IContainerComputer owner;
    private final IntSet keysDown = new IntOpenHashSet( 4 );

    private int lastMouseX;
    private int lastMouseY;
    private int lastMouseDown = -1;

    public InputState( IContainerComputer owner )
    {
        this.owner = owner;
    }

    @Override
    public void queueEvent( String event, Object[] arguments )
    {
        IComputer computer = this.owner.getComputer();
        if( computer != null )
        {
            computer.queueEvent( event, arguments );
        }
    }

    @Override
    public void keyDown( int key, boolean repeat )
    {
        this.keysDown.add( key );
        IComputer computer = this.owner.getComputer();
        if( computer != null )
        {
            computer.keyDown( key, repeat );
        }
    }

    @Override
    public void keyUp( int key )
    {
        this.keysDown.remove( key );
        IComputer computer = this.owner.getComputer();
        if( computer != null )
        {
            computer.keyUp( key );
        }
    }

    @Override
    public void mouseClick( int button, int x, int y )
    {
        this.lastMouseX = x;
        this.lastMouseY = y;
        this.lastMouseDown = button;

        IComputer computer = this.owner.getComputer();
        if( computer != null )
        {
            computer.mouseClick( button, x, y );
        }
    }

    @Override
    public void mouseUp( int button, int x, int y )
    {
        this.lastMouseX = x;
        this.lastMouseY = y;
        this.lastMouseDown = -1;

        IComputer computer = this.owner.getComputer();
        if( computer != null )
        {
            computer.mouseUp( button, x, y );
        }
    }

    @Override
    public void mouseDrag( int button, int x, int y )
    {
        this.lastMouseX = x;
        this.lastMouseY = y;
        this.lastMouseDown = button;

        IComputer computer = this.owner.getComputer();
        if( computer != null )
        {
            computer.mouseDrag( button, x, y );
        }
    }

    @Override
    public void mouseScroll( int direction, int x, int y )
    {
        this.lastMouseX = x;
        this.lastMouseY = y;

        IComputer computer = this.owner.getComputer();
        if( computer != null )
        {
            computer.mouseScroll( direction, x, y );
        }
    }

    public void close()
    {
        IComputer computer = this.owner.getComputer();
        if( computer != null )
        {
            IntIterator keys = this.keysDown.iterator();
            while( keys.hasNext() )
            {
                computer.keyUp( keys.nextInt() );
            }

            if( this.lastMouseDown != -1 )
            {
                computer.mouseUp( this.lastMouseDown, this.lastMouseX, this.lastMouseY );
            }
        }

        this.keysDown.clear();
        this.lastMouseDown = -1;
    }
}
