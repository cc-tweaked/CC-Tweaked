// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.gui;

import org.lwjgl.glfw.GLFW;

/**
 * Supports for converting/translating key codes.
 */
public class KeyConverter {
    /**
     * GLFW's key events refer to the physical key code, rather than the "actual" key code (with keyboard layout
     * applied).
     * <p>
     * This makes sense for WASD-style input, but is a right pain for keyboard shortcuts — this function attempts to
     * translate those keys back to their "actual" key code. See also
     * <a href="https://github.com/glfw/glfw/issues/1502"> this discussion on GLFW's GitHub.</a>
     *
     * @param key      The current key code.
     * @param scanCode The current scan code.
     * @return The translated key code.
     */
    public static int physicalToActual(int key, int scanCode) {
        var name = GLFW.glfwGetKeyName(key, scanCode);
        if (name == null || name.length() != 1) return key;

        // If we've got a single character as the key name, treat that as the ASCII value of the key,
        // and map that back to a key code.
        var character = name.charAt(0);

        // 0-9 and A-Z map directly to their GLFW key (they're the same ASCII code).
        if ((character >= '0' && character <= '9') || (character >= 'A' && character <= 'Z')) return character;
        // a-z map to GLFW_KEY_{A,Z}
        if (character >= 'a' && character <= 'z') return GLFW.GLFW_KEY_A + (character - 'a');

        return key;
    }
}
