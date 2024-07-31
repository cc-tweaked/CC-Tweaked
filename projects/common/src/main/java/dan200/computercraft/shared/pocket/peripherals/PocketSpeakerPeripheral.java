// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.pocket.peripherals;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.pocket.IPocketAccess;
import dan200.computercraft.shared.peripheral.speaker.SpeakerPosition;
import dan200.computercraft.shared.peripheral.speaker.UpgradeSpeakerPeripheral;

import javax.annotation.Nullable;

public class PocketSpeakerPeripheral extends UpgradeSpeakerPeripheral {
    private final IPocketAccess access;

    public PocketSpeakerPeripheral(IPocketAccess access) {
        this.access = access;
    }

    @Override
    public SpeakerPosition getPosition() {
        var entity = access.getEntity();
        return entity == null ? SpeakerPosition.of(access.getLevel(), access.getPosition()) : SpeakerPosition.of(entity);
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return other instanceof PocketSpeakerPeripheral;
    }

    @Override
    public void update() {
        super.update();

        access.setLight(madeSound() ? 0x3320fc : -1);
    }
}
