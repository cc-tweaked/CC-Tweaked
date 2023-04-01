// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.pocket.peripherals;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.pocket.IPocketAccess;
import dan200.computercraft.shared.peripheral.speaker.SpeakerPosition;
import dan200.computercraft.shared.peripheral.speaker.UpgradeSpeakerPeripheral;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class PocketSpeakerPeripheral extends UpgradeSpeakerPeripheral {
    private final IPocketAccess access;
    private @Nullable Level level;
    private Vec3 position = Vec3.ZERO;

    public PocketSpeakerPeripheral(IPocketAccess access) {
        this.access = access;
    }

    @Override
    public SpeakerPosition getPosition() {
        var entity = access.getEntity();
        return entity == null ? SpeakerPosition.of(level, position) : SpeakerPosition.of(entity);
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return other instanceof PocketSpeakerPeripheral;
    }

    @Override
    public void update() {
        var entity = access.getEntity();
        if (entity != null) {
            level = entity.level;
            position = entity.position();
        }

        super.update();

        access.setLight(madeSound() ? 0x3320fc : -1);
    }
}
