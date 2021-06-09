/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.modem.wired;

import com.google.common.base.Objects;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.network.wired.IWiredElement;
import dan200.computercraft.api.network.wired.IWiredNode;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralTile;
import dan200.computercraft.shared.command.text.ChatHelpers;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.peripheral.modem.ModemState;
import dan200.computercraft.shared.util.DirectionUtil;
import dan200.computercraft.shared.util.TickScheduler;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.*;

import static dan200.computercraft.shared.peripheral.modem.wired.BlockWiredModemFull.MODEM_ON;
import static dan200.computercraft.shared.peripheral.modem.wired.BlockWiredModemFull.PERIPHERAL_ON;

public class TileWiredModemFull extends TileGeneric implements IPeripheralTile
{
    private static final String NBT_PERIPHERAL_ENABLED = "PeripheralAccess";
    private final WiredModemPeripheral[] modems = new WiredModemPeripheral[6];
    private final WiredModemLocalPeripheral[] peripherals = new WiredModemLocalPeripheral[6];
    private final ModemState modemState = new ModemState( () -> TickScheduler.schedule( this ) );
    private final WiredModemElement element = new FullElement( this );
    private final IWiredNode node = this.element.getNode();
    private boolean peripheralAccessAllowed = false;
    private boolean destroyed = false;
    private boolean connectionsFormed = false;

    public TileWiredModemFull( BlockEntityType<TileWiredModemFull> type )
    {
        super( type );
        for( int i = 0; i < this.peripherals.length; i++ )
        {
            Direction facing = Direction.byId( i );
            this.peripherals[i] = new WiredModemLocalPeripheral();
        }
    }

    @Override
    public void destroy()
    {
        if( !this.destroyed )
        {
            this.destroyed = true;
            this.doRemove();
        }
        super.destroy();
    }

    @Override
    public void onChunkUnloaded()
    {
        super.onChunkUnloaded();
        doRemove();
    }

    private void doRemove()
    {
        if( this.world == null || !this.world.isClient )
        {
            this.node.remove();
            this.connectionsFormed = false;
        }
    }

    @Nonnull
    @Override
    public ActionResult onActivate( PlayerEntity player, Hand hand, BlockHitResult hit )
    {
        if( this.getWorld().isClient )
        {
            return ActionResult.SUCCESS;
        }

        // On server, we interacted if a peripheral was found
        Set<String> oldPeriphNames = this.getConnectedPeripheralNames();
        this.togglePeripheralAccess();
        Set<String> periphNames = this.getConnectedPeripheralNames();

        if( !Objects.equal( periphNames, oldPeriphNames ) )
        {
            sendPeripheralChanges( player, "chat.computercraft.wired_modem.peripheral_disconnected", oldPeriphNames );
            sendPeripheralChanges( player, "chat.computercraft.wired_modem.peripheral_connected", periphNames );
        }

        return ActionResult.SUCCESS;
    }

    @Override
    public void onNeighbourChange( @Nonnull BlockPos neighbour )
    {
        this.onNeighbourTileEntityChange( neighbour );
    }

    @Override
    public void onNeighbourTileEntityChange( @Nonnull BlockPos neighbour )
    {
        if( !this.world.isClient && this.peripheralAccessAllowed )
        {
            for( Direction facing : DirectionUtil.FACINGS )
            {
                if( this.getPos().offset( facing )
                    .equals( neighbour ) )
                {
                    this.refreshPeripheral( facing );
                }
            }
        }
    }

    @Override
    public void blockTick()
    {
        if( this.getWorld().isClient )
        {
            return;
        }

        if( this.modemState.pollChanged() )
        {
            this.updateBlockState();
        }

        if( !this.connectionsFormed )
        {
            this.connectionsFormed = true;

            this.connectionsChanged();
            if( this.peripheralAccessAllowed )
            {
                for( Direction facing : DirectionUtil.FACINGS )
                {
                    this.peripherals[facing.ordinal()].attach( this.world, this.getPos(), facing );
                }
                this.updateConnectedPeripherals();
            }
        }
    }

    private void connectionsChanged()
    {
        if( this.getWorld().isClient )
        {
            return;
        }

        World world = this.getWorld();
        BlockPos current = this.getPos();
        for( Direction facing : DirectionUtil.FACINGS )
        {
            BlockPos offset = current.offset( facing );
            if( !world.isChunkLoaded( offset ) )
            {
                continue;
            }

            IWiredElement element = ComputerCraftAPI.getWiredElementAt( world, offset, facing.getOpposite() );
            if( element == null )
            {
                continue;
            }

            this.node.connectTo( element.getNode() );
        }
    }

    private void refreshPeripheral( @Nonnull Direction facing )
    {
        WiredModemLocalPeripheral peripheral = this.peripherals[facing.ordinal()];
        if( this.world != null && !this.isRemoved() && peripheral.attach( this.world, this.getPos(), facing ) )
        {
            this.updateConnectedPeripherals();
        }
    }

    private void updateConnectedPeripherals()
    {
        Map<String, IPeripheral> peripherals = this.getConnectedPeripherals();
        if( peripherals.isEmpty() )
        {
            // If there are no peripherals then disable access and update the display state.
            this.peripheralAccessAllowed = false;
            this.updateBlockState();
        }

        this.node.updatePeripherals( peripherals );
    }

    private Map<String, IPeripheral> getConnectedPeripherals()
    {
        if( !this.peripheralAccessAllowed )
        {
            return Collections.emptyMap();
        }

        Map<String, IPeripheral> peripherals = new HashMap<>( 6 );
        for( WiredModemLocalPeripheral peripheral : this.peripherals )
        {
            peripheral.extendMap( peripherals );
        }
        return peripherals;
    }

