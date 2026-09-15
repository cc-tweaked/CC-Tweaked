// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

/**
 * Maps Minecraft/SDL input events to the corresponding GLFS code.
 */
public final class SDLInput {
    private static final Int2IntMap KEYS;

    private SDLInput() {
    }

    static {
        KEYS = new Int2IntOpenHashMap();
        KEYS.put(InputConstants.KEY_SPACE, 32);
        KEYS.put(InputConstants.KEY_APOSTROPHE, 39);
        KEYS.put(InputConstants.KEY_COMMA, 44);
        KEYS.put(InputConstants.KEY_MINUS, 45);
        KEYS.put(InputConstants.KEY_PERIOD, 46);
        KEYS.put(InputConstants.KEY_SLASH, 47);
        KEYS.put(InputConstants.KEY_0, 48);
        KEYS.put(InputConstants.KEY_1, 49);
        KEYS.put(InputConstants.KEY_2, 50);
        KEYS.put(InputConstants.KEY_3, 51);
        KEYS.put(InputConstants.KEY_4, 52);
        KEYS.put(InputConstants.KEY_5, 53);
        KEYS.put(InputConstants.KEY_6, 54);
        KEYS.put(InputConstants.KEY_7, 55);
        KEYS.put(InputConstants.KEY_8, 56);
        KEYS.put(InputConstants.KEY_9, 57);
        KEYS.put(InputConstants.KEY_SEMICOLON, 59);
        KEYS.put(InputConstants.KEY_EQUALS, 61);
        KEYS.put(InputConstants.KEY_A, 65);
        KEYS.put(InputConstants.KEY_B, 66);
        KEYS.put(InputConstants.KEY_C, 67);
        KEYS.put(InputConstants.KEY_D, 68);
        KEYS.put(InputConstants.KEY_E, 69);
        KEYS.put(InputConstants.KEY_F, 70);
        KEYS.put(InputConstants.KEY_G, 71);
        KEYS.put(InputConstants.KEY_H, 72);
        KEYS.put(InputConstants.KEY_I, 73);
        KEYS.put(InputConstants.KEY_J, 74);
        KEYS.put(InputConstants.KEY_K, 75);
        KEYS.put(InputConstants.KEY_L, 76);
        KEYS.put(InputConstants.KEY_M, 77);
        KEYS.put(InputConstants.KEY_N, 78);
        KEYS.put(InputConstants.KEY_O, 79);
        KEYS.put(InputConstants.KEY_P, 80);
        KEYS.put(InputConstants.KEY_Q, 81);
        KEYS.put(InputConstants.KEY_R, 82);
        KEYS.put(InputConstants.KEY_S, 83);
        KEYS.put(InputConstants.KEY_T, 84);
        KEYS.put(InputConstants.KEY_U, 85);
        KEYS.put(InputConstants.KEY_V, 86);
        KEYS.put(InputConstants.KEY_W, 87);
        KEYS.put(InputConstants.KEY_X, 88);
        KEYS.put(InputConstants.KEY_Y, 89);
        KEYS.put(InputConstants.KEY_Z, 90);
        KEYS.put(InputConstants.KEY_LBRACKET, 91);
        KEYS.put(InputConstants.KEY_BACKSLASH, 92);
        KEYS.put(InputConstants.KEY_RBRACKET, 93);
        KEYS.put(InputConstants.KEY_GRAVE, 96);
        KEYS.put(InputConstants.KEY_RETURN, 257);
        KEYS.put(InputConstants.KEY_TAB, 258);
        KEYS.put(InputConstants.KEY_BACKSPACE, 259);
        KEYS.put(InputConstants.KEY_INSERT, 260);
        KEYS.put(InputConstants.KEY_DELETE, 261);
        KEYS.put(InputConstants.KEY_RIGHT, 262);
        KEYS.put(InputConstants.KEY_LEFT, 263);
        KEYS.put(InputConstants.KEY_DOWN, 264);
        KEYS.put(InputConstants.KEY_UP, 265);
        KEYS.put(InputConstants.KEY_PAGEUP, 266);
        KEYS.put(InputConstants.KEY_PAGEDOWN, 267);
        KEYS.put(InputConstants.KEY_HOME, 268);
        KEYS.put(InputConstants.KEY_END, 269);
        KEYS.put(InputConstants.KEY_CAPSLOCK, 280);
        KEYS.put(InputConstants.KEY_SCROLLLOCK, 281);
        KEYS.put(InputConstants.KEY_NUMLOCK, 282);
        KEYS.put(InputConstants.KEY_PRINTSCREEN, 283);
        KEYS.put(InputConstants.KEY_PAUSE, 284);
        KEYS.put(InputConstants.KEY_F1, 290);
        KEYS.put(InputConstants.KEY_F2, 291);
        KEYS.put(InputConstants.KEY_F3, 292);
        KEYS.put(InputConstants.KEY_F4, 293);
        KEYS.put(InputConstants.KEY_F5, 294);
        KEYS.put(InputConstants.KEY_F6, 295);
        KEYS.put(InputConstants.KEY_F7, 296);
        KEYS.put(InputConstants.KEY_F8, 297);
        KEYS.put(InputConstants.KEY_F9, 298);
        KEYS.put(InputConstants.KEY_F10, 299);
        KEYS.put(InputConstants.KEY_F11, 300);
        KEYS.put(InputConstants.KEY_F12, 301);
        KEYS.put(InputConstants.KEY_F13, 302);
        KEYS.put(InputConstants.KEY_F14, 303);
        KEYS.put(InputConstants.KEY_F15, 304);
        KEYS.put(InputConstants.KEY_F16, 305);
        KEYS.put(InputConstants.KEY_F17, 306);
        KEYS.put(InputConstants.KEY_F18, 307);
        KEYS.put(InputConstants.KEY_F19, 308);
        KEYS.put(InputConstants.KEY_F20, 309);
        KEYS.put(InputConstants.KEY_F21, 310);
        KEYS.put(InputConstants.KEY_F22, 311);
        KEYS.put(InputConstants.KEY_F23, 312);
        KEYS.put(InputConstants.KEY_F24, 313);
        // KEYS.put(InputConstants.KEY_F25, 314);
        KEYS.put(InputConstants.KEY_NUMPAD0, 320);
        KEYS.put(InputConstants.KEY_NUMPAD1, 321);
        KEYS.put(InputConstants.KEY_NUMPAD2, 322);
        KEYS.put(InputConstants.KEY_NUMPAD3, 323);
        KEYS.put(InputConstants.KEY_NUMPAD4, 324);
        KEYS.put(InputConstants.KEY_NUMPAD5, 325);
        KEYS.put(InputConstants.KEY_NUMPAD6, 326);
        KEYS.put(InputConstants.KEY_NUMPAD7, 327);
        KEYS.put(InputConstants.KEY_NUMPAD8, 328);
        KEYS.put(InputConstants.KEY_NUMPAD9, 329);
        // KEYS.put(InputConstants.KEY_NUMPADDECIMAL, 330);
        // KEYS.put(InputConstants.KEY_NUMPADDIVIDE, 331);
        // KEYS.put(InputConstants.KEY_NUMPADMULTIPLY, 332);
        // KEYS.put(InputConstants.KEY_NUMPADSUBTRACT, 333);
        // KEYS.put(InputConstants.KEY_NUMPADADD, 334);
        KEYS.put(InputConstants.KEY_NUMPADENTER, 335);
        KEYS.put(InputConstants.KEY_NUMPADEQUALS, 336);
        KEYS.put(InputConstants.KEY_LSHIFT, 340);
        KEYS.put(InputConstants.KEY_LCONTROL, 341);
        KEYS.put(InputConstants.KEY_LALT, 342);
        KEYS.put(InputConstants.KEY_LGUI, 343);
        KEYS.put(InputConstants.KEY_RSHIFT, 344);
        KEYS.put(InputConstants.KEY_RCONTROL, 345);
        KEYS.put(InputConstants.KEY_RALT, 346);
        // KEYS.put(InputConstants.KEY_MENU, 348);
    }

    public static int getKey(KeyEvent event) {
        return KEYS.getOrDefault(event.key(), -1);
    }

    public static int getButton(MouseButtonEvent event) {
        return switch (event.button()) {
            case InputConstants.MOUSE_BUTTON_LEFT -> 1;
            case InputConstants.MOUSE_BUTTON_RIGHT -> 2;
            case InputConstants.MOUSE_BUTTON_MIDDLE -> 3;
            default -> -1;
        };
    }
}
