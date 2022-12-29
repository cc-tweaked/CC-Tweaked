/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.pocket.peripherals;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.pocket.IPocketAccess;
import dan200.computercraft.shared.peripheral.modem.ModemState;
import dan200.computercraft.shared.peripheral.modem.wireless.WirelessModemPeripheral;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class PocketModemPeripheral extends WirelessModemPeripheral {
    private @Nullable Level level = null;
    private Vec3 position = Vec3.ZERO;

    public PocketModemPeripheral(boolean advanced, IPocketAccess access) {
        super(new ModemState(), advanced);
        setLocation(access);
    }

    void setLocation(IPocketAccess access) {
        var entity = access.getEntity();
        if (entity != null) {
            level = entity.getCommandSenderWorld();
            position = entity.getEyePosition(1);
        }
    }

    @Override
    public Level getLevel() {
        if (level == null) throw new IllegalStateException("Using modem before position has been defined");
        return level;
    }

    @Override
    public Vec3 getPosition() {
        return position;
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return other instanceof PocketModemPeripheral;
    }
}
