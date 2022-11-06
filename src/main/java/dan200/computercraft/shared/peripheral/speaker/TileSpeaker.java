/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.speaker;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.network.client.SpeakerStopClientMessage;
import dan200.computercraft.shared.platform.PlatformHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TileSpeaker extends TileGeneric {
    private final SpeakerPeripheral peripheral;

    public TileSpeaker(BlockEntityType<TileSpeaker> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        peripheral = new Peripheral(this);
    }

    protected void serverTick() {
        peripheral.update();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide) {
            PlatformHelper.get().sendToAllPlayers(new SpeakerStopClientMessage(peripheral.getSource()), getLevel().getServer());
        }
    }

    public IPeripheral peripheral() {
        return peripheral;
    }

    private static final class Peripheral extends SpeakerPeripheral {
        private final TileSpeaker speaker;

        private Peripheral(TileSpeaker speaker) {
            this.speaker = speaker;
        }

        @Nonnull
        @Override
        public SpeakerPosition getPosition() {
            return SpeakerPosition.of(speaker.getLevel(), Vec3.atCenterOf(speaker.getBlockPos()));
        }

        @Override
        public boolean equals(@Nullable IPeripheral other) {
            return this == other || (other instanceof Peripheral && speaker == ((Peripheral) other).speaker);
        }
    }
}
