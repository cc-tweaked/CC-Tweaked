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
import dan200.computercraft.shared.Registry;
import dan200.computercraft.shared.command.CommandCopy;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.peripheral.modem.ModemState;
import dan200.computercraft.shared.util.CapabilityUtil;
import dan200.computercraft.shared.util.DirectionUtil;
import dan200.computercraft.shared.util.TickScheduler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullConsumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;

import static dan200.computercraft.shared.Capabilities.CAPABILITY_PERIPHERAL;
import static dan200.computercraft.shared.Capabilities.CAPABILITY_WIRED_ELEMENT;

public class TileCable extends TileGeneric
{
    private static final String NBT_PERIPHERAL_ENABLED = "PeirpheralAccess";

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
        public Vector3d getPosition()
        {
            BlockPos pos = getPos();
            return new Vector3d( pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5 );
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

    private boolean m_peripheralAccessAllowed;
    private final WiredModemLocalPeripheral m_peripheral = new WiredModemLocalPeripheral( this::refreshPeripheral );

    private boolean m_destroyed = false;

    private Direction modemDirection = Direction.NORTH;
    private boolean hasModemDirection = false;
    private boolean m_connectionsFormed = false;

    private final WiredModemElement m_cable = new CableElement();
    private LazyOptional<IWiredElement> elementCap;
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
        public Vector3d getPosition()
        {
            BlockPos pos = getPos().offset( modemDirection );
            return new Vector3d( pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5 );
        }

        @Nonnull
        @Override
        public Object getTarget()
        {
            return TileCable.this;
        }
    };
    private LazyOptional<IPeripheral> modemCap;

    private final NonNullConsumer<LazyOptional<IWiredElement>> connectedNodeChanged = x -> connectionsChanged();

    public TileCable( TileEntityType<? extends TileCable> type )
    {
        super( type );
    }

