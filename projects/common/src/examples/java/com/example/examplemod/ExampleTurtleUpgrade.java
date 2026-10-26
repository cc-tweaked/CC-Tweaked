package com.example.examplemod;

import dan200.computercraft.api.turtle.AbstractTurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleUpgradeType;
import dan200.computercraft.api.upgrades.UpgradeType;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * An example turtle upgrade.
 */
// @start region=body
public class ExampleTurtleUpgrade extends AbstractTurtleUpgrade.StackAware {
    public ExampleTurtleUpgrade(Ingredient stack) {
        super(TurtleUpgradeType.PERIPHERAL, "upgrade.examplemod.example_turtle_upgrade.adjective", stack);
    }

    @Override
    public UpgradeType<ExampleTurtleUpgrade> getType() {
        return ExampleMod.EXAMPLE_TURTLE_UPGRADE;
    }
}
// @end region=body
