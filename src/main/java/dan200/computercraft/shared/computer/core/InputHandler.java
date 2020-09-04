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
public interface InputHandler {
    default void keyDown(int key, boolean repeat) {
        this.queueEvent("key",
                        new Object[] {
                       key,
                       repeat
                   });
    }

    void queueEvent(String event, Object[] arguments);

    default void keyUp(int key) {
        this.queueEvent("key_up", new Object[] {key});
    }

    default void mouseClick(int button, int x, int y) {
        this.queueEvent("mouse_click",
                        new Object[] {
                       button,
                       x,
                       y
                   });
    }

    default void mouseUp(int button, int x, int y) {
        this.queueEvent("mouse_up",
                        new Object[] {
                       button,
                       x,
                       y
                   });
    }

    default void mouseDrag(int button, int x, int y) {
        this.queueEvent("mouse_drag",
                        new Object[] {
                       button,
                       x,
                       y
                   });
    }

    default void mouseScroll(int direction, int x, int y) {
        this.queueEvent("mouse_scroll",
                        new Object[] {
                       direction,
                       x,
                       y
                   });
    }
}