    private void updateBlockState()
    {
        BlockState state = this.getCachedState();
        boolean modemOn = this.modemState.isOpen(), peripheralOn = this.peripheralAccessAllowed;
        if( state.get( MODEM_ON ) == modemOn && state.get( PERIPHERAL_ON ) == peripheralOn )
        {
            return;
        }

        this.getWorld().setBlockState( this.getPos(),
            state.with( MODEM_ON, modemOn )
                .with( PERIPHERAL_ON, peripheralOn ) );
    }

    private Set<String> getConnectedPeripheralNames()
    {
        if( !this.peripheralAccessAllowed )
        {
            return Collections.emptySet();
        }

        Set<String> peripherals = new HashSet<>( 6 );
        for( WiredModemLocalPeripheral peripheral : this.peripherals )
        {
            String name = peripheral.getConnectedName();
            if( name != null )
            {
                peripherals.add( name );
            }
        }
        return peripherals;
    }

    private void togglePeripheralAccess()
    {
        if( !this.peripheralAccessAllowed )
        {
            boolean hasAny = false;
            for( Direction facing : DirectionUtil.FACINGS )
            {
                WiredModemLocalPeripheral peripheral = this.peripherals[facing.ordinal()];
                peripheral.attach( this.world, this.getPos(), facing );
                hasAny |= peripheral.hasPeripheral();
            }

            if( !hasAny )
            {
                return;
            }

            this.peripheralAccessAllowed = true;
            this.node.updatePeripherals( this.getConnectedPeripherals() );
        }
        else
        {
            this.peripheralAccessAllowed = false;

            for( WiredModemLocalPeripheral peripheral : this.peripherals )
            {
                peripheral.detach();
            }
            this.node.updatePeripherals( Collections.emptyMap() );
        }

        this.updateBlockState();
    }

    private static void sendPeripheralChanges( PlayerEntity player, String kind, Collection<String> peripherals )
    {
        if( peripherals.isEmpty() )
        {
            return;
        }

        List<String> names = new ArrayList<>( peripherals );
        names.sort( Comparator.naturalOrder() );

        LiteralText base = new LiteralText( "" );
        for( int i = 0; i < names.size(); i++ )
        {
            if( i > 0 )
            {
                base.append( ", " );
            }
            base.append( ChatHelpers.copy( names.get( i ) ) );
        }

        player.sendMessage( new TranslatableText( kind, base ), false );
    }

    @Override
    public void fromTag( @Nonnull BlockState state, @Nonnull CompoundTag nbt )
    {
        super.fromTag( state, nbt );
        this.peripheralAccessAllowed = nbt.getBoolean( NBT_PERIPHERAL_ENABLED );
        for( int i = 0; i < this.peripherals.length; i++ )
        {
            this.peripherals[i].read( nbt, Integer.toString( i ) );
        }
    }

    @Nonnull
    @Override
    public CompoundTag toTag( CompoundTag nbt )
    {
        nbt.putBoolean( NBT_PERIPHERAL_ENABLED, this.peripheralAccessAllowed );
        for( int i = 0; i < this.peripherals.length; i++ )
        {
            this.peripherals[i].write( nbt, Integer.toString( i ) );
        }
        return super.toTag( nbt );
    }

    @Override
    public void markRemoved()
    {
        super.markRemoved();
        this.doRemove();
    }

    @Override
    public void cancelRemoval()
    {
        super.cancelRemoval();
        TickScheduler.schedule( this );
    }

    public IWiredElement getElement()
    {
        return this.element;
    }

    @Nonnull
    @Override
    public IPeripheral getPeripheral( Direction side )
    {
        WiredModemPeripheral peripheral = this.modems[side.ordinal()];
        if( peripheral != null )
        {
            return peripheral;
        }

        WiredModemLocalPeripheral localPeripheral = this.peripherals[side.ordinal()];
        return this.modems[side.ordinal()] = new WiredModemPeripheral( this.modemState, this.element )
        {
            @Nonnull
            @Override
            protected WiredModemLocalPeripheral getLocalPeripheral()
            {
                return localPeripheral;
            }

            @Nonnull
            @Override
            public Vec3d getPosition()
            {
                BlockPos pos = TileWiredModemFull.this.getPos().offset( side );
                return new Vec3d( pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5 );
            }

            @Nonnull
            @Override
            public Object getTarget()
            {
                return TileWiredModemFull.this;
            }
        };
    }

    private static final class FullElement extends WiredModemElement
    {
        private final TileWiredModemFull entity;

        private FullElement( TileWiredModemFull entity )
        {
            this.entity = entity;
        }

        @Override
        protected void detachPeripheral( String name )
        {
            for( int i = 0; i < 6; i++ )
            {
                WiredModemPeripheral modem = this.entity.modems[i];
                if( modem != null )
                {
                    modem.detachPeripheral( name );
                }
            }
        }

        @Override
        protected void attachPeripheral( String name, IPeripheral peripheral )
        {
            for( int i = 0; i < 6; i++ )
            {
                WiredModemPeripheral modem = this.entity.modems[i];
                if( modem != null )
                {
                    modem.attachPeripheral( name, peripheral );
                }
            }
        }

        @Nonnull
        @Override
        public World getWorld()
        {
            return this.entity.getWorld();
        }

        @Nonnull
        @Override
        public Vec3d getPosition()
        {
            BlockPos pos = this.entity.getPos();
            return new Vec3d( pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5 );
        }
    }
}
