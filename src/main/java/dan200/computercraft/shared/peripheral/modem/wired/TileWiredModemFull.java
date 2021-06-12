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
    private final IWiredNode node = element.getNode();
    private boolean peripheralAccessAllowed = false;
    private boolean destroyed = false;
    private boolean connectionsFormed = false;

    public TileWiredModemFull( BlockEntityType<TileWiredModemFull> type )
    {
        super( type );
        for( int i = 0; i < peripherals.length; i++ )
        {
            Direction facing = Direction.byId( i );
            peripherals[i] = new WiredModemLocalPeripheral();
        }
    }

    @Override
    public void destroy()
    {
        if( !destroyed )
        {
            destroyed = true;
            doRemove();
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
        if( world == null || !world.isClient )
        {
            node.remove();
            connectionsFormed = false;
        }
    }

    @Nonnull
    @Override
    public ActionResult onActivate( PlayerEntity player, Hand hand, BlockHitResult hit )
    {
        if( getWorld().isClient )
        {
            return ActionResult.SUCCESS;
        }

        // On server, we interacted if a peripheral was found
        Set<String> oldPeriphNames = getConnectedPeripheralNames();
        togglePeripheralAccess();
        Set<String> periphNames = getConnectedPeripheralNames();

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
        onNeighbourTileEntityChange( neighbour );
    }

    @Override
    public void onNeighbourTileEntityChange( @Nonnull BlockPos neighbour )
    {
        if( !world.isClient && peripheralAccessAllowed )
        {
            for( Direction facing : DirectionUtil.FACINGS )
            {
                if( getPos().offset( facing )
                    .equals( neighbour ) )
                {
                    refreshPeripheral( facing );
                }
            }
        }
    }

    @Override
    public void blockTick()
    {
        if( getWorld().isClient )
        {
            return;
        }

        if( modemState.pollChanged() )
        {
            updateBlockState();
        }

        if( !connectionsFormed )
        {
            connectionsFormed = true;

            connectionsChanged();
            if( peripheralAccessAllowed )
            {
                for( Direction facing : DirectionUtil.FACINGS )
                {
                    peripherals[facing.ordinal()].attach( world, getPos(), facing );
                }
                updateConnectedPeripherals();
            }
        }
    }

    private void connectionsChanged()
    {
        if( getWorld().isClient )
        {
            return;
        }

        World world = getWorld();
        BlockPos current = getPos();
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

            node.connectTo( element.getNode() );
        }
    }

    private void refreshPeripheral( @Nonnull Direction facing )
    {
        WiredModemLocalPeripheral peripheral = peripherals[facing.ordinal()];
        if( world != null && !isRemoved() && peripheral.attach( world, getPos(), facing ) )
        {
            updateConnectedPeripherals();
        }
    }

    private void updateConnectedPeripherals()
    {
        Map<String, IPeripheral> peripherals = getConnectedPeripherals();
        if( peripherals.isEmpty() )
        {
            // If there are no peripherals then disable access and update the display state.
            peripheralAccessAllowed = false;
            updateBlockState();
        }

        node.updatePeripherals( peripherals );
    }

    private Map<String, IPeripheral> getConnectedPeripherals()
    {
        if( !peripheralAccessAllowed )
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
        BlockState state = getCachedState();
        boolean modemOn = modemState.isOpen(), peripheralOn = peripheralAccessAllowed;
        if( state.get( MODEM_ON ) == modemOn && state.get( PERIPHERAL_ON ) == peripheralOn )
        {
            return;
        }

        getWorld().setBlockState( getPos(),
            state.with( MODEM_ON, modemOn )
                .with( PERIPHERAL_ON, peripheralOn ) );
    }

    private Set<String> getConnectedPeripheralNames()
    {
        if( !peripheralAccessAllowed )
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
        if( !peripheralAccessAllowed )
        {
            boolean hasAny = false;
            for( Direction facing : DirectionUtil.FACINGS )
            {
                WiredModemLocalPeripheral peripheral = peripherals[facing.ordinal()];
                peripheral.attach( world, getPos(), facing );
                hasAny |= peripheral.hasPeripheral();
            }

            if( !hasAny )
            {
                return;
            }

            peripheralAccessAllowed = true;
            node.updatePeripherals( getConnectedPeripherals() );
        }
        else
        {
            peripheralAccessAllowed = false;

            for( WiredModemLocalPeripheral peripheral : peripherals )
            {
                peripheral.detach();
            }
            node.updatePeripherals( Collections.emptyMap() );
        }

        updateBlockState();
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
        peripheralAccessAllowed = nbt.getBoolean( NBT_PERIPHERAL_ENABLED );
        for( int i = 0; i < peripherals.length; i++ )
        {
            peripherals[i].read( nbt, Integer.toString( i ) );
        }
    }

    @Nonnull
    @Override
    public CompoundTag toTag( CompoundTag nbt )
    {
        nbt.putBoolean( NBT_PERIPHERAL_ENABLED, peripheralAccessAllowed );
        for( int i = 0; i < peripherals.length; i++ )
        {
            peripherals[i].write( nbt, Integer.toString( i ) );
        }
        return super.toTag( nbt );
    }

    @Override
    public void markRemoved()
    {
        super.markRemoved();
        doRemove();
    }

    @Override
    public void cancelRemoval()
    {
        super.cancelRemoval();
        TickScheduler.schedule( this );
    }

    public IWiredElement getElement()
    {
        return element;
    }

    @Nonnull
    @Override
    public IPeripheral getPeripheral( Direction side )
    {
        WiredModemPeripheral peripheral = modems[side.ordinal()];
        if( peripheral != null )
        {
            return peripheral;
        }

        WiredModemLocalPeripheral localPeripheral = peripherals[side.ordinal()];
        return modems[side.ordinal()] = new WiredModemPeripheral( modemState, element )
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
                BlockPos pos = getPos().offset( side );
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
                WiredModemPeripheral modem = entity.modems[i];
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
                WiredModemPeripheral modem = entity.modems[i];
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
            return entity.getWorld();
        }

        @Nonnull
        @Override
        public Vec3d getPosition()
        {
            BlockPos pos = entity.getPos();
            return new Vec3d( pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5 );
        }
    }
}
