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
import dan200.computercraft.shared.ComputerCraftRegistry;
import dan200.computercraft.shared.command.text.ChatHelpers;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.peripheral.modem.ModemState;
import dan200.computercraft.shared.util.DirectionUtil;
import dan200.computercraft.shared.util.TickScheduler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;

public class TileCable extends TileGeneric implements IPeripheralTile
{
    private static final String NBT_PERIPHERAL_ENABLED = "PeirpheralAccess";
    private final WiredModemLocalPeripheral peripheral = new WiredModemLocalPeripheral();
    private final WiredModemElement cable = new CableElement();
    private final IWiredNode node = cable.getNode();
    private boolean peripheralAccessAllowed;
    private boolean destroyed = false;
    private Direction modemDirection = Direction.NORTH;
    private final WiredModemPeripheral modem = new WiredModemPeripheral( new ModemState( () -> TickScheduler.schedule( this ) ), cable )
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
            BlockPos pos = getPos().offset( modemDirection );
            return new Vec3d( pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5 );
        }

        @Nonnull
        @Override
        public Object getTarget()
        {
            return TileCable.this;
        }
    };
    private boolean hasModemDirection = false;
    private boolean connectionsFormed = false;

    public TileCable( BlockEntityType<? extends TileCable> type )
    {
        super( type );
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

    private void onRemove()
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
        if( player.isInSneakingPose() )
        {
            return ActionResult.PASS;
        }
        if( !canAttachPeripheral() )
        {
            return ActionResult.FAIL;
        }

        if( getWorld().isClient )
        {
            return ActionResult.SUCCESS;
        }

        String oldName = peripheral.getConnectedName();
        togglePeripheralAccess();
        String newName = peripheral.getConnectedName();
        if( !Objects.equal( newName, oldName ) )
        {
            if( oldName != null )
            {
                player.sendMessage( new TranslatableText( "chat.computercraft.wired_modem.peripheral_disconnected",
                    ChatHelpers.copy( oldName ) ), false );
            }
            if( newName != null )
            {
                player.sendMessage( new TranslatableText( "chat.computercraft.wired_modem.peripheral_connected",
                    ChatHelpers.copy( newName ) ), false );
            }
        }

        return ActionResult.SUCCESS;
    }

    @Override
    public void onNeighbourChange( @Nonnull BlockPos neighbour )
    {
        Direction dir = getDirection();
        if( neighbour.equals( getPos().offset( dir ) ) && hasModem() && !getCachedState().canPlaceAt( getWorld(), getPos() ) )
        {
            if( hasCable() )
            {
                // Drop the modem and convert to cable
                Block.dropStack( getWorld(), getPos(), new ItemStack( ComputerCraftRegistry.ModItems.WIRED_MODEM ) );
                getWorld().setBlockState( getPos(),
                    getCachedState().with( BlockCable.MODEM, CableModemVariant.None ) );
                modemChanged();
                connectionsChanged();
            }
            else
            {
                // Drop everything and remove block
                Block.dropStack( getWorld(), getPos(), new ItemStack( ComputerCraftRegistry.ModItems.WIRED_MODEM ) );
                getWorld().removeBlock( getPos(), false );
                // This'll call #destroy(), so we don't need to reset the network here.
            }

            return;
        }

        onNeighbourTileEntityChange( neighbour );
    }

    @Nonnull
    private Direction getDirection()
    {
        refreshDirection();
        return modemDirection == null ? Direction.NORTH : modemDirection;
    }

    public boolean hasModem()
    {
        return getCachedState().get( BlockCable.MODEM ) != CableModemVariant.None;
    }

    boolean hasCable()
    {
        return getCachedState().get( BlockCable.CABLE );
    }

    void modemChanged()
    {
        // Tell anyone who cares that the connection state has changed
        if( getWorld().isClient )
        {
            return;
        }

        // If we can no longer attach peripherals, then detach any
        // which may have existed
        if( !canAttachPeripheral() && peripheralAccessAllowed )
        {
            peripheralAccessAllowed = false;
            peripheral.detach();
            node.updatePeripherals( Collections.emptyMap() );
            markDirty();
            updateBlockState();
        }
    }

    void connectionsChanged()
    {
        if( getWorld().isClient )
        {
            return;
        }

        BlockState state = getCachedState();
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
            if( element != null )
            {
                // TODO Figure out why this crashes.
                IWiredNode node = element.getNode();
                if( node != null && this.node != null )
                {
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
        }
    }

    private boolean canAttachPeripheral()
    {
        return hasCable() && hasModem();
    }

    private void updateBlockState()
    {
        BlockState state = getCachedState();
        CableModemVariant oldVariant = state.get( BlockCable.MODEM );
        CableModemVariant newVariant = CableModemVariant.from( oldVariant.getFacing(), modem.getModemState()
            .isOpen(), peripheralAccessAllowed );

        if( oldVariant != newVariant )
        {
            world.setBlockState( getPos(), state.with( BlockCable.MODEM, newVariant ) );
        }
    }

    private void refreshPeripheral()
    {
        if( world != null && !isRemoved() && peripheral.attach( world, getPos(), getDirection() ) )
        {
            updateConnectedPeripherals();
        }
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
    public void onNeighbourTileEntityChange( @Nonnull BlockPos neighbour )
    {
        super.onNeighbourTileEntityChange( neighbour );
        if( !world.isClient && peripheralAccessAllowed )
        {
            Direction facing = getDirection();
            if( getPos().offset( facing )
                .equals( neighbour ) )
            {
                refreshPeripheral();
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

        refreshDirection();

        if( modem.getModemState()
            .pollChanged() )
        {
            updateBlockState();
        }

        if( !connectionsFormed )
        {
            connectionsFormed = true;

            connectionsChanged();
            if( peripheralAccessAllowed )
            {
                peripheral.attach( world, pos, modemDirection );
                updateConnectedPeripherals();
            }
        }
    }

    private void togglePeripheralAccess()
    {
        if( !peripheralAccessAllowed )
        {
            peripheral.attach( world, getPos(), getDirection() );
            if( !peripheral.hasPeripheral() )
            {
                return;
            }

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

    @Nullable
    private Direction getMaybeDirection()
    {
        refreshDirection();
        return modemDirection;
    }

    private void refreshDirection()
    {
        if( hasModemDirection )
        {
            return;
        }

        hasModemDirection = true;
        modemDirection = getCachedState().get( BlockCable.MODEM )
            .getFacing();
    }

    @Override
    public void readNbt( @Nonnull BlockState state, @Nonnull NbtCompound nbt )
    {
        super.readNbt( state, nbt );
        peripheralAccessAllowed = nbt.getBoolean( NBT_PERIPHERAL_ENABLED );
        peripheral.read( nbt, "" );
    }

    @Nonnull
    @Override
    public NbtCompound writeNbt( NbtCompound nbt )
    {
        nbt.putBoolean( NBT_PERIPHERAL_ENABLED, peripheralAccessAllowed );
        peripheral.write( nbt, "" );
        return super.writeNbt( nbt );
    }

    @Override
    public void markRemoved()
    {
        super.markRemoved();
        onRemove();
    }

    @Override
    public void cancelRemoval()
    {
        super.cancelRemoval();
        TickScheduler.schedule( this );
    }

    @Override
    public void resetBlock()
    {
        super.resetBlock();
        hasModemDirection = false;
        if( !world.isClient )
        {
            world.getBlockTickScheduler()
                .schedule( pos,
                    getCachedState().getBlock(), 0 );
        }
    }

    public IWiredElement getElement( Direction facing )
    {
        return BlockCable.canConnectIn( getCachedState(), facing ) ? cable : null;
    }

    @Nonnull
    @Override
    public IPeripheral getPeripheral( Direction side )
    {
        return !destroyed && hasModem() && side == getDirection() ? modem : null;
    }

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
            BlockPos pos = getPos();
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
}
