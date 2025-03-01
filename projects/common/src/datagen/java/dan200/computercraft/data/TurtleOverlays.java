// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.data;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.shared.turtle.TurtleOverlay;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/**
 * Built-in turtle overlays.
 */
final class TurtleOverlays {
    public static final ResourceKey<TurtleOverlay> RAINBOW_FLAG = create("rainbow_flag");
    public static final ResourceKey<TurtleOverlay> TRANS_FLAG = create("trans_flag");

    private static ResourceKey<TurtleOverlay> create(String name) {
        return ResourceKey.create(TurtleOverlay.REGISTRY, ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, name));
    }

    private TurtleOverlays() {
    }

    public static void register(BootstrapContext<TurtleOverlay> registry) {
        registry.register(RAINBOW_FLAG, new TurtleOverlay(
            ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "block/turtle_rainbow_overlay"),
            true
        ));

        registry.register(TRANS_FLAG, new TurtleOverlay(
            ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "block/turtle_trans_overlay"),
            true
        ));
    }
}
