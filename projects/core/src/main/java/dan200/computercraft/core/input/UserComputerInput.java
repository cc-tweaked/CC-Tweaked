// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.input;

import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.core.util.StringUtil;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.nio.ByteBuffer;

/**
 * A {@link ComputerInput} that wraps an existing {@link ComputerInput}. This both validates any user inputs (e.g.
 * ensuring mouse presses only happen on an advanced terminal), and supports {@linkplain #releaseInputs()
 * releasing any held inputs} (e.g. for when the computer loses focus).
 */
public final class UserComputerInput implements ComputerInput {
    private final ComputerInput delegate;
    private final boolean mouseSupport;
    private final int termWidth;
    private final int termHeight;

    private final IntSet keysDown = new IntOpenHashSet(4);

    private int lastMouseX;
    private int lastMouseY;
    private int lastMouseDown = -1;

    public UserComputerInput(ComputerInput delegate, boolean mouseSupport, int termWidth, int termHeight) {
        this.delegate = delegate;
        this.mouseSupport = mouseSupport;
        this.termWidth = termWidth;
        this.termHeight = termHeight;
    }

    public UserComputerInput(ComputerInput delegate, Terminal terminal) {
        this(delegate, terminal.isColour(), terminal.getWidth(), terminal.getHeight());
    }

    @Override
    public void keyDown(int key, boolean repeat) {
        if (key < 0) return;

        keysDown.add(key);
        delegate.keyDown(key, repeat);
    }

    /**
     * Queue a {@code key} event on the computer. This behaves the same as {@link #keyDown(int, boolean)}, but infers
     * the {@code "repeat} state from the currently held keys.
     *
     * @param key The key to press.
     */
    public void keyDown(int key) {
        keyDown(key, keysDown.contains(key));
    }

    @Override
    public void keyUp(int key) {
        if (key < 0) return;

        keysDown.remove(key);
        delegate.keyUp(key);
    }

    @Override
    public void charTyped(byte chr) {
        delegate.charTyped(chr);
    }

    public void codepointTyped(int codepoint) {
        var terminalChar = StringUtil.unicodeToTerminal(codepoint);
        if (StringUtil.isTypableChar(terminalChar)) charTyped((byte) terminalChar);
    }

    private static boolean isValidClipboard(ByteBuffer buffer) {
        for (int i = buffer.position(), max = buffer.limit(); i < max; i++) {
            if (!StringUtil.isTypableChar(buffer.get(i))) return false;
        }
        return true;
    }

    @Override
    public void paste(ByteBuffer contents) {
        if (contents.remaining() > 0 && isValidClipboard(contents)) delegate.paste(contents);
    }

    /**
     * Paste a string.
     *
     * @param contents The string to paste.
     */
    public void paste(String contents) {
        paste(StringUtil.getClipboardString(contents));
    }

    @Override
    public void mouseClick(int button, int x, int y) {
        if (!mouseSupport || button < 1 || button > 3) return;
        var clampedX = lastMouseX = Math.min(Math.max(x, 1), termWidth);
        var clampedY = lastMouseY = Math.min(Math.max(y, 1), termHeight);

        delegate.mouseClick(button, clampedX, clampedY);
        lastMouseDown = button;
    }

    /**
     * Queue a {@code mouse_click} event on the computer. This behaves the same as {@link #mouseClick(int, int, int)},
     * but infers the mouse position from the last mouse position.
     *
     * @param button The mouse button pressed, between 1 and 3.
     */
    public void mouseClick(int button) {
        mouseClick(button, lastMouseX, lastMouseY);
    }

    @Override
    public void mouseUp(int button, int x, int y) {
        if (!mouseSupport || button < 1 || button > 3) return;
        var clampedX = lastMouseX = Math.min(Math.max(x, 1), termWidth);
        var clampedY = lastMouseY = Math.min(Math.max(y, 1), termHeight);

        if (lastMouseDown == button) {
            delegate.mouseUp(button, clampedX, clampedY);
            lastMouseDown = -1;
        }
    }

    /**
     * Queue a {@code mouse_scroll} event on the computer. This behaves the same as {@link #mouseUp(int, int, int)},
     * but infers the mouse position from the last mouse position.
     *
     * @param button The mouse button released, between 1 and 3.
     */
    public void mouseUp(int button) {
        mouseUp(button, lastMouseX, lastMouseY);
    }

    @Override
    public void mouseDrag(int button, int x, int y) {
        if (!mouseSupport || button < 1 || button > 3) return;
        var clampedX = Math.min(Math.max(x, 1), termWidth);
        var clampedY = Math.min(Math.max(y, 1), termHeight);

        if (button == lastMouseDown && (clampedX != lastMouseX || clampedY != lastMouseY)) {
            delegate.mouseDrag(button, clampedX, clampedY);
            lastMouseX = clampedX;
            lastMouseY = clampedY;
        }
    }

    /**
     * Update the mouse position, and optionally queue a {@code mouse_drag} event on the computer.
     * <p>
     * This is similar to {@link #mouseDrag(int, int, int)}, but when the currently clicked button is not available.
     *
     * @param x The X position of the mouse, between 1 and the terminal width.
     * @param y The Y position of the mouse, between 1 and the terminal width.
     */
    public void mouseMove(int x, int y) {
        if (!mouseSupport) return;
        var clampedX = Math.min(Math.max(x, 1), termWidth);
        var clampedY = Math.min(Math.max(y, 1), termHeight);

        if (lastMouseDown != -1 && (clampedX != lastMouseX || clampedY != lastMouseY)) {
            delegate.mouseDrag(lastMouseDown, clampedX, clampedY);
        }

        lastMouseX = clampedX;
        lastMouseY = clampedY;
    }

    @Override
    public void mouseScroll(int direction, int x, int y) {
        if (!mouseSupport || direction == 0) return;
        var clampedX = lastMouseX = Math.min(Math.max(x, 1), termWidth);
        var clampedY = lastMouseY = Math.min(Math.max(y, 1), termHeight);

        delegate.mouseScroll(direction, clampedX, clampedY);
    }

    /**
     * Queue a {@code mouse_scroll} event on the computer. This behaves the same as {@link #mouseScroll(int, int, int)},
     * but infers the mouse position from the last mouse position.
     *
     * @param direction The direction of the scroll.
     */
    public void mouseScroll(int direction) {
        mouseScroll(direction, lastMouseX, lastMouseY);
    }

    /**
     * Release all currently held inputs, such as held keys and pressed mouse buttons.
     */
    public void releaseInputs() {
        // Release all keys
        var keys = keysDown.iterator();
        while (keys.hasNext()) delegate.keyUp(keys.nextInt());
        keysDown.clear();

        // Release last held mouse button.
        if (lastMouseDown != -1) {
            delegate.mouseUp(lastMouseDown, lastMouseX, lastMouseY);
            lastMouseDown = -1;
        }
    }
}
