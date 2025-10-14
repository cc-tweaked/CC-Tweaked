// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.peripheral.generic.methods;

import dan200.computercraft.api.lua.LuaFunction;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;

/**
 * Fluid methods for Forge's {@link EnergyHandler}.
 */
public final class EnergyMethods extends AbstractEnergyMethods<EnergyHandler> {
    @Override
    @LuaFunction(mainThread = true)
    public long getEnergy(EnergyHandler energy) {
        return energy.getAmountAsLong();
    }

    @Override
    @LuaFunction(mainThread = true)
    public long getEnergyCapacity(EnergyHandler energy) {
        return energy.getCapacityAsLong();
    }
}
