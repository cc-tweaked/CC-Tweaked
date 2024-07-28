// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.pocket.peripherals;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.pocket.IPocketAccess;
import dan200.computercraft.shared.peripheral.modem.ModemState;
import dan200.computercraft.shared.peripheral.modem.wireless.WirelessModemPeripheral;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class PocketModemPeripheral extends WirelessModemPeripheral {
    private final IPocketAccess access;

    public PocketModemPeripheral(boolean advanced, IPocketAccess access) {
        super(new ModemState(), advanced);
        this.access = access;
    }

    @Override
    public Level getLevel() {
        return access.getLevel();
    }

    @Override
    public Vec3 getPosition() {
        return access.getPosition();
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return other instanceof PocketModemPeripheral;
    }
}
