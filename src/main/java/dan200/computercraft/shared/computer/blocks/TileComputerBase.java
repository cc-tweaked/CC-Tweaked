/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.blocks;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.shared.BundledRedstone;
import dan200.computercraft.shared.Peripherals;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ComputerState;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.core.ServerContext;
import dan200.computercraft.shared.network.container.ComputerContainerData;
import dan200.computercraft.shared.util.DirectionUtil;
import dan200.computercraft.shared.util.IDAssigner;
import dan200.computercraft.shared.util.RedstoneUtil;
import joptsimple.internal.Strings;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.LockableTileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.INameable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.LockCode;
import net.minecraftforge.common.util.NonNullConsumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public abstract class TileComputerBase extends TileGeneric implements IComputerTile, ITickableTileEntity, INameable, INamedContainerProvider
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

    private LockCode lockCode = LockCode.NO_LOCK;

    private final ComputerFamily family;

    public TileComputerBase( TileEntityType<? extends TileGeneric> type, ComputerFamily family )
    {
        super( type );
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
        if( getLevel().isClientSide ) return;

        ServerComputer computer = getServerComputer();
        if( computer != null ) computer.close();
        instanceID = -1;
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

    protected boolean canNameWithTag( PlayerEntity player )
    {
        return false;
    }

    @Override
    public boolean isUsable( PlayerEntity player )
    {
        return super.isUsable( player ) && LockableTileEntity.canUnlock( player, lockCode, getDisplayName() );
    }

    @Nonnull
    @Override
    public ActionResultType onActivate( PlayerEntity player, Hand hand, BlockRayTraceResult hit )
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
            return ActionResultType.SUCCESS;
        }
        else if( !player.isCrouching() )
        {
            // Regular right click to activate computer
            if( !getLevel().isClientSide && isUsable( player ) )
            {
                ServerComputer computer = createServerComputer();
                computer.turnOn();
                new ComputerContainerData( computer ).open( player, this );
            }
            return ActionResultType.SUCCESS;
        }
        return ActionResultType.PASS;
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

    @Override
    public void tick()
    {
        if( getLevel().isClientSide ) return;
        if( computerID < 0 && !startOn ) return; // Don't tick if we don't need a computer!

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

    @Nonnull
    @Override
    public CompoundNBT save( @Nonnull CompoundNBT nbt )
    {
        // Save ID, label and power state
        if( computerID >= 0 ) nbt.putInt( NBT_ID, computerID );
        if( label != null ) nbt.putString( NBT_LABEL, label );
        nbt.putBoolean( NBT_ON, on );

        lockCode.addToTag( nbt );

        return super.save( nbt );
    }

    @Override
    public void load( @Nonnull BlockState state, @Nonnull CompoundNBT nbt )
    {
        super.load( state, nbt );

        // Load ID, label and power state
        computerID = nbt.contains( NBT_ID ) ? nbt.getInt( NBT_ID ) : -1;
        label = nbt.contains( NBT_LABEL ) ? nbt.getString( NBT_LABEL ) : null;
        on = startOn = nbt.getBoolean( NBT_ON );

        lockCode = LockCode.fromTag( nbt );
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
     * <p>
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

    protected abstract ServerComputer createComputer( int id );

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
    public final ServerComputer createServerComputer()
    {
        if( getLevel().isClientSide ) throw new IllegalStateException( "Cannot access server computer on the client." );

        boolean changed = false;

        ServerComputer computer = ServerContext.get( getLevel().getServer() ).registry().get( instanceID );
        if( computer == null )
        {
            if( computerID < 0 )
            {
                computerID = ComputerCraftAPI.createUniqueNumberedSaveDir( level, IDAssigner.COMPUTER );
                updateBlock();
            }

            computer = createComputer( computerID );
            instanceID = computer.register();
            fresh = true;
            changed = true;
        }

        if( changed ) updateInputsImmediately( computer );
        return computer;
    }

    @Nullable
    public ServerComputer getServerComputer()
    {
        return getLevel().isClientSide ? null : ServerContext.get( getLevel().getServer() ).registry().get( instanceID );
    }

    // Networking stuff

    @Nonnull
    @Override
    public final SUpdateTileEntityPacket getUpdatePacket()
    {
        return new SUpdateTileEntityPacket( worldPosition, 0, getUpdateTag() );
    }

    @Nonnull
    @Override
    public CompoundNBT getUpdateTag()
    {
        // We need this for pick block on the client side.
        CompoundNBT nbt = super.getUpdateTag();
        if( label != null ) nbt.putString( NBT_LABEL, label );
        if( computerID >= 0 ) nbt.putInt( NBT_ID, computerID );
        return nbt;
    }

    @Override
    public void handleUpdateTag( @Nonnull CompoundNBT nbt )
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
            lockCode = copy.lockCode;
            updateBlock();
        }
        copy.instanceID = -1;
    }

    @Nonnull
    @Override
    public ITextComponent getName()
    {
        return hasCustomName()
            ? new StringTextComponent( label )
            : new TranslationTextComponent( getBlockState().getBlock().getDescriptionId() );
    }

    @Override
    public boolean hasCustomName()
    {
        return !Strings.isNullOrEmpty( label );
    }

    @Nullable
    @Override
    public ITextComponent getCustomName()
    {
        return hasCustomName() ? new StringTextComponent( label ) : null;
    }

    @Nonnull
    @Override
    public ITextComponent getDisplayName()
    {
        return INameable.super.getDisplayName();
    }
}
