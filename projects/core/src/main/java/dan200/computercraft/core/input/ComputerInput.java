// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.input;

import dan200.computercraft.core.util.StringUtil;

import java.nio.ByteBuffer;

/**
 * Input events that can be performed on a computer.
 *
 * @see EventComputerInput
 * @see UserComputerInput
 */
public interface ComputerInput {
    /**
     * Queue a {@code key} event.
     *
     * @param key    The key that was pressed.
     * @param repeat Whether this is a repeat input.
     */
    void keyDown(int key, boolean repeat);

    /**
     * Queue a {@code key_up} event.
     *
     * @param key The key that was released.
     */
    void keyUp(int key);

    /**
     * Type a character on the computer.
     *
     * @param chr The character to type.
     * @see StringUtil#isTypableChar(byte)
     */
    void charTyped(byte chr);

    /**
     * Paste a string.
     *
     * @param contents The string to paste.
     * @see StringUtil#getClipboardString(String)
     */
    void paste(ByteBuffer contents);

    /**
     * Queue a {@code mouse_click} event.
     *
     * @param button The mouse button that was pressed, between 1 and 3 (inclusive).
     * @param x      The x coordinate of the mouse, between 1 and the terminal width (inclusive).
     * @param y      The y coordinate of the mouse, between 1 and the terminal height (inclusive).
     */
    void mouseClick(int button, int x, int y);

    /**
     * Queue a {@code mouse_up} event.
     *
     * @param button The mouse button that was released, between 1 and 3 (inclusive).
     * @param x      The x coordinate of the mouse, between 1 and the terminal width (inclusive).
     * @param y      The y coordinate of the mouse, between 1 and the terminal height (inclusive).
     */
    void mouseUp(int button, int x, int y);

    /**
     * Queue a {@code mouse_drag} event.
     *
     * @param button The mouse button that is being pressed, between 1 and 3 (inclusive).
     * @param x      The x coordinate of the mouse, between 1 and the terminal width (inclusive).
     * @param y      The y coordinate of the mouse, between 1 and the terminal height (inclusive).
     */
    void mouseDrag(int button, int x, int y);

    /**
     * Queue a {@code mouse_scroll} event.
     *
     * @param direction The direction of the scroll, where negative values are up and positive ones are down.
     * @param x         The x coordinate of the mouse, between 1 and the terminal width (inclusive).
     * @param y         The y coordinate of the mouse, between 1 and the terminal height (inclusive).
     */
    void mouseScroll(int direction, int x, int y);
}
