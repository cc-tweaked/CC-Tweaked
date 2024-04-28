// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.turtle.upgrades;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.turtle.AbstractTurtleUpgrade;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.api.turtle.TurtleUpgradeType;
import dan200.computercraft.api.upgrades.UpgradeType;
import dan200.computercraft.shared.ModRegistry;
import net.minecraft.world.item.ItemStack;


public class TurtleCraftingTable extends AbstractTurtleUpgrade {
    public TurtleCraftingTable(ItemStack stack) {
        super(TurtleUpgradeType.PERIPHERAL, "upgrade.minecraft.crafting_table.adjective", stack);
    }

    @Override
    public IPeripheral createPeripheral(ITurtleAccess turtle, TurtleSide side) {
        return new CraftingTablePeripheral(turtle);
    }

    @Override
    public UpgradeType<TurtleCraftingTable> getType() {
        return ModRegistry.TurtleUpgradeTypes.WORKBENCH.get();
    }
}
