/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.modem.wireless;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.common.GenericTile;
import dan200.computercraft.shared.peripheral.modem.ModemPeripheral;
import dan200.computercraft.shared.peripheral.modem.ModemState;
import dan200.computercraft.shared.util.TickScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class WirelessModemBlockEntity extends GenericTile {
    private static class Peripheral extends WirelessModemPeripheral {
        private final WirelessModemBlockEntity entity;

        Peripheral(WirelessModemBlockEntity entity) {
            super(new ModemState(() -> TickScheduler.schedule(entity.tickToken)), entity.advanced);
            this.entity = entity;
        }

        @Override
        public Level getLevel() {
            return entity.getLevel();
        }

        @Override
        public Vec3 getPosition() {
            return Vec3.atLowerCornerOf(entity.getBlockPos().relative(entity.getDirection()));
        }

        @Override
        public boolean equals(@Nullable IPeripheral other) {
            return this == other || (other instanceof Peripheral && entity == ((Peripheral) other).entity);
        }

        @Override
        public Object getTarget() {
            return entity;
        }
    }

    private final boolean advanced;

    private final ModemPeripheral modem;
    private boolean destroyed = false;
    private @Nullable Runnable modemChanged;
    private final TickScheduler.Token tickToken = new TickScheduler.Token(this);

    public WirelessModemBlockEntity(BlockEntityType<? extends WirelessModemBlockEntity> type, BlockPos pos, BlockState state, boolean advanced) {
        super(type, pos, state);
        this.advanced = advanced;
        modem = new Peripheral(this);
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved(); // TODO: Replace with onLoad
        TickScheduler.schedule(tickToken);
    }

    @Override
    public void destroy() {
        if (!destroyed) {
            modem.destroy();
            destroyed = true;
        }
    }

    @Override
    @Deprecated
    public void setBlockState(BlockState state) {
        var direction = getDirection();
        super.setBlockState(state);
        if (getDirection() != direction && modemChanged != null) modemChanged.run();
    }

    @Override
    public void blockTick() {
        if (modem.getModemState().pollChanged()) updateBlockState();
    }

    private Direction getDirection() {
        return getBlockState().getValue(WirelessModemBlock.FACING);
    }

    private void updateBlockState() {
        var on = modem.getModemState().isOpen();
        var state = getBlockState();
        if (state.getValue(WirelessModemBlock.ON) != on) {
            getLevel().setBlockAndUpdate(getBlockPos(), state.setValue(WirelessModemBlock.ON, on));
        }
    }

    @Nullable
    public IPeripheral getPeripheral(@Nullable Direction direction) {
        if (destroyed) return null;
        return direction == null || getDirection() == direction ? modem : null;
    }

    public void onModemChanged(Runnable callback) {
        modemChanged = callback;
    }
}
