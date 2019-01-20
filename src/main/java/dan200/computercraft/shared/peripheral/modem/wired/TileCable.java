/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.modem.wired;

import com.google.common.base.Objects;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.network.wired.IWiredElement;
import dan200.computercraft.api.network.wired.IWiredNode;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.command.CommandCopy;
import dan200.computercraft.shared.common.BlockGeneric;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.peripheral.PeripheralType;
import dan200.computercraft.shared.peripheral.common.IPeripheralTile;
import dan200.computercraft.shared.peripheral.common.PeripheralItemFactory;
import dan200.computercraft.shared.peripheral.modem.ModemState;
import dan200.computercraft.shared.util.TickScheduler;
import dan200.computercraft.shared.wired.CapabilityWiredElement;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;

public class TileCable extends TileGeneric implements IPeripheralTile
{
    private class CableElement extends WiredModemElement
    {
        @Nonnull
        @Override
        public World getWorld()
        {
            return TileCable.this.getWorld();
        }

        @Nonnull
        @Override
        public Vec3d getPosition()
        {
            BlockPos pos = TileCable.this.getPos();
            return new Vec3d( pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5 );
        }

        @Override
        protected void attachPeripheral( String name, IPeripheral peripheral )
        {
            m_modem.attachPeripheral( name, peripheral );
        }

        @Override
        protected void detachPeripheral( String name )
        {
            m_modem.detachPeripheral( name );
        }
    }

    // Members

    private boolean m_peripheralAccessAllowed;
    private WiredModemLocalPeripheral m_peripheral = new WiredModemLocalPeripheral();

    private boolean m_destroyed = false;

    private EnumFacing modemDirection;
    private boolean hasModemDirection = false;

    private boolean m_connectionsFormed = false;

