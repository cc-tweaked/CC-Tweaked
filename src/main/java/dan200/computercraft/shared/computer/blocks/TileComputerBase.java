/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.blocks;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.peripheral.IPeripheral;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullConsumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public abstract class TileComputerBase extends TileGeneric implements IComputerTile, Nameable, MenuProvider
{
    private static final String NBT_ID = "ComputerId";
    private static final String NBT_LABEL = "Label";
    private static final String NBT_ON = "On";

    private int instanceID = -1;
    private int computerID = -1;
    protected String label = null;
    private boolean on = false;
    boolean startOn = false;
    private boolean fresh = false;
    private final NonNullConsumer<LazyOptional<IPeripheral>>[] invalidate;

    private final ComputerFamily family;

    public TileComputerBase( BlockEntityType<? extends TileGeneric> type, BlockPos pos, BlockState state, ComputerFamily family )
    {
        super( type, pos, state );
        this.family = family;

        // We cache these so we can guarantee we only ever register one listener for adjacent capabilities.
        @SuppressWarnings( { "unchecked", "rawtypes" } )
        NonNullConsumer<LazyOptional<IPeripheral>>[] invalidate = this.invalidate = new NonNullConsumer[6];
        for( Direction direction : Direction.values() )
        {
            invalidate[direction.ordinal()] = o -> updateInput( direction );
        }
    }

    protected void unload()
    {
        if( instanceID >= 0 )
        {
            if( !getLevel().isClientSide ) ComputerCraft.serverComputerRegistry.remove( instanceID );
            instanceID = -1;
        }
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

    @Override
    public void setRemoved()
    {
        unload();
        super.setRemoved();
    }

    protected boolean canNameWithTag( Player player )
    {
        return false;
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
                setLabel( currentItem.getHoverName().getString() );
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
                new ComputerContainerData( createServerComputer() ).open( player, this );
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

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

    protected void serverTick()
    {
        ServerComputer computer = createServerComputer();
        if( computer == null ) return;

        // If the computer isn't on and should be, then turn it on
        if( startOn || (fresh && on) )
        {
            computer.turnOn();
            startOn = false;
        }

        computer.keepAlive();

        fresh = false;
        computerID = computer.getID();
        label = computer.getLabel();
        on = computer.isOn();

        // Update the block state if needed. We don't fire a block update intentionally,
        // as this only really is needed on the client side.
        updateBlockState( computer.getState() );

        // TODO: This should ideally be split up into label/id/on (which should save NBT and sync to client) and
        //  redstone (which should update outputs)
        if( computer.hasOutputChanged() ) updateOutput();
    }

    protected abstract void updateBlockState( ComputerState newState );

    @Nonnull
    @Override
    public CompoundTag save( @Nonnull CompoundTag nbt )
    {
        // Save ID, label and power state
        if( computerID >= 0 ) nbt.putInt( NBT_ID, computerID );
        if( label != null ) nbt.putString( NBT_LABEL, label );
        nbt.putBoolean( NBT_ON, on );

        return super.save( nbt );
    }

    @Override
    public void load( @Nonnull CompoundTag nbt )
    {
        super.load( nbt );

        // Load ID, label and power state
        computerID = nbt.contains( NBT_ID ) ? nbt.getInt( NBT_ID ) : -1;
        label = nbt.contains( NBT_LABEL ) ? nbt.getString( NBT_LABEL ) : null;
        on = startOn = nbt.getBoolean( NBT_ON );
    }

    protected boolean isPeripheralBlockedOnSide( ComputerSide localSide )
    {
        return false;
    }

    protected abstract Direction getDirection();

    protected ComputerSide remapToLocalSide( Direction globalSide )
    {
        return remapLocalSide( DirectionUtil.toLocal( getDirection(), globalSide ) );
    }

    protected ComputerSide remapLocalSide( ComputerSide localSide )
    {
        return localSide;
    }

    private void updateSideInput( ServerComputer computer, Direction dir, BlockPos offset )
    {
        Direction offsetSide = dir.getOpposite();
        ComputerSide localDir = remapToLocalSide( dir );

        computer.setRedstoneInput( localDir, getRedstoneInput( level, offset, dir ) );
        computer.setBundledRedstoneInput( localDir, BundledRedstone.getOutput( getLevel(), offset, offsetSide ) );
        if( !isPeripheralBlockedOnSide( localDir ) )
        {
            IPeripheral peripheral = Peripherals.getPeripheral( getLevel(), offset, offsetSide, invalidate[dir.ordinal()] );
            computer.setPeripheral( localDir, peripheral );
        }
    }

    /**
     * Gets the redstone input for an adjacent block.
     *
     * @param world The world we exist in
     * @param pos   The position of the neighbour
     * @param side  The side we are reading from
     * @return The effective redstone power
     * @see DiodeBlock#getInputSignal(Level, BlockPos, BlockState)
     */
    protected static int getRedstoneInput( Level world, BlockPos pos, Direction side )
    {
        int power = world.getSignal( pos, side );
        if( power >= 15 ) return power;

        BlockState neighbour = world.getBlockState( pos );
        return neighbour.getBlock() == Blocks.REDSTONE_WIRE
            ? Math.max( power, neighbour.getValue( RedStoneWireBlock.POWER ) )
            : power;
    }

    public void updateInput()
    {
        if( getLevel() == null || getLevel().isClientSide ) return;

        // Update all sides
        ServerComputer computer = getServerComputer();
        if( computer == null ) return;

        BlockPos pos = computer.getPosition();
        for( Direction dir : DirectionUtil.FACINGS )
        {
            updateSideInput( computer, dir, pos.relative( dir ) );
        }
    }

    private void updateInput( BlockPos neighbour )
    {
        if( getLevel() == null || getLevel().isClientSide ) return;

        ServerComputer computer = getServerComputer();
        if( computer == null ) return;

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
        updateInput();
    }

    private void updateInput( Direction dir )
    {
        if( getLevel() == null || getLevel().isClientSide ) return;

        ServerComputer computer = getServerComputer();
        if( computer == null ) return;

        updateSideInput( computer, dir, worldPosition.relative( dir ) );
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

    protected abstract ServerComputer createComputer( int instanceID, int id );

    @Override
    public final int getComputerID()
    {
        return computerID;
    }

    @Override
    public final String getLabel()
    {
        return label;
    }

    @Override
    public final void setComputerID( int id )
    {
        if( getLevel().isClientSide || computerID == id ) return;

        computerID = id;
        ServerComputer computer = getServerComputer();
        if( computer != null ) computer.setID( computerID );
        setChanged();
    }

    @Override
    public final void setLabel( String label )
    {
        if( getLevel().isClientSide || Objects.equals( this.label, label ) ) return;

        this.label = label;
        ServerComputer computer = getServerComputer();
        if( computer != null ) computer.setLabel( label );
        setChanged();
    }

    @Override
    public ComputerFamily getFamily()
    {
        return family;
    }

    public ServerComputer createServerComputer()
    {
        if( getLevel().isClientSide ) return null;

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

        if( changed ) updateInput();
        return ComputerCraft.serverComputerRegistry.get( instanceID );
    }

    public ServerComputer getServerComputer()
    {
        return getLevel().isClientSide ? null : ComputerCraft.serverComputerRegistry.get( instanceID );
    }

    // Networking stuff

    @Override
    protected void writeDescription( @Nonnull CompoundTag nbt )
    {
        super.writeDescription( nbt );
        if( label != null ) nbt.putString( NBT_LABEL, label );
        if( computerID >= 0 ) nbt.putInt( NBT_ID, computerID );
    }

    @Override
    protected void readDescription( @Nonnull CompoundTag nbt )
    {
        super.readDescription( nbt );
        label = nbt.contains( NBT_LABEL ) ? nbt.getString( NBT_LABEL ) : null;
        computerID = nbt.contains( NBT_ID ) ? nbt.getInt( NBT_ID ) : -1;
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
    public Component getName()
    {
        return hasCustomName()
            ? new TextComponent( label )
            : new TranslatableComponent( getBlockState().getBlock().getDescriptionId() );
    }

    @Override
    public boolean hasCustomName()
    {
        return !Strings.isNullOrEmpty( label );
    }

    @Nullable
    @Override
    public Component getCustomName()
    {
        return hasCustomName() ? new TextComponent( label ) : null;
    }

    @Nonnull
    @Override
    public Component getDisplayName()
    {
        return Nameable.super.getDisplayName();
    }
}
