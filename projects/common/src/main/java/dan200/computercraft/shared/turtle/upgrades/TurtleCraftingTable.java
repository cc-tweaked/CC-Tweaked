// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.turtle.upgrades;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.turtle.AbstractTurtleUpgrade;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.api.turtle.TurtleUpgradeType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;


public class TurtleCraftingTable extends AbstractTurtleUpgrade.StackAware {
    public TurtleCraftingTable(ResourceLocation id, Ingredient item) {
        super(id, TurtleUpgradeType.PERIPHERAL, "upgrade.minecraft.crafting_table.adjective", item);
    }

    @Override
    public IPeripheral createPeripheral(ITurtleAccess turtle, TurtleSide side) {
        return new CraftingTablePeripheral(turtle);
    }
}
