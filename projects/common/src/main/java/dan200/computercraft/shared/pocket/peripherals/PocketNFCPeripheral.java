// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.pocket.peripherals;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.network.PacketNetwork;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.pocket.IPocketAccess;
import dan200.computercraft.core.util.Nullability;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;

public class PocketNFCPeripheral extends PocketModemPeripheral {

    private @Nullable Entity entity = null;

    public PocketNFCPeripheral(IPocketAccess access) {
        super(false, access);
    }

    public void update(IPocketAccess access) {
        setLocation(access);

        var state = getModemState();
        if (state.pollChanged()) access.setLightSecondary(state.isOpen() ? 0xf39d20 : -1);
    }

    @Override
    void setLocation(IPocketAccess access) {
        entity = access.getEntity();
        super.setLocation(access);
    }

    @Override
    public double getRange() {
        return 0;
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return other instanceof PocketNFCPeripheral;
    }

    @Override
    protected PacketNetwork getNetwork() {
        return ComputerCraftAPI.getNfcNetwork(Nullability.assertNonNull(getLevel().getServer()), Nullability.assertNonNull(entity));
    }
}
