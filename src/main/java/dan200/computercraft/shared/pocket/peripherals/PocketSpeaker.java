/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.pocket.peripherals;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.pocket.AbstractPocketUpgrade;
import dan200.computercraft.api.pocket.IPocketAccess;
import dan200.computercraft.shared.peripheral.speaker.UpgradeSpeakerPeripheral;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PocketSpeaker extends AbstractPocketUpgrade {
    public PocketSpeaker(ResourceLocation id, ItemStack item) {
        super(id, UpgradeSpeakerPeripheral.ADJECTIVE, item);
    }

    @Nullable
    @Override
    public IPeripheral createPeripheral(@Nonnull IPocketAccess access) {
        return new PocketSpeakerPeripheral(access);
    }

    @Override
    public void update(@Nonnull IPocketAccess access, @Nullable IPeripheral peripheral) {
        if (!(peripheral instanceof PocketSpeakerPeripheral)) return;
        ((PocketSpeakerPeripheral) peripheral).update();
    }
}
