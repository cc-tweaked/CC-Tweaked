/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.modem.wired;

import com.google.common.base.Objects;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.network.wired.IWiredElement;
import dan200.computercraft.api.network.wired.IWiredNode;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.command.CommandCopy;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.peripheral.common.IPeripheralTile;
import dan200.computercraft.shared.peripheral.modem.ModemState;
import dan200.computercraft.shared.util.TickScheduler;
import dan200.computercraft.shared.wired.CapabilityWiredElement;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class TileWiredModemFull extends TileGeneric implements IPeripheralTile
{
    private static class FullElement extends WiredModemElement
    {
        private final TileWiredModemFull m_entity;

        private FullElement( TileWiredModemFull m_entity )
        {
            this.m_entity = m_entity;
        }

        @Override
        protected void attachPeripheral( String name, IPeripheral peripheral )
        {
            for( int i = 0; i < 6; i++ )
            {
                WiredModemPeripheral modem = m_entity.m_modems[i];
                if( modem != null ) modem.attachPeripheral( name, peripheral );
            }
        }

        @Override
        protected void detachPeripheral( String name )
        {
            for( int i = 0; i < 6; i++ )
            {
                WiredModemPeripheral modem = m_entity.m_modems[i];
                if( modem != null ) modem.detachPeripheral( name );
            }
        }

        @Nonnull
        @Override
        public World getWorld()
        {
            return m_entity.getWorld();
        }

        @Nonnull
        @Override
        public Vec3d getPosition()
        {
            BlockPos pos = m_entity.getPos();
            return new Vec3d( pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5 );
        }
    }

    private WiredModemPeripheral[] m_modems = new WiredModemPeripheral[6];

    private boolean m_peripheralAccessAllowed = false;
    private WiredModemLocalPeripheral[] m_peripherals = new WiredModemLocalPeripheral[6];

    private boolean m_destroyed = false;
    private boolean m_connectionsFormed = false;

    private final ModemState m_modemState = new ModemState( () -> TickScheduler.schedule( this ) );
    private final WiredModemElement m_element = new FullElement( this );
    private final IWiredNode m_node = m_element.getNode();

    private int m_state = 0;

    public TileWiredModemFull()
    {
        for( int i = 0; i < m_peripherals.length; i++ ) m_peripherals[i] = new WiredModemLocalPeripheral();
    }

    private void remove()
    {
        if( world == null || !world.isRemote )
        {
            m_node.remove();
            m_connectionsFormed = false;
        }
    }

    @Override
    public void destroy()
    {
        if( !m_destroyed )
        {
            m_destroyed = true;
            remove();
        }
        super.destroy();
    }

    @Override
    public void onChunkUnload()
    {
        super.onChunkUnload();
        remove();
    }

    @Override
    public void invalidate()
    {
        super.invalidate();
        remove();
    }

    @Override
    public void onNeighbourChange( @Nonnull BlockPos neighbour )
    {
        onNeighbourTileEntityChange( neighbour );
    }

    @Override
    public void onNeighbourTileEntityChange( @Nonnull BlockPos neighbour )
    {
        if( !world.isRemote && m_peripheralAccessAllowed )
        {
            for( EnumFacing facing : EnumFacing.VALUES )
            {
                if( getPos().offset( facing ).equals( neighbour ) )
                {
                    WiredModemLocalPeripheral peripheral = m_peripherals[facing.ordinal()];
                    if( peripheral.attach( world, getPos(), facing ) ) updateConnectedPeripherals();
                }
            }
        }
    }

    @Override
    public boolean onActivate( EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ )
    {
        if( !getWorld().isRemote )
        {
            // On server, we interacted if a peripheral was found
            Set<String> oldPeriphNames = getConnectedPeripheralNames();
            togglePeripheralAccess();
            Set<String> periphNames = getConnectedPeripheralNames();

            if( !Objects.equal( periphNames, oldPeriphNames ) )
            {
                sendPeripheralChanges( player, "gui.computercraft:wired_modem.peripheral_disconnected", oldPeriphNames );
                sendPeripheralChanges( player, "gui.computercraft:wired_modem.peripheral_connected", periphNames );
            }

            return true;
        }
        else
        {
            // On client, we can't know this, so we assume so to be safe
            // The server will correct us if we're wrong
            return true;
        }
    }

    private static void sendPeripheralChanges( EntityPlayer player, String kind, Collection<String> peripherals )
    {
        if( peripherals.isEmpty() ) return;

        List<String> names = new ArrayList<>( peripherals );
        names.sort( Comparator.naturalOrder() );

        TextComponentString base = new TextComponentString( "" );
        for( int i = 0; i < names.size(); i++ )
        {
            if( i > 0 ) base.appendText( ", " );
            base.appendSibling( CommandCopy.createCopyText( names.get( i ) ) );
        }

        player.sendMessage( new TextComponentTranslation( kind, base ) );
    }

    @Override
    public void readFromNBT( NBTTagCompound nbt )
    {
        super.readFromNBT( nbt );
        m_peripheralAccessAllowed = nbt.getBoolean( "peripheralAccess" );
        for( int i = 0; i < m_peripherals.length; i++ ) m_peripherals[i].readNBT( nbt, "_" + i );
    }

    @Nonnull
    @Override
    public NBTTagCompound writeToNBT( NBTTagCompound nbt )
    {
        nbt = super.writeToNBT( nbt );
        nbt.setBoolean( "peripheralAccess", m_peripheralAccessAllowed );
        for( int i = 0; i < m_peripherals.length; i++ ) m_peripherals[i].writeNBT( nbt, "_" + i );
        return nbt;
    }

    public int getState()
    {
        return m_state;
    }

    private void updateState()
    {
        int state = 0;
        if( m_modemState.isOpen() ) state |= 1;
        if( m_peripheralAccessAllowed ) state |= 2;
        if( state != m_state )
        {
            m_state = state;
            updateBlock();
        }
    }

    @Override
    protected void writeDescription( @Nonnull NBTTagCompound nbt )
    {
        super.writeDescription( nbt );
        nbt.setInteger( "state", m_state );
    }

    @Override
    public final void readDescription( @Nonnull NBTTagCompound nbt )
    {
        super.readDescription( nbt );
        m_state = nbt.getInteger( "state" );
        updateBlock();
    }

    @Override
    public void onLoad()
    {
        super.onLoad();
        if( !world.isRemote ) world.scheduleUpdate( pos, getBlockType(), 0 );
    }

    @Override
    protected void updateTick()
    {
        if( !getWorld().isRemote )
        {
            if( m_modemState.pollChanged() ) updateState();

            if( !m_connectionsFormed )
            {
                m_connectionsFormed = true;

                connectionsChanged();
                if( m_peripheralAccessAllowed )
                {
                    for( EnumFacing facing : EnumFacing.VALUES )
                    {
                        m_peripherals[facing.ordinal()].attach( world, getPos(), facing );
                    }
                    updateConnectedPeripherals();
                }
            }
        }
    }

    private void connectionsChanged()
    {
        if( getWorld().isRemote ) return;

        World world = getWorld();
        BlockPos current = getPos();
        for( EnumFacing facing : EnumFacing.VALUES )
        {
            BlockPos offset = current.offset( facing );
            if( !world.isBlockLoaded( offset ) ) continue;

            IWiredElement element = ComputerCraftAPI.getWiredElementAt( world, offset, facing.getOpposite() );
            if( element == null ) continue;

            // If we can connect to it then do so
            m_node.connectTo( element.getNode() );
        }
    }

    private void togglePeripheralAccess()
    {
        if( !m_peripheralAccessAllowed )
        {
            boolean hasAny = false;
            for( EnumFacing facing : EnumFacing.VALUES )
            {
                WiredModemLocalPeripheral peripheral = m_peripherals[facing.ordinal()];
                peripheral.attach( world, getPos(), facing );
                hasAny |= peripheral.hasPeripheral();
            }

            if( !hasAny ) return;

            m_peripheralAccessAllowed = true;
            m_node.updatePeripherals( getConnectedPeripherals() );
        }
        else
        {
            m_peripheralAccessAllowed = false;

            for( WiredModemLocalPeripheral peripheral : m_peripherals ) peripheral.detach();
            m_node.updatePeripherals( Collections.emptyMap() );
        }

        updateState();
    }

    private Set<String> getConnectedPeripheralNames()
    {
        if( !m_peripheralAccessAllowed ) return Collections.emptySet();

        Set<String> peripherals = new HashSet<>( 6 );
        for( WiredModemLocalPeripheral m_peripheral : m_peripherals )
        {
            String name = m_peripheral.getConnectedName();
            if( name != null ) peripherals.add( name );
        }
        return peripherals;
    }

    private Map<String, IPeripheral> getConnectedPeripherals()
    {
        if( !m_peripheralAccessAllowed ) return Collections.emptyMap();

        Map<String, IPeripheral> peripherals = new HashMap<>( 6 );
        for( WiredModemLocalPeripheral m_peripheral : m_peripherals ) m_peripheral.extendMap( peripherals );
        return peripherals;
    }

    private void updateConnectedPeripherals()
    {
        Map<String, IPeripheral> peripherals = getConnectedPeripherals();
        if( peripherals.isEmpty() )
        {
            // If there are no peripherals then disable access and update the display state.
            m_peripheralAccessAllowed = false;
            updateState();
        }

        m_node.updatePeripherals( peripherals );
    }

    // IWiredElementTile

    @Override
    public boolean hasCapability( @Nonnull Capability<?> capability, @Nullable EnumFacing facing )
    {
        if( capability == CapabilityWiredElement.CAPABILITY ) return !m_destroyed;
        return super.hasCapability( capability, facing );
    }

    @Nullable
    @Override
    public <T> T getCapability( @Nonnull Capability<T> capability, @Nullable EnumFacing facing )
    {
        if( capability == CapabilityWiredElement.CAPABILITY )
        {
            if( m_destroyed ) return null;
            return CapabilityWiredElement.CAPABILITY.cast( m_element );
        }

        return super.getCapability( capability, facing );
    }

    @Override
    public IPeripheral getPeripheral( EnumFacing side )
    {
        if( m_destroyed ) return null;

        WiredModemPeripheral peripheral = m_modems[side.ordinal()];
        if( peripheral == null )
        {
            WiredModemLocalPeripheral localPeripheral = m_peripherals[side.ordinal()];
            peripheral = m_modems[side.ordinal()] = new WiredModemPeripheral( m_modemState, m_element )
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
            };
        }
        return peripheral;
    }
}
