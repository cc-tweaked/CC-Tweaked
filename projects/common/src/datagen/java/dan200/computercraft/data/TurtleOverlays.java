// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.data;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.client.turtle.TurtleOverlay;
import net.minecraft.resources.ResourceLocation;

import java.util.function.BiConsumer;

/**
 * Built-in turtle overlays.
 */
final class TurtleOverlays {
    public static final ResourceLocation RAINBOW_FLAG = create("rainbow_flag");
    public static final ResourceLocation TRANS_FLAG = create("trans_flag");

    private static ResourceLocation create(String name) {
        return ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, name);
    }

    private TurtleOverlays() {
    }

    public static void register(BiConsumer<ResourceLocation, TurtleOverlay.Unbaked> registry) {
        registry.accept(RAINBOW_FLAG, new TurtleOverlay.Unbaked(
            ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "block/turtle_rainbow_overlay"),
            true
        ));

        registry.accept(TRANS_FLAG, new TurtleOverlay.Unbaked(
            ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "block/turtle_trans_overlay"),
            true
        ));
    }
}
