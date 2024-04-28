// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.pocket.peripherals;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.pocket.AbstractPocketUpgrade;
import dan200.computercraft.api.pocket.IPocketAccess;
import dan200.computercraft.api.upgrades.UpgradeType;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.peripheral.speaker.UpgradeSpeakerPeripheral;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public class PocketSpeaker extends AbstractPocketUpgrade {
    public PocketSpeaker(ItemStack item) {
        super(UpgradeSpeakerPeripheral.ADJECTIVE, item);
    }

    @Nullable
    @Override
    public IPeripheral createPeripheral(IPocketAccess access) {
        return new PocketSpeakerPeripheral(access);
    }

    @Override
    public void update(IPocketAccess access, @Nullable IPeripheral peripheral) {
        if (!(peripheral instanceof PocketSpeakerPeripheral)) return;
        ((PocketSpeakerPeripheral) peripheral).update();
    }

    @Override
    public UpgradeType<PocketSpeaker> getType() {
        return ModRegistry.PocketUpgradeTypes.SPEAKER.get();
    }
}
