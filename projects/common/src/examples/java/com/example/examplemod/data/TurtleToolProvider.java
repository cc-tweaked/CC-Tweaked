package com.example.examplemod.data;

import com.example.examplemod.ExampleMod;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleToolBuilder;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

/**
 * Extends the bootstrap registries with a new turtle tool.
 * <p>
 * See {@link TurtleUpgradeProvider} and {@link ITurtleUpgrade} for how this would be used in datagen.
 */
public class TurtleToolProvider {
    // @start region=body
    public static void addUpgrades(BootstrapContext<ITurtleUpgrade> upgrades) {
        TurtleToolBuilder
            .tool(ResourceLocation.fromNamespaceAndPath(ExampleMod.MOD_ID, "wooden_pickaxe"), Items.WOODEN_PICKAXE)
            .register(upgrades);
    }
    // @end region=body
}
