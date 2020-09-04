/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.pocket.peripherals;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.pocket.AbstractPocketUpgrade;
import dan200.computercraft.api.pocket.IPocketAccess;
import dan200.computercraft.shared.ComputerCraftRegistry;

import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;

public class PocketSpeaker extends AbstractPocketUpgrade {
    public PocketSpeaker() {
        super(new Identifier("computercraft", "speaker"), ComputerCraftRegistry.ModBlocks.SPEAKER);
    }

    @Nullable
    @Override
    public IPeripheral createPeripheral(@Nonnull IPocketAccess access) {
        return new PocketSpeakerPeripheral();
    }

    @Override
    public void update(@Nonnull IPocketAccess access, @Nullable IPeripheral peripheral) {
        if (!(peripheral instanceof PocketSpeakerPeripheral)) {
            return;
        }

        PocketSpeakerPeripheral speaker = (PocketSpeakerPeripheral) peripheral;

        Entity entity = access.getEntity();
        if (entity != null) {
            speaker.setLocation(entity.getEntityWorld(), entity.getCameraPosVec(1));
        }

        speaker.update();
        access.setLight(speaker.madeSound(20) ? 0x3320fc : -1);
    }
}
