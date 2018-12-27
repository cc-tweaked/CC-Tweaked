/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2018. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.modem.wired;

import com.google.common.base.Objects;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.network.wired.IWiredElement;
import dan200.computercraft.api.network.wired.IWiredNode;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.command.CommandCopy;
import dan200.computercraft.shared.peripheral.common.TilePeripheralBase;
import dan200.computercraft.shared.peripheral.modem.ModemState;
import dan200.computercraft.shared.wired.CapabilityWiredElement;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class TileWiredModemFull extends TilePeripheralBase
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

    private final ModemState m_modemState = new ModemState();
    private final WiredModemElement m_element = new FullElement( this );
    private final IWiredNode m_node = m_element.getNode();

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
    public EnumFacing getDirection()
    {
        return EnumFacing.NORTH;
    }

    @Override
    public void setDirection( EnumFacing dir )
    {
    }

    @Override
    public void onNeighbourChange()
    {
        if( !world.isRemote && m_peripheralAccessAllowed )
        {
            boolean hasChanged = false;
            for( EnumFacing facing : EnumFacing.VALUES )
            {
                hasChanged |= m_peripherals[facing.ordinal()].attach( world, getPos(), facing );
            }

            if( hasChanged ) updateConnectedPeripherals();
        }
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

    @Nonnull
    @Override
    public AxisAlignedBB getBounds()
    {
        return BlockCable.FULL_BLOCK_AABB;
    }

    @Override
    public boolean onActivate( EntityPlayer player, EnumFacing side, float hitX, float hitY, float hitZ )
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
    public void readFromNBT( NBTTagCompound tag )
    {
        super.readFromNBT( tag );
        m_peripheralAccessAllowed = tag.getBoolean( "peripheralAccess" );
        for( int i = 0; i < m_peripherals.length; i++ ) m_peripherals[i].readNBT( tag, "_" + i );
    }

    @Nonnull
    @Override
    public NBTTagCompound writeToNBT( NBTTagCompound tag )
    {
        tag = super.writeToNBT( tag );
        tag.setBoolean( "peripheralAccess", m_peripheralAccessAllowed );
        for( int i = 0; i < m_peripherals.length; i++ ) m_peripherals[i].writeNBT( tag, "_" + i );
        return tag;
    }

    protected void updateAnim()
    {
        int anim = 0;
        if( m_modemState.isOpen() ) anim |= 1;
        if( m_peripheralAccessAllowed ) anim |= 2;
        setAnim( anim );
    }

    @Override
    public final void readDescription( @Nonnull NBTTagCompound tag )
    {
        super.readDescription( tag );
        updateBlock();
    }

    @Override
    public void update()
    {
        if( !getWorld().isRemote )
        {
            if( m_modemState.pollChanged() ) updateAnim();

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

        super.update();
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

            IWiredElement element = ComputerCraft.getWiredElementAt( world, offset, facing.getOpposite() );
            if( element == null ) continue;

            // If we can connect to it then do so
            m_node.connectTo( element.getNode() );
        }
    }

    // private stuff
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

        updateAnim();
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
            updateAnim();
        }

        m_node.updatePeripherals( peripherals );
    }

    // IWiredElementTile

    @Override
    public boolean hasCapability( @Nonnull Capability<?> capability, @Nullable EnumFacing facing )
    {
        return capability == CapabilityWiredElement.CAPABILITY || super.hasCapability( capability, facing );
    }

    @Nullable
    @Override
    public <T> T getCapability( @Nonnull Capability<T> capability, @Nullable EnumFacing facing )
    {
        if( capability == CapabilityWiredElement.CAPABILITY )
        {
            return CapabilityWiredElement.CAPABILITY.cast( m_element );
        }

        return super.getCapability( capability, facing );
    }

    // IPeripheralTile

    @Override
    public IPeripheral getPeripheral( EnumFacing side )
    {
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
