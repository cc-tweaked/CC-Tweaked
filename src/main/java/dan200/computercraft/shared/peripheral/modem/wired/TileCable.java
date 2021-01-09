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
            return TileCable.this.getLevel();
        }

        @Nonnull
        @Override
        public Vector3d getPosition()
        {
            BlockPos pos = getBlockPos();
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
            BlockPos pos = getBlockPos().relative( modemDirection );
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
        if( level == null || !level.isClientSide )
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
    public void setRemoved()
    {
        super.setRemoved();
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
    public void clearCache()
    {
        super.clearCache();
        hasModemDirection = false;
        if( !level.isClientSide ) level.getBlockTicks().scheduleTick( worldPosition, getBlockState().getBlock(), 0 );
    }

    private void refreshDirection()
    {
        if( hasModemDirection ) return;

        hasModemDirection = true;
        modemDirection = getBlockState().getValue( BlockCable.MODEM ).getFacing();
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
        if( neighbour.equals( getBlockPos().relative( dir ) ) && hasModem() && !getBlockState().canSurvive( getLevel(), getBlockPos() ) )
        {
            if( hasCable() )
            {
                // Drop the modem and convert to cable
                Block.popResource( getLevel(), getBlockPos(), new ItemStack( Registry.ModItems.WIRED_MODEM.get() ) );
                getLevel().setBlockAndUpdate( getBlockPos(), getBlockState().setValue( BlockCable.MODEM, CableModemVariant.None ) );
                modemChanged();
                connectionsChanged();
            }
            else
            {
                // Drop everything and remove block
                Block.popResource( getLevel(), getBlockPos(), new ItemStack( Registry.ModItems.WIRED_MODEM.get() ) );
                getLevel().removeBlock( getBlockPos(), false );
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
        if( !level.isClientSide && m_peripheralAccessAllowed )
        {
            Direction facing = getDirection();
            if( getBlockPos().relative( facing ).equals( neighbour ) ) refreshPeripheral();
        }
    }

    private void refreshPeripheral()
    {
        if( level != null && !isRemoved() && m_peripheral.attach( level, getBlockPos(), getDirection() ) )
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

        if( getLevel().isClientSide ) return ActionResultType.SUCCESS;

        String oldName = m_peripheral.getConnectedName();
        togglePeripheralAccess();
        String newName = m_peripheral.getConnectedName();
        if( !Objects.equal( newName, oldName ) )
        {
            if( oldName != null )
            {
                player.displayClientMessage( new TranslationTextComponent( "chat.computercraft.wired_modem.peripheral_disconnected",
                    CommandCopy.createCopyText( oldName ) ), false );
            }
            if( newName != null )
            {
                player.displayClientMessage( new TranslationTextComponent( "chat.computercraft.wired_modem.peripheral_connected",
                    CommandCopy.createCopyText( newName ) ), false );
            }
        }

        return ActionResultType.SUCCESS;
    }

    @Override
    public void load( @Nonnull BlockState state, @Nonnull CompoundNBT nbt )
    {
        super.load( state, nbt );
        m_peripheralAccessAllowed = nbt.getBoolean( NBT_PERIPHERAL_ENABLED );
        m_peripheral.read( nbt, "" );
    }

    @Nonnull
    @Override
    public CompoundNBT save( CompoundNBT nbt )
    {
        nbt.putBoolean( NBT_PERIPHERAL_ENABLED, m_peripheralAccessAllowed );
        m_peripheral.write( nbt, "" );
        return super.save( nbt );
    }

    private void updateBlockState()
    {
        BlockState state = getBlockState();
        CableModemVariant oldVariant = state.getValue( BlockCable.MODEM );
        CableModemVariant newVariant = CableModemVariant
            .from( oldVariant.getFacing(), m_modem.getModemState().isOpen(), m_peripheralAccessAllowed );

        if( oldVariant != newVariant )
        {
            level.setBlockAndUpdate( getBlockPos(), state.setValue( BlockCable.MODEM, newVariant ) );
        }
    }

    @Override
    public void blockTick()
    {
        if( getLevel().isClientSide ) return;

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
                m_peripheral.attach( level, worldPosition, modemDirection );
                updateConnectedPeripherals();
            }
        }
    }

    void connectionsChanged()
    {
        if( getLevel().isClientSide ) return;

        BlockState state = getBlockState();
        World world = getLevel();
        BlockPos current = getBlockPos();
        for( Direction facing : DirectionUtil.FACINGS )
        {
            BlockPos offset = current.relative( facing );
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

        if( getLevel().isClientSide ) return;

        // If we can no longer attach peripherals, then detach any
        // which may have existed
        if( !canAttachPeripheral() && m_peripheralAccessAllowed )
        {
            m_peripheralAccessAllowed = false;
            m_peripheral.detach();
            m_node.updatePeripherals( Collections.emptyMap() );
            setChanged();
            updateBlockState();
        }
    }

    private void togglePeripheralAccess()
    {
        if( !m_peripheralAccessAllowed )
        {
            m_peripheral.attach( level, getBlockPos(), getDirection() );
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
        return getBlockState().getValue( BlockCable.CABLE );
    }

    public boolean hasModem()
    {
        return getBlockState().getValue( BlockCable.MODEM ) != CableModemVariant.None;
    }

    private boolean canAttachPeripheral()
    {
        return hasCable() && hasModem();
    }
}
