/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.modem;

import com.google.common.base.Objects;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.network.wired.IWiredElement;
import dan200.computercraft.api.network.wired.IWiredNode;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.peripheral.common.BlockCable;
import dan200.computercraft.shared.peripheral.common.TilePeripheralBase;
import dan200.computercraft.shared.util.IDAssigner;
import dan200.computercraft.shared.wired.CapabilityWiredElement;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
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
                if( modem != null && !name.equals( m_entity.getCachedPeripheralName( EnumFacing.VALUES[i] ) ) )
                {
                    modem.attachPeripheral( name, peripheral );
                }
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

        @Nonnull
        @Override
        public Map<String, IPeripheral> getPeripherals()
        {
            return m_entity.getPeripherals();
        }
    }

    private WiredModemPeripheral[] m_modems = new WiredModemPeripheral[6];

    private boolean m_peripheralAccessAllowed = false;
    private int[] m_attachedPeripheralIDs = new int[6];
    private String[] m_attachedPeripheralTypes = new String[6];

    private boolean m_destroyed = false;
    private boolean m_connectionsFormed = false;

    private final WiredModemElement m_element = new FullElement( this );
    private final IWiredNode node = m_element.getNode();

    public TileWiredModemFull()
    {
        Arrays.fill( m_attachedPeripheralIDs, -1 );
    }

    private void remove()
    {
        if( world == null || !world.isRemote )
        {
            node.remove();
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
            Map<String, IPeripheral> updated = getPeripherals();

            if( updated.isEmpty() )
            {
                // If there are no peripherals then disable access and update the display state.
                m_peripheralAccessAllowed = false;
                updateAnim();
            }

            // Always invalidate the node: it's more accurate than checking if the peripherals
            // have changed
            node.invalidate();
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
            Set<String> oldPeriphName = getPeripherals().keySet();
            togglePeripheralAccess();
            Set<String> periphName = getPeripherals().keySet();

            if( !Objects.equal( periphName, oldPeriphName ) )
            {
                if( !oldPeriphName.isEmpty() )
                {
                    List<String> names = new ArrayList<>( oldPeriphName );
                    names.sort( Comparator.naturalOrder() );

                    player.sendMessage(
                        new TextComponentTranslation( "gui.computercraft:wired_modem.peripheral_disconnected", String.join( ", ", names ) )
                    );
                }
                if( !periphName.isEmpty() )
                {
                    List<String> names = new ArrayList<>( periphName );
                    names.sort( Comparator.naturalOrder() );
                    player.sendMessage(
                        new TextComponentTranslation( "gui.computercraft:wired_modem.peripheral_connected", String.join( ", ", names ) )
                    );
                }
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

    @Override
    public void readFromNBT( NBTTagCompound tag )
    {
        super.readFromNBT( tag );
        m_peripheralAccessAllowed = tag.getBoolean( "peripheralAccess" );
        for( int i = 0; i < m_attachedPeripheralIDs.length; i++ )
        {
            if( tag.hasKey( "peripheralID_" + i, Constants.NBT.TAG_ANY_NUMERIC ) )
            {
                m_attachedPeripheralIDs[i] = tag.getInteger( "peripheralID_" + i );
            }
            if( tag.hasKey( "peripheralType_" + i, Constants.NBT.TAG_STRING ) )
            {
                m_attachedPeripheralTypes[i] = tag.getString( "peripheralType_" + i );
            }
        }
    }

    @Nonnull
    @Override
    public NBTTagCompound writeToNBT( NBTTagCompound tag )
    {
        tag = super.writeToNBT( tag );
        tag.setBoolean( "peripheralAccess", m_peripheralAccessAllowed );
        for( int i = 0; i < m_attachedPeripheralIDs.length; i++ )
        {
            if( m_attachedPeripheralIDs[i] >= 0 )
            {
                tag.setInteger( "peripheralID_" + i, m_attachedPeripheralIDs[i] );
            }
            if( m_attachedPeripheralTypes[i] != null )
            {
                tag.setString( "peripheralType_" + i, m_attachedPeripheralTypes[i] );
            }
        }
        return tag;
    }

    protected void updateAnim()
    {
        int anim = 0;
        for( WiredModemPeripheral modem : m_modems )
        {
            if( modem != null && modem.isActive() )
            {
                anim += 1;
                break;
            }
        }

        if( m_peripheralAccessAllowed )
        {
            anim += 2;
        }
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
            boolean changed = false;
            for( WiredModemPeripheral peripheral : m_modems )
            {
                if( peripheral != null && peripheral.pollChanged() ) changed = true;
            }
            if( changed ) updateAnim();

            if( !m_connectionsFormed )
            {
                networkChanged();
                m_connectionsFormed = true;
            }
        }

        super.update();
    }

    private void networkChanged()
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
            node.connectTo( element.getNode() );
        }

        node.invalidate();
    }

    // private stuff
    private void togglePeripheralAccess()
    {
        if( !m_peripheralAccessAllowed )
        {
            m_peripheralAccessAllowed = true;
            if( getPeripherals().isEmpty() )
            {
                m_peripheralAccessAllowed = false;
                return;
            }
        }
        else
        {
            m_peripheralAccessAllowed = false;
        }

        updateAnim();
        node.invalidate();
    }

    @Nonnull
    private Map<String, IPeripheral> getPeripherals()
    {
        if( !m_peripheralAccessAllowed ) return Collections.emptyMap();

        Map<String, IPeripheral> peripherals = new HashMap<>( 6 );
        for( EnumFacing facing : EnumFacing.VALUES )
        {
            BlockPos neighbour = getPos().offset( facing );
            IPeripheral peripheral = TileCable.getPeripheral( getWorld(), neighbour, facing.getOpposite() );
            if( peripheral != null && !(peripheral instanceof WiredModemPeripheral) )
            {
                String type = peripheral.getType();
                int id = m_attachedPeripheralIDs[facing.ordinal()];
                String oldType = m_attachedPeripheralTypes[facing.ordinal()];
                if( id < 0 || !type.equals( oldType ) )
                {
                    m_attachedPeripheralTypes[facing.ordinal()] = type;
                    id = m_attachedPeripheralIDs[facing.ordinal()] = IDAssigner.getNextIDFromFile( new File(
                        ComputerCraft.getWorldDir( getWorld() ),
                        "computer/lastid_" + type + ".txt"
                    ) );
                }

                peripherals.put( type + "_" + id, peripheral );
            }
        }

        return peripherals;
    }

    private String getCachedPeripheralName( EnumFacing facing )
    {
        if( !m_peripheralAccessAllowed ) return null;

        int id = m_attachedPeripheralIDs[facing.ordinal()];
        String type = m_attachedPeripheralTypes[facing.ordinal()];
        return id < 0 || type == null ? null : type + "_" + id;
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
            peripheral = m_modems[side.ordinal()] = new WiredModemPeripheral( m_element )
            {
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
