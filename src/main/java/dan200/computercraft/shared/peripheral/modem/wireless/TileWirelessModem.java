/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.modem.wireless;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.peripheral.modem.ModemPeripheral;
import dan200.computercraft.shared.peripheral.modem.ModemState;
import dan200.computercraft.shared.util.CapabilityUtil;
import dan200.computercraft.shared.util.NamedTileEntityType;
import dan200.computercraft.shared.util.TickScheduler;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static dan200.computercraft.shared.Capabilities.CAPABILITY_PERIPHERAL;

public class TileWirelessModem extends TileGeneric
{
    public static final NamedTileEntityType<TileWirelessModem> FACTORY_NORMAL = NamedTileEntityType.create(
        new ResourceLocation( ComputerCraft.MOD_ID, "wireless_modem_normal" ),
        f -> new TileWirelessModem( f, false )
    );

    public static final NamedTileEntityType<TileWirelessModem> FACTORY_ADVANCED = NamedTileEntityType.create(
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

        @Nonnull
        @Override
        public Object getTarget()
        {
            return entity;
        }
    }

    private final boolean advanced;

    private boolean hasModemDirection = false;
    private Direction modemDirection = Direction.DOWN;
    private final ModemPeripheral modem;
    private boolean destroyed = false;
    private LazyOptional<IPeripheral> modemCap;

    public TileWirelessModem( TileEntityType<? extends TileWirelessModem> type, boolean advanced )
    {
        super( type );
        this.advanced = advanced;
        modem = new Peripheral( this );
    }

    @Override
    public void onLoad()
    {
        super.onLoad();
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
    public void updateContainingBlockInfo()
    {
        super.updateContainingBlockInfo();
        hasModemDirection = false;
        world.getPendingBlockTicks().scheduleTick( getPos(), getBlockState().getBlock(), 0 );
    }

    @Override
    public void blockTick()
    {
        Direction currentDirection = modemDirection;
        refreshDirection();
        // Invalidate the capability if the direction has changed. I'm not 100% happy with this implementation
        //  - ideally we'd do it within refreshDirection or updateContainingBlockInfo, but this seems the _safest_
        //  place.
        if( currentDirection != modemDirection ) modemCap = CapabilityUtil.invalidate( modemCap );

        if( modem.getModemState().pollChanged() ) updateBlockState();
    }

    private void refreshDirection()
    {
        if( hasModemDirection ) return;

        hasModemDirection = true;
        modemDirection = getBlockState().get( BlockWirelessModem.FACING );
    }

    private void updateBlockState()
    {
        boolean on = modem.getModemState().isOpen();
        BlockState state = getBlockState();
        if( state.get( BlockWirelessModem.ON ) != on )
        {
            getWorld().setBlockState( getPos(), state.with( BlockWirelessModem.ON, on ) );
        }
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability( @Nonnull Capability<T> cap, @Nullable Direction side )
    {
        if( cap == CAPABILITY_PERIPHERAL )
        {
            refreshDirection();
            if( side != null && modemDirection != side ) return LazyOptional.empty();
            if( modemCap == null ) modemCap = LazyOptional.of( () -> modem );
            return modemCap.cast();
        }

        return super.getCapability( cap, side );
    }
}