    private void onRemove()
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
            onRemove();
        }
    }

    @Override
    public void onChunkUnloaded()
    {
        super.onChunkUnloaded();
        onRemove();
    }

    @Override
    public void remove()
    {
        super.remove();
        onRemove();
    }

    @Override
    protected void invalidateCaps()
    {
        super.invalidateCaps();
        elementCap = CapabilityUtil.invalidate( elementCap );
        modemCap = CapabilityUtil.invalidate( modemCap );
    }

    @Override
    public void onLoad()
    {
        super.onLoad();
        TickScheduler.schedule( this );
    }

    @Override
    public void updateContainingBlockInfo()
    {
        super.updateContainingBlockInfo();
        hasModemDirection = false;
        if( !world.isRemote ) world.getPendingBlockTicks().scheduleTick( pos, getBlockState().getBlock(), 0 );
    }

    private void refreshDirection()
    {
        if( hasModemDirection ) return;

        hasModemDirection = true;
        modemDirection = getBlockState().get( BlockCable.MODEM ).getFacing();
    }

    @Nullable
    private Direction getMaybeDirection()
    {
        refreshDirection();
        return modemDirection;
    }

    @Nonnull
    private Direction getDirection()
    {
        refreshDirection();
        return modemDirection == null ? Direction.NORTH : modemDirection;
    }

    @Override
    public void onNeighbourChange( @Nonnull BlockPos neighbour )
    {
        Direction dir = getDirection();
        if( neighbour.equals( getPos().offset( dir ) ) && hasModem() && !getBlockState().isValidPosition( getWorld(), getPos() ) )
        {
            if( hasCable() )
            {
                // Drop the modem and convert to cable
                Block.spawnAsEntity( getWorld(), getPos(), new ItemStack( Registry.ModItems.WIRED_MODEM.get() ) );
                getWorld().setBlockState( getPos(), getBlockState().with( BlockCable.MODEM, CableModemVariant.None ) );
                modemChanged();
                connectionsChanged();
            }
            else
            {
                // Drop everything and remove block
                Block.spawnAsEntity( getWorld(), getPos(), new ItemStack( Registry.ModItems.WIRED_MODEM.get() ) );
                getWorld().removeBlock( getPos(), false );
                // This'll call #destroy(), so we don't need to reset the network here.
            }

            return;
        }

        onNeighbourTileEntityChange( neighbour );
    }

    @Override
    public void onNeighbourTileEntityChange( @Nonnull BlockPos neighbour )
    {
        super.onNeighbourTileEntityChange( neighbour );
        if( !world.isRemote && m_peripheralAccessAllowed )
        {
            Direction facing = getDirection();
            if( getPos().offset( facing ).equals( neighbour ) ) refreshPeripheral();
        }
    }

    private void refreshPeripheral()
    {
        if( world != null && !isRemoved() && m_peripheral.attach( world, getPos(), getDirection() ) )
        {
            updateConnectedPeripherals();
        }
    }

    @Nonnull
    @Override
    public ActionResultType onActivate( PlayerEntity player, Hand hand, BlockRayTraceResult hit )
    {
        if( player.isCrouching() ) return ActionResultType.PASS;
        if( !canAttachPeripheral() ) return ActionResultType.FAIL;

        if( getWorld().isRemote ) return ActionResultType.SUCCESS;

        String oldName = m_peripheral.getConnectedName();
        togglePeripheralAccess();
        String newName = m_peripheral.getConnectedName();
        if( !Objects.equal( newName, oldName ) )
        {
            if( oldName != null )
            {
                player.sendStatusMessage( new TranslationTextComponent( "chat.computercraft.wired_modem.peripheral_disconnected",
                    CommandCopy.createCopyText( oldName ) ), false );
            }
            if( newName != null )
            {
                player.sendStatusMessage( new TranslationTextComponent( "chat.computercraft.wired_modem.peripheral_connected",
                    CommandCopy.createCopyText( newName ) ), false );
            }
        }

        return ActionResultType.SUCCESS;
    }

    @Override
    public void read( @Nonnull BlockState state, @Nonnull CompoundNBT nbt )
    {
        super.read( state, nbt );
        m_peripheralAccessAllowed = nbt.getBoolean( NBT_PERIPHERAL_ENABLED );
        m_peripheral.read( nbt, "" );
    }

    @Nonnull
    @Override
    public CompoundNBT write( CompoundNBT nbt )
    {
        nbt.putBoolean( NBT_PERIPHERAL_ENABLED, m_peripheralAccessAllowed );
        m_peripheral.write( nbt, "" );
        return super.write( nbt );
    }

    private void updateBlockState()
    {
        BlockState state = getBlockState();
        CableModemVariant oldVariant = state.get( BlockCable.MODEM );
        CableModemVariant newVariant = CableModemVariant
            .from( oldVariant.getFacing(), m_modem.getModemState().isOpen(), m_peripheralAccessAllowed );

        if( oldVariant != newVariant )
        {
            world.setBlockState( getPos(), state.with( BlockCable.MODEM, newVariant ) );
        }
    }

    @Override
    public void blockTick()
    {
        if( getWorld().isRemote ) return;

        Direction oldDirection = modemDirection;
        refreshDirection();
        if( modemDirection != oldDirection )
        {
            // We invalidate both the modem and element if the modem's direction is different.
            modemCap = CapabilityUtil.invalidate( modemCap );
            elementCap = CapabilityUtil.invalidate( elementCap );
        }

        if( m_modem.getModemState().pollChanged() ) updateBlockState();

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

    void connectionsChanged()
    {
        if( getWorld().isRemote ) return;

        BlockState state = getBlockState();
        World world = getWorld();
        BlockPos current = getPos();
        for( Direction facing : DirectionUtil.FACINGS )
        {
            BlockPos offset = current.offset( facing );
            if( !world.isAreaLoaded( offset, 0 ) ) continue;

            LazyOptional<IWiredElement> element = ComputerCraftAPI.getWiredElementAt( world, offset, facing.getOpposite() );
            if( !element.isPresent() ) continue;

            element.addListener( connectedNodeChanged );
            IWiredNode node = element.orElseThrow( NullPointerException::new ).getNode();
            if( BlockCable.canConnectIn( state, facing ) )
            {
                // If we can connect to it then do so
                m_node.connectTo( node );
            }
            else if( m_node.getNetwork() == node.getNetwork() )
            {
                // Otherwise if we're on the same network then attempt to void it.
                m_node.disconnectFrom( node );
            }
        }
    }

    void modemChanged()
    {
        // Tell anyone who cares that the connection state has changed
        elementCap = CapabilityUtil.invalidate( elementCap );

        if( getWorld().isRemote ) return;

        // If we can no longer attach peripherals, then detach any
        // which may have existed
        if( !canAttachPeripheral() && m_peripheralAccessAllowed )
        {
            m_peripheralAccessAllowed = false;
            m_peripheral.detach();
            m_node.updatePeripherals( Collections.emptyMap() );
            markDirty();
            updateBlockState();
        }
    }

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

        updateBlockState();
    }

    private void updateConnectedPeripherals()
    {
        Map<String, IPeripheral> peripherals = m_peripheral.toMap();
        if( peripherals.isEmpty() )
        {
            // If there are no peripherals then disable access and update the display state.
            m_peripheralAccessAllowed = false;
            updateBlockState();
        }

        m_node.updatePeripherals( peripherals );
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability( @Nonnull Capability<T> capability, @Nullable Direction side )
    {
        if( capability == CAPABILITY_WIRED_ELEMENT )
        {
            if( m_destroyed || !BlockCable.canConnectIn( getBlockState(), side ) ) return LazyOptional.empty();
            if( elementCap == null ) elementCap = LazyOptional.of( () -> m_cable );
            return elementCap.cast();
        }

        if( capability == CAPABILITY_PERIPHERAL )
        {
            refreshDirection();
            if( side != null && getMaybeDirection() != side ) return LazyOptional.empty();
            if( modemCap == null ) modemCap = LazyOptional.of( () -> m_modem );
            return modemCap.cast();
        }

        return super.getCapability( capability, side );
    }

    boolean hasCable()
    {
        return getBlockState().get( BlockCable.CABLE );
    }

    public boolean hasModem()
    {
        return getBlockState().get( BlockCable.MODEM ) != CableModemVariant.None;
    }

    private boolean canAttachPeripheral()
    {
        return hasCable() && hasModem();
    }
}
