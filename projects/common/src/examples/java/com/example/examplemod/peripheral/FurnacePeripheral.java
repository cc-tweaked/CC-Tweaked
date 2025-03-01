package com.example.examplemod.peripheral;

import com.example.examplemod.ExampleMod;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.GenericPeripheral;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

/**
 * A peripheral that adds a {@code getBurnTime} method to furnaces. This is used to demonstrate the usage of
 * {@link GenericPeripheral}.
 *
 * @see dan200.computercraft.api.peripheral
 * @see BrewingStandPeripheral Using {@code IPeripheral}.
 */
// @start region=body
public class FurnacePeripheral implements GenericPeripheral {
    @Override
    public String id() {
        return ResourceLocation.fromNamespaceAndPath(ExampleMod.MOD_ID, "furnace").toString();
    }

    @LuaFunction(mainThread = true)
    public int getBurnTime(AbstractFurnaceBlockEntity furnace) {
        // Don't do it this way! Use an access widener/transformer to access the "litTime" field instead.
        return furnace.saveWithoutMetadata(furnace.getLevel().registryAccess()).getInt("BurnTime");
    }
}
// @end region=body
