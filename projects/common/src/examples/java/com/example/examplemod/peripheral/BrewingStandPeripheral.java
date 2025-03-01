package com.example.examplemod.peripheral;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * A peripheral that adds a {@code getFuel()} method to brewing stands. This demonstrates the usage of
 * {@link IPeripheral}.
 *
 * @see dan200.computercraft.api.peripheral
 * @see FurnacePeripheral Using {@code GenericPeripheral}.
 */
// @start region=body
public class BrewingStandPeripheral implements IPeripheral {
    private final BrewingStandBlockEntity brewingStand;

    public BrewingStandPeripheral(BrewingStandBlockEntity brewingStand) {
        this.brewingStand = brewingStand;
    }

    @Override
    public String getType() {
        return "brewing_stand";
    }

    @LuaFunction
    public final int getFuel() {
        // Don't do it this way! Use an access widener/transformer to access the "fuel" field instead.
        return brewingStand.saveWithoutMetadata(brewingStand.getLevel().registryAccess()).getInt("Fuel");
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return other instanceof BrewingStandPeripheral o && brewingStand == o.brewingStand;
    }
}
// @end region=body
