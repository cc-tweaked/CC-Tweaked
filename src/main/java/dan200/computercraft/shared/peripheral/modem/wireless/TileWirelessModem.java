/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.modem.wireless;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralTile;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.peripheral.modem.ModemPeripheral;
import dan200.computercraft.shared.peripheral.modem.ModemState;
import dan200.computercraft.shared.util.NamedBlockEntityType;
import dan200.computercraft.shared.util.TickScheduler;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TileWirelessModem extends TileGeneric implements IPeripheralTile
{
    public static final NamedBlockEntityType<TileWirelessModem> FACTORY_NORMAL = NamedBlockEntityType.create(
        new Identifier( ComputerCraft.MOD_ID, "wireless_modem_normal" ),
        f -> new TileWirelessModem( f, false )
    );

    public static final NamedBlockEntityType<TileWirelessModem> FACTORY_ADVANCED = NamedBlockEntityType.create(
        new Identifier( ComputerCraft.MOD_ID, "wireless_modem_advanced" ),
        f -> new TileWirelessModem( f, true )
    );

    private static class Peripheral extends WirelessModemPeripheral
    {
        private final TileWirelessModem entity;

        Peripheral( TileWirelessModem entity )
        {
            super( new ModemState( () -> TickScheduler.schedule( entity ) ), entity.advanced );
            this.entity = entity;
        }

        @Nonnull
        @Override
        public World getWorld()
        {
            return entity.getWorld();
        }

        @Nonnull
        @Override
        public Vec3d getPosition()
        {
            BlockPos pos = entity.getPos().offset( entity.modemDirection );
            return new Vec3d( pos.getX(), pos.getY(), pos.getZ() );
        }

        @Override
        public boolean equals( IPeripheral other )
        {
            return this == other || (other instanceof Peripheral && entity == ((Peripheral) other).entity);
        }
    }

    private final boolean advanced;

    private boolean hasModemDirection = false;
    private Direction modemDirection = Direction.DOWN;
    private final ModemPeripheral modem;
    private boolean destroyed = false;

    public TileWirelessModem( BlockEntityType<? extends TileWirelessModem> type, boolean advanced )
    {
        super( type );
        this.advanced = advanced;
        modem = new Peripheral( this );
    }

    @Override
    public void validate()
    {
        super.validate();
        TickScheduler.schedule( this );
    }

    @Override
    public void destroy()
    {
        if( !destroyed )
        {
            modem.destroy();
            destroyed = true;
        }
    }

    @Override
    public void markDirty()
    {
        super.markDirty();
        if( world != null )
        {
            updateDirection();
        }
        else
        {
            hasModemDirection = false;
        }
    }

    @Override
    public void resetBlock()
    {
        super.resetBlock();
        hasModemDirection = false;
        world.getBlockTickScheduler().schedule( getPos(), getCachedState().getBlock(), 0 );
    }

    @Override
    public void blockTick()
    {
        updateDirection();

        if( modem.getModemState().pollChanged() ) updateBlockState();
    }

    private void updateDirection()
    {
        if( hasModemDirection ) return;

        hasModemDirection = true;
        modemDirection = getCachedState().get( BlockWirelessModem.FACING );
    }

    private void updateBlockState()
    {
        boolean on = modem.getModemState().isOpen();
        BlockState state = getCachedState();
        if( state.get( BlockWirelessModem.ON ) != on )
        {
            getWorld().setBlockState( getPos(), state.with( BlockWirelessModem.ON, on ) );
        }
    }

    @Nullable
    @Override
    public IPeripheral getPeripheral( @Nonnull Direction side )
    {
        updateDirection();
        return side == modemDirection ? modem : null;
    }
}
