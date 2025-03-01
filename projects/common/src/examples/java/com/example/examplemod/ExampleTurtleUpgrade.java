package com.example.examplemod;

import dan200.computercraft.api.turtle.AbstractTurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleUpgradeType;
import dan200.computercraft.api.upgrades.UpgradeType;
import net.minecraft.world.item.ItemStack;

/**
 * An example turtle upgrade.
 */
// @start region=body
public class ExampleTurtleUpgrade extends AbstractTurtleUpgrade {
    public ExampleTurtleUpgrade(ItemStack stack) {
        super(TurtleUpgradeType.PERIPHERAL, "example", stack);
    }

    @Override
    public UpgradeType<ExampleTurtleUpgrade> getType() {
        return ExampleMod.EXAMPLE_TURTLE_UPGRADE;
    }
}
// @end region=body
