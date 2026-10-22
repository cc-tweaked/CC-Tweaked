// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.media;

import dan200.computercraft.impl.ComputerCraftAPIService;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.stream.Stream;

/**
 * The contents of a page (or book) created by a ComputerCraft printer.
 *
 * @since 1.115
 */
public interface PrintoutContents {
    /**
     * Get the (possibly empty) title for this printout.
     *
     * @return The title of this printout.
     */
    String getTitle();

    /**
     * Get the text contents of this printout, as a sequence of lines.
     * <p>
     * The lines in the printout may include blank lines at the end of the document, as well as trailing spaces on each
     * line.
     *
     * @return The text contents of this printout.
     */
    Stream<String> getTextLines();

    /**
     * Get the printout contents for a particular stack.
     *
     * @param stack The stack to get the contents for.
     * @return The printout contents, or {@code null} if this is not a printout item.
     */
    static @Nullable PrintoutContents get(ItemStack stack) {
        return ComputerCraftAPIService.get().getPrintoutContents(stack);
    }
}
