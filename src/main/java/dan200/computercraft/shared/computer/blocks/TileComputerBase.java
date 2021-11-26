/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.blocks;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralTile;
import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.shared.BundledRedstone;
import dan200.computercraft.shared.Peripherals;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ComputerState;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.network.container.ComputerContainerData;
import dan200.computercraft.shared.util.DirectionUtil;
import dan200.computercraft.shared.util.RedstoneUtil;
import joptsimple.internal.Strings;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public abstract class TileComputerBase extends TileGeneric implements IComputerTile, IPeripheralTile, Nameable,
    ExtendedScreenHandlerFactory
{
    private static final String NBT_ID = "ComputerId";
    private static final String NBT_LABEL = "Label";
    private static final String NBT_ON = "On";
    private final ComputerFamily family;
    protected String label = null;
    boolean startOn = false;
    private int instanceID = -1;
    private int computerID = -1;
    private boolean on = false;
    private boolean fresh = false;

    public TileComputerBase( BlockEntityType<? extends TileGeneric> type, ComputerFamily family, BlockPos pos, BlockState state )
    {
        super( type, pos, state );
        this.family = family;
    }

    @Override
    public void destroy()
    {
        unload();
        for( Direction dir : DirectionUtil.FACINGS )
        {
            RedstoneUtil.propagateRedstoneOutput( getLevel(), getBlockPos(), dir );
        }
    }

    @Override
    public void onChunkUnloaded()
    {
        unload();
    }

    protected void unload()
    {
        if( instanceID >= 0 )
        {
            if( !getLevel().isClientSide )
            {
                ComputerCraft.serverComputerRegistry.remove( instanceID );
            }
            instanceID = -1;
        }
    }

    @Nonnull
    @Override
    public InteractionResult onActivate( Player player, InteractionHand hand, BlockHitResult hit )
    {
        ItemStack currentItem = player.getItemInHand( hand );
        if( !currentItem.isEmpty() && currentItem.getItem() == Items.NAME_TAG && canNameWithTag( player ) && currentItem.hasCustomHoverName() )
        {
            // Label to rename computer
            if( !getLevel().isClientSide )
            {
                setLabel( currentItem.getHoverName()
                    .getString() );
                currentItem.shrink( 1 );
            }
            return InteractionResult.SUCCESS;
        }
        else if( !player.isCrouching() )
        {
            // Regular right click to activate computer
            if( !getLevel().isClientSide && isUsable( player, false ) )
            {
                createServerComputer().turnOn();
                createServerComputer().sendTerminalState( player );
                new ComputerContainerData( createServerComputer() ).open( player, this );
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    protected boolean canNameWithTag( Player player )
    {
        return false;
    }

    public ServerComputer createServerComputer()
    {
        if( getLevel().isClientSide )
        {
            return null;
        }

        boolean changed = false;
        if( instanceID < 0 )
        {
            instanceID = ComputerCraft.serverComputerRegistry.getUnusedInstanceID();
            changed = true;
        }
        if( !ComputerCraft.serverComputerRegistry.contains( instanceID ) )
        {
            ServerComputer computer = createComputer( instanceID, computerID );
            ComputerCraft.serverComputerRegistry.add( instanceID, computer );
            fresh = true;
            changed = true;
        }
        if( changed )
        {
            updateBlock();
            updateInput();
        }
        return ComputerCraft.serverComputerRegistry.get( instanceID );
    }

    public ServerComputer getServerComputer()
    {
        return getLevel().isClientSide ? null : ComputerCraft.serverComputerRegistry.get( instanceID );
    }

    protected abstract ServerComputer createComputer( int instanceID, int id );

    public void updateInput()
    {
        if( getLevel() == null || getLevel().isClientSide )
        {
            return;
        }

        // Update all sides
        ServerComputer computer = getServerComputer();
        if( computer == null )
        {
            return;
        }

        BlockPos pos = computer.getPosition();
        for( Direction dir : DirectionUtil.FACINGS )
        {
            updateSideInput( computer, dir, pos.relative( dir ) );
        }
    }

    private void updateSideInput( ServerComputer computer, Direction dir, BlockPos offset )
    {
        Direction offsetSide = dir.getOpposite();
        ComputerSide localDir = remapToLocalSide( dir );

        computer.setRedstoneInput( localDir, getRedstoneInput( level, offset, dir ) );
        computer.setBundledRedstoneInput( localDir, BundledRedstone.getOutput( getLevel(), offset, offsetSide ) );
        if( !isPeripheralBlockedOnSide( localDir ) )
        {
            IPeripheral peripheral = Peripherals.getPeripheral( getLevel(), offset, offsetSide );
            computer.setPeripheral( localDir, peripheral );
        }
    }

    protected ComputerSide remapToLocalSide( Direction globalSide )
    {
        return remapLocalSide( DirectionUtil.toLocal( getDirection(), globalSide ) );
    }

    /**
     * Gets the redstone input for an adjacent block.
     *
     * @param world The world we exist in
     * @param pos   The position of the neighbour
     * @param side  The side we are reading from
     * @return The effective redstone power
     */
    protected static int getRedstoneInput( Level world, BlockPos pos, Direction side )
    {
        int power = world.getSignal( pos, side );
        if( power >= 15 )
        {
            return power;
        }

        BlockState neighbour = world.getBlockState( pos );
        return neighbour.getBlock() == Blocks.REDSTONE_WIRE ? Math.max( power, neighbour.getValue( RedStoneWireBlock.POWER ) ) : power;
    }

    protected boolean isPeripheralBlockedOnSide( ComputerSide localSide )
    {
        return false;
    }

    protected ComputerSide remapLocalSide( ComputerSide localSide )
    {
        return localSide;
    }

    protected abstract Direction getDirection();

    @Override
    public void onNeighbourChange( @Nonnull BlockPos neighbour )
    {
        updateInput( neighbour );
    }

    @Override
    public void onNeighbourTileEntityChange( @Nonnull BlockPos neighbour )
    {
        updateInput( neighbour );
    }

    @Override
    protected void readDescription( @Nonnull CompoundTag nbt )
    {
        super.readDescription( nbt );
        label = nbt.contains( NBT_LABEL ) ? nbt.getString( NBT_LABEL ) : null;
        computerID = nbt.contains( NBT_ID ) ? nbt.getInt( NBT_ID ) : -1;
    }

    @Override
    protected void writeDescription( @Nonnull CompoundTag nbt )
    {
        super.writeDescription( nbt );
        if( label != null )
        {
            nbt.putString( NBT_LABEL, label );
        }
        if( computerID >= 0 )
        {
            nbt.putInt( NBT_ID, computerID );
        }
    }

    public void serverTick()
    {
        ServerComputer computer = createServerComputer();
        if( computer == null )
        {
            return;
        }

        // If the computer isn't on and should be, then turn it on
        if( startOn || fresh && on )
        {
            computer.turnOn();
            startOn = false;
        }

        computer.keepAlive();

        fresh = false;
        computerID = computer.getID();
        label = computer.getLabel();
        on = computer.isOn();

        if( computer.hasOutputChanged() )
        {
            updateOutput();
        }

        // Update the block state if needed. We don't fire a block update intentionally,
        // as this only really is needed on the client side.
        updateBlockState( computer.getState() );

        if( computer.hasOutputChanged() )
        {
            updateOutput();
        }
    }

    public void updateOutput()
    {
        // Update redstone
        updateBlock();
        for( Direction dir : DirectionUtil.FACINGS )
        {
            RedstoneUtil.propagateRedstoneOutput( getLevel(), getBlockPos(), dir );
        }
    }

    protected abstract void updateBlockState( ComputerState newState );

    @Override
    public void load( @Nonnull CompoundTag nbt )
    {
        super.load( nbt );

        // Load ID, label and power state
        computerID = nbt.contains( NBT_ID ) ? nbt.getInt( NBT_ID ) : -1;
        label = nbt.contains( NBT_LABEL ) ? nbt.getString( NBT_LABEL ) : null;
        on = startOn = nbt.getBoolean( NBT_ON );
    }

    @Nonnull
    @Override
    public CompoundTag save( @Nonnull CompoundTag nbt )
    {
        // Save ID, label and power state
        if( computerID >= 0 )
        {
            nbt.putInt( NBT_ID, computerID );
        }
        if( label != null )
        {
            nbt.putString( NBT_LABEL, label );
        }
        nbt.putBoolean( NBT_ON, on );
        return super.save( nbt );
    }

    @Override
    public void setRemoved()
    {
        unload();
        super.setRemoved();
    }

    private void updateInput( BlockPos neighbour )
    {
        if( getLevel() == null || this.level.isClientSide )
        {
            return;
        }

        ServerComputer computer = getServerComputer();
        if( computer == null )
        {
            return;
        }

        for( Direction dir : DirectionUtil.FACINGS )
        {
            BlockPos offset = worldPosition.relative( dir );
            if( offset.equals( neighbour ) )
            {
                updateSideInput( computer, dir, offset );
                return;
            }
        }

        // If the position is not any adjacent one, update all inputs.
        this.updateInput();
    }

    private void updateInput( Direction dir )
    {
        if( getLevel() == null || this.level.isClientSide )
        {
            return;
        }

        ServerComputer computer = getServerComputer();
        if( computer == null )
        {
            return;
        }

        updateSideInput( computer, dir, worldPosition.relative( dir ) );
    }

    @Override
    public final int getComputerID()
    {
        return computerID;
    }

    @Override
    public final void setComputerID( int id )
    {
        if( this.level.isClientSide || computerID == id )
        {
            return;
        }

        computerID = id;
        ServerComputer computer = getServerComputer();
        if( computer != null )
        {
            computer.setID( computerID );
        }
        setChanged();
    }

    @Override
    public final String getLabel()
    {
        return label;
    }

    // Networking stuff

    @Override
    public final void setLabel( String label )
    {
        if( this.level.isClientSide || Objects.equals( this.label, label ) )
        {
            return;
        }

        this.label = label;
        ServerComputer computer = getServerComputer();
        if( computer != null )
        {
            computer.setLabel( label );
        }
        setChanged();
    }

    @Override
    public ComputerFamily getFamily()
    {
        return family;
    }

    protected void transferStateFrom( TileComputerBase copy )
    {
        if( copy.computerID != computerID || copy.instanceID != instanceID )
        {
            unload();
            instanceID = copy.instanceID;
            computerID = copy.computerID;
            label = copy.label;
            on = copy.on;
            startOn = copy.startOn;
            updateBlock();
        }
        copy.instanceID = -1;
    }

    @Nonnull
    @Override
    public IPeripheral getPeripheral( Direction side )
    {
        return new ComputerPeripheral( "computer", createProxy() );
    }

    public abstract ComputerProxy createProxy();

    @Nonnull
    @Override
    public Component getName()
    {
        return hasCustomName() ? new TextComponent( label ) : new TranslatableComponent( getBlockState().getBlock()
            .getDescriptionId() );
    }

    @Override
    public boolean hasCustomName()
    {
        return !Strings.isNullOrEmpty( label );
    }

    @Nonnull
    @Override
    public Component getDisplayName()
    {
        return Nameable.super.getDisplayName();
    }

    @Nullable
    @Override
    public Component getCustomName()
    {
        return hasCustomName() ? new TextComponent( label ) : null;
    }

    @Override
    public void writeScreenOpeningData( ServerPlayer serverPlayerEntity, FriendlyByteBuf packetByteBuf )
    {
        packetByteBuf.writeInt( getServerComputer().getInstanceID() );
        packetByteBuf.writeEnum( getServerComputer().getFamily() );
    }
}