    private final WiredModemElement m_cable = new CableElement();
    private final IWiredNode m_node = m_cable.getNode();
    private final WiredModemPeripheral m_modem = new WiredModemPeripheral(
        new ModemState( () -> TickScheduler.schedule( this ) ),
        m_cable
    )
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
            BlockPos pos = getPos().offset( modemDirection );
            return new Vec3d( pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5 );
        }
    };

    private int m_state = 0;

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
            m_modem.destroy();
            remove();
        }
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
        if( !world.isRemote )
        {
            updateDirection();
            world.scheduleUpdate( pos, getBlockType(), 0 );
        }
    }

    @Override
    public void updateContainingBlockInfo()
    {
        super.updateContainingBlockInfo();
        hasModemDirection = false;
        if( !world.isRemote ) world.scheduleUpdate( pos, getBlockType(), 0 );
    }

    private void updateDirection()
    {
        if( !hasModemDirection )
        {
            hasModemDirection = true;
            modemDirection = getDirection();
        }
    }

    private EnumFacing getDirection()
    {
        IBlockState state = getBlockState();
        EnumFacing facing = state.getValue( BlockCable.Properties.MODEM ).getFacing();
        return facing != null ? facing : EnumFacing.NORTH;
    }

    @Override
    public void getDroppedItems( @Nonnull NonNullList<ItemStack> drops, boolean creative )
    {
        if( !creative )
        {
            PeripheralType type = getPeripheralType();
            switch( type )
            {
                case Cable:
                case WiredModem:
                {
                    drops.add( PeripheralItemFactory.create( type, null, 1 ) );
                    break;
                }
                case WiredModemWithCable:
                {
                    drops.add( PeripheralItemFactory.create( PeripheralType.WiredModem, null, 1 ) );
                    drops.add( PeripheralItemFactory.create( PeripheralType.Cable, null, 1 ) );
                    break;
                }
            }
        }
    }

    @Override
    public void onNeighbourChange()
    {
        EnumFacing dir = getDirection();
        if( !getWorld().isSideSolid(
            getPos().offset( dir ),
            dir.getOpposite()
        ) )
        {
            switch( getPeripheralType() )
            {
                case WiredModem:
                {
                    // Drop everything and remove block
                    ((BlockGeneric) getBlockType()).dropAllItems( getWorld(), getPos(), false );
                    getWorld().setBlockToAir( getPos() );

                    // This'll call #destroy(), so we don't need to reset the network here.
                    return;
                }
                case WiredModemWithCable:
                {
                    // Drop the modem and convert to cable
                    ((BlockGeneric) getBlockType()).dropItem( getWorld(), getPos(), PeripheralItemFactory.create( PeripheralType.WiredModem, null, 1 ) );
                    setBlockState( getBlockState().withProperty( BlockCable.Properties.MODEM, BlockCableModemVariant.None ) );
                    modemChanged();
                    connectionsChanged();

                    return;
                }
            }
        }

        if( !world.isRemote && m_peripheralAccessAllowed )
        {
            if( m_peripheral.attach( world, getPos(), dir ) ) updateConnectedPeripherals();
        }
    }

    @Override
    public void onNeighbourTileEntityChange( @Nonnull BlockPos neighbour )
    {
        super.onNeighbourTileEntityChange( neighbour );
        if( !world.isRemote && m_peripheralAccessAllowed )
        {
            EnumFacing facing = getDirection();
            if( getPos().offset( facing ).equals( neighbour ) )
            {
                if( m_peripheral.attach( world, getPos(), facing ) ) updateConnectedPeripherals();
            }
        }
    }

    @Override
    public boolean onActivate( EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ )
    {
        if( getPeripheralType() == PeripheralType.WiredModemWithCable && !player.isSneaking() )
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
                            new TextComponentTranslation( "gui.computercraft:wired_modem.peripheral_disconnected",
                                CommandCopy.createCopyText( oldPeriphName ) )
                        );
                    }
                    if( periphName != null )
                    {
                        player.sendMessage(
                            new TextComponentTranslation( "gui.computercraft:wired_modem.peripheral_connected",
                                CommandCopy.createCopyText( periphName ) )
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
    public void readFromNBT( NBTTagCompound nbt )
    {
        // Read properties
        super.readFromNBT( nbt );
        m_peripheralAccessAllowed = nbt.getBoolean( "peripheralAccess" );
        m_peripheral.readNBT( nbt, "" );
    }

    @Nonnull
    @Override
    public NBTTagCompound writeToNBT( NBTTagCompound nbt )
    {
        // Write properties
        nbt = super.writeToNBT( nbt );
        nbt.setBoolean( "peripheralAccess", m_peripheralAccessAllowed );
        m_peripheral.writeNBT( nbt, "" );
        return nbt;
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

    public int getState()
    {
        return m_state;
    }

    private void updateState()
    {
        int state = 0;
        if( m_modem.getModemState().isOpen() ) state |= 1;
        if( m_peripheralAccessAllowed ) state |= 2;
        if( state != m_state )
        {
            m_state = state;
            updateBlock();
        }
    }

    @Override
    protected void updateTick()
    {
        if( !getWorld().isRemote )
        {
            updateDirection();

            if( m_modem.getModemState().pollChanged() ) updateState();

            if( !m_connectionsFormed )
            {
                m_connectionsFormed = true;

                connectionsChanged();
                if( m_peripheralAccessAllowed )
                {
                    m_peripheral.attach( world, pos, modemDirection );
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

            IWiredElement element = ComputerCraftAPI.getWiredElementAt( world, offset, facing.getOpposite() );
            if( element == null ) continue;

            if( BlockCable.canConnectIn( state, facing ) )
            {
                // If we can connect to it then do so
                m_node.connectTo( element.getNode() );
            }
            else if( m_node.getNetwork() == element.getNode().getNetwork() )
            {
                // Otherwise if we're on the same network then attempt to void it.
                m_node.disconnectFrom( element.getNode() );
            }
        }
    }

    public void modemChanged()
    {
        if( getWorld().isRemote ) return;

        PeripheralType type = getPeripheralType();
        if( type != PeripheralType.WiredModemWithCable && m_peripheralAccessAllowed )
        {
            m_peripheralAccessAllowed = false;
            m_peripheral.detach();
            m_node.updatePeripherals( Collections.emptyMap() );
            markDirty();
            updateState();
        }
    }

    // private stuff
    private void togglePeripheralAccess()
    {
        if( !m_peripheralAccessAllowed )
        {
            m_peripheral.attach( world, getPos(), getDirection() );
            if( !m_peripheral.hasPeripheral() ) return;

            m_peripheralAccessAllowed = true;
            m_node.updatePeripherals( m_peripheral.toMap() );
        }
        else
        {
            m_peripheral.detach();

            m_peripheralAccessAllowed = false;
            m_node.updatePeripherals( Collections.emptyMap() );
        }

        updateState();
    }

    private void updateConnectedPeripherals()
    {
        Map<String, IPeripheral> peripherals = m_peripheral.toMap();
        if( peripherals.isEmpty() )
        {
            // If there are no peripherals then disable access and update the display state.
            m_peripheralAccessAllowed = false;
            updateState();
        }

        m_node.updatePeripherals( peripherals );
    }

    @Override
    public boolean canRenderBreaking()
    {
        return true;
    }

    @Override
    public boolean hasCapability( @Nonnull Capability<?> capability, @Nullable EnumFacing facing )
    {
        if( capability == CapabilityWiredElement.CAPABILITY )
        {
            return !m_destroyed && BlockCable.canConnectIn( getBlockState(), facing );
        }

        return super.hasCapability( capability, facing );
    }

    @Nullable
    @Override
    public <T> T getCapability( @Nonnull Capability<T> capability, @Nullable EnumFacing facing )
    {
        if( capability == CapabilityWiredElement.CAPABILITY )
        {
            return !m_destroyed && BlockCable.canConnectIn( getBlockState(), facing )
                ? CapabilityWiredElement.CAPABILITY.cast( m_cable )
                : null;
        }

        return super.getCapability( capability, facing );
    }

    @Override
    public IPeripheral getPeripheral( EnumFacing side )
    {
        return !m_destroyed && getPeripheralType() != PeripheralType.Cable && side == getDirection() ? m_modem : null;
    }

    public PeripheralType getPeripheralType()
    {
        IBlockState state = getBlockState();
        return ComputerCraft.Blocks.cable.getPeripheralType( state );
    }
}
