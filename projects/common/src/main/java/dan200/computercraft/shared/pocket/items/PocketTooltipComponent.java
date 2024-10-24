// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.pocket.items;

import dan200.computercraft.shared.computer.core.ComputerFamily;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * A tooltip computer describing a pocket computer.
 * <p>
 * This has no behaviour on its own. When rendering, this is converted to an equivalent client-side component,
 * that renders the computer's terminal.
 *
 * @param id     The instance ID of this pocket computer.
 * @param family The family of this pocket computer.
 * @see PocketComputerItem#getTooltipImage(ItemStack)
 * @see dan200.computercraft.client.pocket.PocketClientTooltipComponent
 */
public record PocketTooltipComponent(UUID id, ComputerFamily family) implements TooltipComponent {
}
