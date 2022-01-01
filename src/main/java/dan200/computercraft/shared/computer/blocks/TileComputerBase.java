/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
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
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
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

    private int invalidSides = 0;
    private final NonNullConsumer<Object>[] invalidate;

    private final ComputerFamily family;

    public TileComputerBase( BlockEntityType<? extends TileGeneric> type, BlockPos pos, BlockState state, ComputerFamily family )
    {
        super( type, pos, state );
        this.family = family;

        // We cache these so we can guarantee we only ever register one listener for adjacent capabilities.
        @SuppressWarnings( { "unchecked", "rawtypes" } )
        NonNullConsumer<Object>[] invalidate = this.invalidate = new NonNullConsumer[6];
        for( Direction direction : Direction.values() )
        {
            int mask = 1 << direction.ordinal();
            invalidate[direction.ordinal()] = o -> invalidSides |= mask;
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
        updateInputAt( neighbour );
    }

    @Override
    public void onNeighbourTileEntityChange( @Nonnull BlockPos neighbour )
    {
        updateInputAt( neighbour );
    }

    protected void serverTick()
    {
        ServerComputer computer = createServerComputer();

        if( invalidSides != 0 )
        {
            for( Direction direction : DirectionUtil.FACINGS )
            {
                if( (invalidSides & (1 << direction.ordinal())) != 0 ) refreshPeripheral( computer, direction );
            }
        }

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

    @Override
    public void saveAdditional( @Nonnull CompoundTag nbt )
    {
        // Save ID, label and power state
        if( computerID >= 0 ) nbt.putInt( NBT_ID, computerID );
        if( label != null ) nbt.putString( NBT_LABEL, label );
        nbt.putBoolean( NBT_ON, on );

        super.saveAdditional( nbt );
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

    private void updateRedstoneInput( @Nonnull ServerComputer computer, Direction dir, BlockPos targetPos )
    {
        Direction offsetSide = dir.getOpposite();
        ComputerSide localDir = remapToLocalSide( dir );

        computer.setRedstoneInput( localDir, RedstoneUtil.getRedstoneInput( level, targetPos, dir ) );
        computer.setBundledRedstoneInput( localDir, BundledRedstone.getOutput( getLevel(), targetPos, offsetSide ) );
    }

    private void refreshPeripheral( @Nonnull ServerComputer computer, Direction dir )
    {
        invalidSides &= ~(1 << dir.ordinal());

        ComputerSide localDir = remapToLocalSide( dir );
        if( isPeripheralBlockedOnSide( localDir ) ) return;

        Direction offsetSide = dir.getOpposite();
        IPeripheral peripheral = Peripherals.getPeripheral( getLevel(), getBlockPos().relative( dir ), offsetSide, invalidate[dir.ordinal()] );
        computer.setPeripheral( localDir, peripheral );
    }

    public void updateInputsImmediately()
    {
        ServerComputer computer = getServerComputer();
        if( computer != null ) updateInputsImmediately( computer );
    }

    /**
     * Update all redstone and peripherals.
     *
     * This should only be really be called when the computer is being ticked (though there are some cases where it
     * won't be), as peripheral scanning requires adjacent tiles to be in a "correct" state - which may not be the case
     * if they're still updating!
     *
     * @param computer The current computer instance.
     */
    private void updateInputsImmediately( @Nonnull ServerComputer computer )
    {
        BlockPos pos = getBlockPos();
        for( Direction dir : DirectionUtil.FACINGS )
        {
            updateRedstoneInput( computer, dir, pos.relative( dir ) );
            refreshPeripheral( computer, dir );
        }
    }

    private void updateInputAt( @Nonnull BlockPos neighbour )
    {
        ServerComputer computer = getServerComputer();
        if( computer == null ) return;

        for( Direction dir : DirectionUtil.FACINGS )
        {
            BlockPos offset = getBlockPos().relative( dir );
            if( offset.equals( neighbour ) )
            {
                updateRedstoneInput( computer, dir, offset );
                invalidSides |= 1 << dir.ordinal();
                return;
            }
        }

        // If the position is not any adjacent one, update all inputs. This is pretty terrible, but some redstone mods
        // handle this incorrectly.
        BlockPos pos = getBlockPos();
        for( Direction dir : DirectionUtil.FACINGS ) updateRedstoneInput( computer, dir, pos.relative( dir ) );
        invalidSides = (1 << 6) - 1; // Mark all peripherals as dirty.
    }

    /**
     * Update the block's state and propagate redstone output.
     */
    public void updateOutput()
    {
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

    @Nonnull
    public ServerComputer createServerComputer()
    {
        if( getLevel().isClientSide ) throw new IllegalStateException( "Cannot access server computer on the client." );

        boolean changed = false;
        if( instanceID < 0 )
        {
            instanceID = ComputerCraft.serverComputerRegistry.getUnusedInstanceID();
            changed = true;
        }

        ServerComputer computer = ComputerCraft.serverComputerRegistry.get( instanceID );
        if( computer == null )
        {
            computer = createComputer( instanceID, computerID );
            ComputerCraft.serverComputerRegistry.add( instanceID, computer );
            fresh = true;
            changed = true;
        }

        if( changed ) updateInputsImmediately( computer );
        return computer;
    }

    @Nullable
    public ServerComputer getServerComputer()
    {
        return getLevel().isClientSide ? null : ComputerCraft.serverComputerRegistry.get( instanceID );
    }

    // Networking stuff

    @Nonnull
    @Override
    public final ClientboundBlockEntityDataPacket getUpdatePacket()
    {
        return ClientboundBlockEntityDataPacket.create( this );
    }

    @Nonnull
    @Override
    public CompoundTag getUpdateTag()
    {
        // We need this for pick block on the client side.
        CompoundTag nbt = super.getUpdateTag();
        if( label != null ) nbt.putString( NBT_LABEL, label );
        if( computerID >= 0 ) nbt.putInt( NBT_ID, computerID );
        return nbt;
    }

    @Override
    public void handleUpdateTag( @Nonnull CompoundTag nbt )
    {
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
