/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.core;

/**
 * Receives some input and forwards it to a computer.
 *
 * @see InputState
 * @see IComputer
 */
public interface InputHandler
{
    void queueEvent( String event, Object[] arguments );

    default void keyDown( int key, boolean repeat )
    {
        queueEvent( "key", new Object[] { key, repeat } );
    }

    default void keyUp( int key )
    {
        queueEvent( "key_up", new Object[] { key } );
    }

    default void mouseClick( int button, int x, int y )
    {
        queueEvent( "mouse_click", new Object[] { button, x, y } );
    }

    default void mouseUp( int button, int x, int y )
    {
        queueEvent( "mouse_up", new Object[] { button, x, y } );
    }

    default void mouseDrag( int button, int x, int y )
    {
        queueEvent( "mouse_drag", new Object[] { button, x, y } );
    }

    default void mouseScroll( int direction, int x, int y )
    {
        queueEvent( "mouse_scroll", new Object[] { direction, x, y } );
    }

    default void mouseMove( int x, int y )
    {
        // Always send the first argument (e.g. button, direction) as 1, to maintain
        // backwards compatibility with programs that perform blanket-handling for
        // all mouse events. Off-screen mouse movements (-1) are translated to nil.
        queueEvent( "mouse_move", new Object[] { 1, x == -1 ? null : x, y == -1 ? null : y } );
    }

    default void update()
    {
    }
}
