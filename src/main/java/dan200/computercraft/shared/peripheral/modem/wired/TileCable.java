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
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.peripheral.common.IPeripheralTile;
import dan200.computercraft.shared.wired.CapabilityWiredElement;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;

import static dan200.computercraft.shared.peripheral.modem.wired.BlockCable.canConnectIn;
import static dan200.computercraft.shared.peripheral.modem.wired.BlockCable.isCable;
import static dan200.computercraft.shared.peripheral.modem.wired.BlockCable.isModem;

public class TileCable extends TileGeneric implements IPeripheralTile, ITickable
{
    private static class CableElement extends WiredModemElement
    {
        private final TileCable tile;

        private CableElement( TileCable tile )
        {
            this.tile = tile;
        }

        @Nonnull
        @Override
        public World getWorld()
        {
            return tile.getWorld();
        }

        @Nonnull
        @Override
        public Vec3d getPosition()
        {
            BlockPos pos = tile.getPos();
            return new Vec3d( pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5 );
        }

        @Override
        protected void attachPeripheral( String name, IPeripheral peripheral )
        {
            tile.modem.attachPeripheral( name, peripheral );
        }

        @Override
        protected void detachPeripheral( String name )
        {
            tile.modem.detachPeripheral( name );
        }
    }

    // Members

    private boolean m_peripheralAccessAllowed;
    private WiredModemLocalPeripheral m_peripheral = new WiredModemLocalPeripheral();

    private boolean m_destroyed = false;

    private boolean m_connectionsFormed = false;

    private boolean modemOn = false;
    private boolean peripheralOn = false;

    private WiredModemElement cable = new CableElement( this );
    private IWiredNode node = cable.getNode();
    private WiredModemPeripheral modem = new WiredModemPeripheral( cable )
    {
        @Nonnull
        @Override
        protected WiredModemLocalPeripheral getLocalPeripheral()
        {
            return m_peripheral;
        }

        @Nonnull
        @Override
        public Vec3d getPosition()
        {
            BlockPos pos = getPos().offset( getCachedSide() );
            return new Vec3d( pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5 );
        }
    };

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
    public void onLoad()
    {
        super.onLoad();
        getBlockState();
    }

    protected EnumFacing getSide()
    {
        return getBlockState().getValue( BlockCable.MODEM ).getFacing();
    }

    private EnumFacing getCachedSide()
    {
        return getBlockStateSafe().getValue( BlockCable.MODEM ).getFacing();
    }

    @Override
    public void onNeighbourChange()
    {
        EnumFacing side = getSide();
        if( !getWorld().isSideSolid( getPos().offset( side ), side.getOpposite() ) )
        {
            IBlockState state = getBlockState();
            if( isModem( state ) )
            {
                if( isCable( state ) )
                {
                    // Drop the modem and convert to cable
                    Block.spawnAsEntity( getWorld(), getPos(), new ItemStack( ComputerCraft.Items.wiredModem ) );
                    setBlockState( state.withProperty( BlockCable.MODEM, BlockCableModemVariant.None ) );
                    modemChanged();
                    connectionsChanged();
                }
                else
                {
                    // Drop everything and remove block
                    // This'll call #destroy(), so we don't need to reset the network here.
                    state.getBlock().dropBlockAsItem( getWorld(), getPos(), state, 1 );
                    getWorld().setBlockToAir( getPos() );
                }

                return;
            }
        }

        if( !world.isRemote && m_peripheralAccessAllowed )
        {
            if( m_peripheral.attach( world, getPos(), side ) ) updateConnectedPeripherals();
        }
    }

    @Override
    public void onNeighbourTileEntityChange( @Nonnull BlockPos neighbour )
    {
        super.onNeighbourTileEntityChange( neighbour );
        if( !world.isRemote && m_peripheralAccessAllowed )
        {
            EnumFacing facing = getSide();
            if( getPos().offset( facing ).equals( neighbour ) )
            {
                if( m_peripheral.attach( world, getPos(), facing ) ) updateConnectedPeripherals();
            }
        }
    }

    @Override
    public boolean onActivate( EntityPlayer player, EnumFacing side, float hitX, float hitY, float hitZ )
    {
        IBlockState state = getBlockState();
        if( isCable( state ) && isModem( state ) && !player.isSneaking() )
        {
            if( !getWorld().isRemote )
            {
                // On server, we interacted if a peripheral was found
                String oldPeriphName = m_peripheral.getConnectedName();
                togglePeripheralAccess();
                String periphName = m_peripheral.getConnectedName();

                if( !Objects.equal( periphName, oldPeriphName ) )
                {
                    if( oldPeriphName != null )
                    {
                        player.sendMessage(
                            new TextComponentTranslation( "gui.computercraft:wired_modem.peripheral_disconnected", oldPeriphName )
                        );
                    }
                    if( periphName != null )
                    {
                        player.sendMessage(
                            new TextComponentTranslation( "gui.computercraft:wired_modem.peripheral_connected", periphName )
                        );
                    }
                    return true;
                }
            }
            else
            {
                // On client, we can't know this, so we assume so to be safe
                // The server will correct us if we're wrong
                return true;
            }
        }
        return false;
    }

