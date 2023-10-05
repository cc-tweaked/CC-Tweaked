// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.web.js;

import org.teavm.jso.JSObject;

import javax.annotation.Nullable;

/**
 * The Javascript-side terminal which displays this computer.
 *
 * @see Callbacks.AddComputer#addComputer(ComputerDisplay)
 */
public interface ComputerDisplay extends JSObject {
    /**
     * Set this computer's current state.
     *
     * @param label This computer's label
     * @param on    If this computer is on right now
     */
    void setState(@Nullable String label, boolean on);

    /**
     * Update the terminal's properties.
     *
     * @param width        The terminal width
     * @param height       The terminal height
     * @param x            The X cursor
     * @param y            The Y cursor
     * @param blink        Whether the cursor is blinking
     * @param cursorColour The cursor's colour
     */
    void updateTerminal(int width, int height, int x, int y, boolean blink, int cursorColour);

    /**
     * Set a line on the terminal.
     *
     * @param line The line index to set
     * @param text The line's text
     * @param fore The line's foreground
     * @param back The line's background
     */
    void setTerminalLine(int line, String text, String fore, String back);

    /**
     * Set the palette colour for a specific index.
     *
     * @param colour The colour index to set
     * @param r      The red value, between 0 and 1
     * @param g      The green value, between 0 and 1
     * @param b      The blue value, between 0 and 1
     */
    void setPaletteColour(int colour, double r, double g, double b);

    /**
     * Mark the terminal as having changed. Should be called after all other terminal methods.
     */
    void flushTerminal();
}
