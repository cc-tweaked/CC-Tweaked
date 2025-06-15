// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.render;

import dan200.computercraft.client.gui.ComputerScreen;
import net.minecraft.client.resources.metadata.gui.GuiSpriteScaling;

/**
 * Constants for the borders of computers, either for a {@linkplain ComputerScreen GUI} or
 * {@linkplain PocketItemRenderer in-hand pocket computers}.
 */
public final class ComputerBorderRenderer {
    /**
     * The margin between the terminal and its border.
     */
    public static final int MARGIN = 2;

    /**
     * The size of the terminal border.
     * <p>
     * This is only used for layout of elements within UI. When rendering, the size of the computer's border is
     * determined by its {@link GuiSpriteScaling}.
     */
    public static final int BORDER = 12;

    private ComputerBorderRenderer() {
    }
}
