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
import dan200.computercraft.shared.command.text.ChatHelpers;
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
import net.minecraft.util.math.Vec3d;
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
        public Vec3d getPosition()
        {
            BlockPos pos = getBlockPos();
            return new Vec3d( pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5 );
        }

        @Override
        protected void attachPeripheral( String name, IPeripheral peripheral )
        {
            modem.attachPeripheral( name, peripheral );
        }

        @Override
        protected void detachPeripheral( String name )
        {
            modem.detachPeripheral( name );
        }
    }

    private boolean peripheralAccessAllowed;
    private final WiredModemLocalPeripheral peripheral = new WiredModemLocalPeripheral( this::refreshPeripheral );

    private boolean destroyed = false;

    private Direction modemDirection = Direction.NORTH;
    private boolean hasModemDirection = false;
    private boolean connectionsFormed = false;

    private final WiredModemElement cable = new CableElement();
    private LazyOptional<IWiredElement> elementCap;
    private final IWiredNode node = cable.getNode();
    private final WiredModemPeripheral modem = new WiredModemPeripheral(
        new ModemState( () -> TickScheduler.schedule( this ) ),
        cable
    )
    {
        @Nonnull
        @Override
        protected WiredModemLocalPeripheral getLocalPeripheral()
        {
            return peripheral;
        }

        @Nonnull
        @Override
        public Vec3d getPosition()
        {
            BlockPos pos = getBlockPos().relative( modemDirection );
            return new Vec3d( pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5 );
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
            node.remove();
            connectionsFormed = false;
        }
    }

    @Override
    public void destroy()
    {
        if( !destroyed )
        {
            destroyed = true;
            modem.destroy();
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
        if( !level.isClientSide && peripheralAccessAllowed )
        {
            Direction facing = getDirection();
            if( getBlockPos().relative( facing ).equals( neighbour ) ) refreshPeripheral();
        }
    }

    private void refreshPeripheral()
    {
        if( level != null && !isRemoved() && peripheral.attach( level, getBlockPos(), getDirection() ) )
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

        String oldName = peripheral.getConnectedName();
        togglePeripheralAccess();
        String newName = peripheral.getConnectedName();
        if( !Objects.equal( newName, oldName ) )
        {
            if( oldName != null )
            {
                player.displayClientMessage( new TranslationTextComponent( "chat.computercraft.wired_modem.peripheral_disconnected",
                    ChatHelpers.copy( oldName ) ), false );
            }
            if( newName != null )
            {
                player.displayClientMessage( new TranslationTextComponent( "chat.computercraft.wired_modem.peripheral_connected",
                    ChatHelpers.copy( newName ) ), false );
            }
        }

        return ActionResultType.SUCCESS;
    }

    @Override
    public void load( @Nonnull CompoundNBT nbt )
    {
        super.load( nbt );
        peripheralAccessAllowed = nbt.getBoolean( NBT_PERIPHERAL_ENABLED );
        peripheral.read( nbt, "" );
    }

    @Nonnull
    @Override
    public CompoundNBT save( CompoundNBT nbt )
    {
        nbt.putBoolean( NBT_PERIPHERAL_ENABLED, peripheralAccessAllowed );
        peripheral.write( nbt, "" );
        return super.save( nbt );
    }

    private void updateBlockState()
    {
        BlockState state = getBlockState();
        CableModemVariant oldVariant = state.getValue( BlockCable.MODEM );
        CableModemVariant newVariant = CableModemVariant
            .from( oldVariant.getFacing(), modem.getModemState().isOpen(), peripheralAccessAllowed );

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

        if( modem.getModemState().pollChanged() ) updateBlockState();

        if( !connectionsFormed )
        {
            connectionsFormed = true;

            connectionsChanged();
            if( peripheralAccessAllowed )
            {
                peripheral.attach( level, worldPosition, modemDirection );
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
                this.node.connectTo( node );
            }
            else if( this.node.getNetwork() == node.getNetwork() )
            {
                // Otherwise if we're on the same network then attempt to void it.
                this.node.disconnectFrom( node );
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
        if( !canAttachPeripheral() && peripheralAccessAllowed )
        {
            peripheralAccessAllowed = false;
            peripheral.detach();
            node.updatePeripherals( Collections.emptyMap() );
            setChanged();
            updateBlockState();
        }
    }

    private void togglePeripheralAccess()
    {
        if( !peripheralAccessAllowed )
        {
            peripheral.attach( level, getBlockPos(), getDirection() );
            if( !peripheral.hasPeripheral() ) return;

            peripheralAccessAllowed = true;
            node.updatePeripherals( peripheral.toMap() );
        }
        else
        {
            peripheral.detach();

            peripheralAccessAllowed = false;
            node.updatePeripherals( Collections.emptyMap() );
        }

        updateBlockState();
    }

    private void updateConnectedPeripherals()
    {
        Map<String, IPeripheral> peripherals = peripheral.toMap();
        if( peripherals.isEmpty() )
        {
            // If there are no peripherals then disable access and update the display state.
            peripheralAccessAllowed = false;
            updateBlockState();
        }

        node.updatePeripherals( peripherals );
    }

    @Override
    public boolean canRenderBreaking()
    {
        return true;
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability( @Nonnull Capability<T> capability, @Nullable Direction side )
    {
        if( capability == CAPABILITY_WIRED_ELEMENT )
        {
            if( destroyed || !BlockCable.canConnectIn( getBlockState(), side ) ) return LazyOptional.empty();
            if( elementCap == null ) elementCap = LazyOptional.of( () -> cable );
            return elementCap.cast();
        }

        if( capability == CAPABILITY_PERIPHERAL )
        {
            refreshDirection();
            if( side != null && getMaybeDirection() != side ) return LazyOptional.empty();
            if( modemCap == null ) modemCap = LazyOptional.of( () -> modem );
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
