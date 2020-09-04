/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.modem.wireless;

import javax.annotation.Nonnull;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralTile;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.peripheral.modem.ModemPeripheral;
import dan200.computercraft.shared.peripheral.modem.ModemState;
import dan200.computercraft.shared.util.TickScheduler;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class TileWirelessModem extends TileGeneric implements IPeripheralTile {
    private final boolean advanced;
    private final ModemPeripheral modem;
    private boolean hasModemDirection = false;
    private Direction modemDirection = Direction.DOWN;
    private boolean destroyed = false;
    public TileWirelessModem(BlockEntityType<? extends TileWirelessModem> type, boolean advanced) {
        super(type);
        this.advanced = advanced;
        this.modem = new Peripheral(this);
    }

    @Override
    public void cancelRemoval() {
        super.cancelRemoval();
        TickScheduler.schedule(this);
    }

    @Override
    public void resetBlock() {
        super.resetBlock();
        this.hasModemDirection = false;
        this.world.getBlockTickScheduler()
                  .schedule(this.getPos(),
                       this.getCachedState().getBlock(), 0);
    }

    @Override
    public void destroy() {
        if (!this.destroyed) {
            this.modem.destroy();
            this.destroyed = true;
        }
    }

    @Override
    public void blockTick() {
        Direction currentDirection = this.modemDirection;
        this.refreshDirection();
        // Invalidate the capability if the direction has changed. I'm not 100% happy with this implementation
        //  - ideally we'd do it within refreshDirection or updateContainingBlockInfo, but this seems the _safest_
        //  place.
        if (this.modem.getModemState()
                      .pollChanged()) {
            this.updateBlockState();
        }
    }

    private void refreshDirection() {
        if (this.hasModemDirection) {
            return;
        }

        this.hasModemDirection = true;
        this.modemDirection = this.getCachedState().get(BlockWirelessModem.FACING);
    }

    private void updateBlockState() {
        boolean on = this.modem.getModemState()
                               .isOpen();
        BlockState state = this.getCachedState();
        if (state.get(BlockWirelessModem.ON) != on) {
            this.getWorld().setBlockState(this.getPos(), state.with(BlockWirelessModem.ON, on));
        }
    }

    @Nonnull
    @Override
    public IPeripheral getPeripheral(Direction side) {
        this.refreshDirection();
        return side == this.modemDirection ? this.modem : null;
    }

    private static class Peripheral extends WirelessModemPeripheral {
        private final TileWirelessModem entity;

        Peripheral(TileWirelessModem entity) {
            super(new ModemState(() -> TickScheduler.schedule(entity)), entity.advanced);
            this.entity = entity;
        }

        @Nonnull
        @Override
        public World getWorld() {
            return this.entity.getWorld();
        }

        @Nonnull
        @Override
        public Vec3d getPosition() {
            BlockPos pos = this.entity.getPos()
                                      .offset(this.entity.modemDirection);
            return new Vec3d(pos.getX(), pos.getY(), pos.getZ());
        }

        @Nonnull
        @Override
        public Object getTarget() {
            return this.entity;
        }

        @Override
        public boolean equals(IPeripheral other) {
            return this == other || (other instanceof Peripheral && this.entity == ((Peripheral) other).entity);
        }
    }
}