    @Override
    public void readFromNBT( NBTTagCompound nbttagcompound )
    {
        // Read properties
        super.readFromNBT( nbttagcompound );
        m_peripheralAccessAllowed = nbttagcompound.getBoolean( "peripheralAccess" );
        m_peripheral.readNBT( nbttagcompound, "" );
    }

    @Nonnull
    @Override
    public NBTTagCompound writeToNBT( NBTTagCompound nbttagcompound )
    {
        // Write properties
        nbttagcompound = super.writeToNBT( nbttagcompound );
        nbttagcompound.setBoolean( "peripheralAccess", m_peripheralAccessAllowed );
        m_peripheral.writeNBT( nbttagcompound, "" );
        return nbttagcompound;
    }

    protected void updateVisualState()
    {
        boolean modemOn = modem != null && modem.isActive();
        boolean peripheralOn = m_peripheralAccessAllowed;

        if( modemOn != this.modemOn || peripheralOn != this.peripheralOn )
        {
            this.modemOn = modemOn;
            this.peripheralOn = peripheralOn;
            updateBlock();
        }
    }

    @Override
    protected void writeDescription( @Nonnull NBTTagCompound tag )
    {
        super.writeDescription( tag );
        tag.setBoolean( "modem_on", modemOn );
        tag.setBoolean( "peripheral_on", peripheralOn );
    }

    @Override
    public final void readDescription( @Nonnull NBTTagCompound tag )
    {
        super.readDescription( tag );
        modemOn = tag.getBoolean( "modem_on" );
        peripheralOn = tag.getBoolean( "peripheral_on" );

        updateBlock();
    }

    @Override
    public void update()
    {
        if( !getWorld().isRemote )
        {
            getBlockState(); // Ensure the block state is up-to-date for modems
            if( !m_connectionsFormed )
            {
                m_connectionsFormed = true;

                connectionsChanged();
                if( m_peripheralAccessAllowed )
                {
                    m_peripheral.attach( world, pos, getSide() );
                    updateConnectedPeripherals();
                }
            }
        }
    }

    public void connectionsChanged()
    {
        if( getWorld().isRemote ) return;

        IBlockState state = getBlockState();
        World world = getWorld();
        BlockPos current = getPos();
        for( EnumFacing facing : EnumFacing.VALUES )
        {
            BlockPos offset = current.offset( facing );
            if( !world.isBlockLoaded( offset ) ) continue;

            IWiredElement element = ComputerCraft.getWiredElementAt( world, offset, facing.getOpposite() );
            if( element == null ) continue;

            if( canConnectIn( state, facing ) )
            {
                // If we can connect to it then do so
                node.connectTo( element.getNode() );
            }
            else if( node.getNetwork() == element.getNode().getNetwork() )
            {
                // Otherwise if we're on the same network then attempt to void it.
                node.disconnectFrom( element.getNode() );
            }
        }
    }

    public void modemChanged()
    {
        if( getWorld().isRemote ) return;

        IBlockState state = getBlockState();
        if( m_peripheralAccessAllowed && (!isModem( state ) || !isCable( state )) )
        {
            m_peripheralAccessAllowed = false;
            m_peripheral.detach();
            node.updatePeripherals( Collections.emptyMap() );
            markDirty();
            updateVisualState();
        }
    }

    // private stuff
    private void togglePeripheralAccess()
    {
        if( !m_peripheralAccessAllowed )
        {
            m_peripheral.attach( world, getPos(), getSide() );
            if( !m_peripheral.hasPeripheral() ) return;

            m_peripheralAccessAllowed = true;
            node.updatePeripherals( m_peripheral.toMap() );
        }
        else
        {
            m_peripheral.detach();

            m_peripheralAccessAllowed = false;
            node.updatePeripherals( Collections.emptyMap() );
        }

        updateVisualState();
    }

    private void updateConnectedPeripherals()
    {
        Map<String, IPeripheral> peripherals = m_peripheral.toMap();
        if( peripherals.isEmpty() )
        {
            // If there are no peripherals then disable access and update the display state.
            m_peripheralAccessAllowed = false;
            updateVisualState();
        }

        node.updatePeripherals( peripherals );
    }

    @Override
    public boolean canRenderBreaking()
    {
        return true;
    }

    public boolean isModemOn()
    {
        return modemOn;
    }

    public boolean isPeripheralOn()
    {
        return peripheralOn;
    }

    // IWiredElement capability

    @Override
    public boolean hasCapability( @Nonnull Capability<?> capability, @Nullable EnumFacing facing )
    {
        if( capability == CapabilityWiredElement.CAPABILITY ) return canConnectIn( getBlockState(), facing );
        return super.hasCapability( capability, facing );
    }

    @Nullable
    @Override
    public <T> T getCapability( @Nonnull Capability<T> capability, @Nullable EnumFacing facing )
    {
        if( capability == CapabilityWiredElement.CAPABILITY )
        {
            return canConnectIn( getBlockState(), facing ) ? CapabilityWiredElement.CAPABILITY.cast( cable ) : null;
        }

        return super.getCapability( capability, facing );
    }

    // IPeripheralTile

    @Override
    public IPeripheral getPeripheral( EnumFacing side )
    {
        return isModem( getBlockState() ) ? side == getSide() ? modem : null : null;
    }
}
