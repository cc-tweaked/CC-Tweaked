/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.core;

import java.util.HashMap;
import java.util.Map;

/**
 * Receives some input and forwards it to a computer
 *
 * @see InputState
 * @see IComputer
 */
public interface InputHandler
{
    int MODIFIER_CTRL = 1;
    int MODIFIER_ALT = 2;
    int MODIFIER_SHIFT = 4;

    void queueEvent( String event, Object[] arguments );

    default void keyDown( int key, boolean repeat, int modifiers )
    {
        Map<String, Boolean> modifierTable = new HashMap<>( 3 );
        modifierTable.put( "ctrl", (modifiers & MODIFIER_CTRL) != 0 );
        modifierTable.put( "alt", (modifiers & MODIFIER_ALT) != 0 );
        modifierTable.put( "shift", (modifiers & MODIFIER_SHIFT) != 0 );
        queueEvent( "key", new Object[] { key, repeat, modifierTable } );
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
}
