// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.peripheral.generic.methods;

import dan200.computercraft.api.lua.LuaFunction;
import net.minecraftforge.energy.IEnergyStorage;

/**
 * Fluid methods for Forge's {@link IEnergyStorage}.
 */
public final class EnergyMethods extends AbstractEnergyMethods<IEnergyStorage> {
    @Override
    @LuaFunction(mainThread = true)
    public int getEnergy(IEnergyStorage energy) {
        return energy.getEnergyStored();
    }

    @Override
    @LuaFunction(mainThread = true)
    public int getEnergyCapacity(IEnergyStorage energy) {
        return energy.getMaxEnergyStored();
    }
}
