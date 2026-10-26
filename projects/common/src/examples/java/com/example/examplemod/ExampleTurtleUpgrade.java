package com.example.examplemod;

import dan200.computercraft.api.turtle.AbstractTurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleUpgradeType;
import dan200.computercraft.api.upgrades.UpgradeBase;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * An example turtle upgrade.
 */
// @start region=body
public class ExampleTurtleUpgrade extends AbstractTurtleUpgrade.StackAware {
    public ExampleTurtleUpgrade(ResourceLocation id, Ingredient item) {
        super(id, TurtleUpgradeType.PERIPHERAL, UpgradeBase.getDefaultAdjective(id), item);
    }
}
// @end region=body
