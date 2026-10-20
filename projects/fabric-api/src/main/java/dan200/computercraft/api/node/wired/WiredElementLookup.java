// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.node.wired;

import dan200.computercraft.api.network.wired.WiredElement;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * {@linkplain BlockApiLookup Block API lookup} for {@link WiredElement}s. This should be used to query wired elements
 * from a block.
 *
 * @deprecated Use {@link dan200.computercraft.api.network.wired.WiredElementLookup}
 */
@Deprecated
public final class WiredElementLookup {
    public static final ResourceLocation ID = dan200.computercraft.api.network.wired.WiredElementLookup.ID;

    private WiredElementLookup() {
    }

    public static BlockApiLookup<WiredElement, Direction> get() {
        return dan200.computercraft.api.network.wired.WiredElementLookup.get();
    }
}
