// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.peripheral.generic.methods;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.GenericPeripheral;
import dan200.computercraft.api.peripheral.PeripheralType;
import net.minecraftforge.energy.IEnergyStorage;

/**
 * Methods for interacting with blocks using Forge's energy storage system.
 * <p>
 * This works with energy storage blocks, as well as generators and machines which consume energy.
 * <p>
 * :::note
 * Due to limitations with Forge's energy API, it is not possible to measure throughput (i.e. RF
 * used/generated per tick).
 * :::
 *
 * @cc.module energy_storage
 * @cc.since 1.94.0
 */
public class EnergyMethods implements GenericPeripheral {
    @Override
    public PeripheralType getType() {
        return PeripheralType.ofAdditional("energy_storage");
    }

    @Override
    public String id() {
        return ComputerCraftAPI.MOD_ID + ":energy";
    }

    /**
     * Get the energy of this block.
     *
     * @param energy The current energy storage.
     * @return The energy stored in this block, in FE.
     */
    @LuaFunction(mainThread = true)
    public static int getEnergy(IEnergyStorage energy) {
        return energy.getEnergyStored();
    }

    /**
     * Get the maximum amount of energy this block can store.
     *
     * @param energy The current energy storage.
     * @return The energy capacity of this block.
     */
    @LuaFunction(mainThread = true)
    public static int getEnergyCapacity(IEnergyStorage energy) {
        return energy.getMaxEnergyStored();
    }
}
