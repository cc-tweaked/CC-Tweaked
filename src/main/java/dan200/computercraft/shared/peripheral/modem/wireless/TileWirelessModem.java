/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.modem.wireless;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.peripheral.modem.ModemPeripheral;
import dan200.computercraft.shared.peripheral.modem.ModemState;
import dan200.computercraft.shared.util.NamedBlockEntityType;
import dan200.computercraft.shared.util.TickScheduler;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class TileWirelessModem extends TileGeneric
{
    public static final NamedBlockEntityType<TileWirelessModem> FACTORY_NORMAL = NamedBlockEntityType.create(
        new ResourceLocation( ComputerCraft.MOD_ID, "wireless_modem_normal" ),
        f -> new TileWirelessModem( f, false )
    );

    public static final NamedBlockEntityType<TileWirelessModem> FACTORY_ADVANCED = NamedBlockEntityType.create(
        new ResourceLocation( ComputerCraft.MOD_ID, "wireless_modem_advanced" ),
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
    private EnumFacing modemDirection = EnumFacing.DOWN;
    private final ModemPeripheral modem;
    private boolean destroyed = false;

    public TileWirelessModem( TileEntityType<? extends TileWirelessModem> type, boolean advanced )
    {
        super( type );
        this.advanced = advanced;
        this.modem = new Peripheral( this );
    }

    @Override
    public void onLoad()
    {
        super.onLoad();
        updateDirection();
        world.getPendingBlockTicks().scheduleTick( getPos(), getBlockState().getBlock(), 0 );
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
    public void updateContainingBlockInfo()
    {
        super.updateContainingBlockInfo();
        hasModemDirection = false;
        world.getPendingBlockTicks().scheduleTick( getPos(), getBlockState().getBlock(), 0 );
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
        modemDirection = getBlockState().get( BlockWirelessModem.FACING );
    }

    private void updateBlockState()
    {
        boolean on = modem.getModemState().isOpen();
        IBlockState state = getBlockState();
        if( state.get( BlockWirelessModem.ON ) != on )
        {
            getWorld().setBlockState( getPos(), state.with( BlockWirelessModem.ON, on ) );
        }
    }
}
