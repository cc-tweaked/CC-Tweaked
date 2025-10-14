// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.model;

import dan200.computercraft.api.ComputerCraftAPI;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;

/**
 * Definitions for the lectern printout model.
 *
 * @see LecternBookModel
 * @see LecternPrintoutModel
 */
public final class LecternPrintoutModelDefinitions {
    public static final Material MATERIAL = Sheets.BLOCK_ENTITIES_MAPPER.apply(ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "printout"));

    static final int TEXTURE_WIDTH = 32;
    static final int TEXTURE_HEIGHT = 32;

    private LecternPrintoutModelDefinitions() {
    }
}
