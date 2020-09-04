/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.blocks;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralProvider;
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
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Nameable;
import net.minecraft.util.Tickable;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public abstract class TileComputerBase extends TileGeneric implements IComputerTile, Tickable, IPeripheralTile, Nameable, NamedScreenHandlerFactory, ExtendedScreenHandlerFactory
{
    private static final String NBT_ID = "ComputerId";
    private static final String NBT_LABEL = "Label";
    private static final String NBT_ON = "On";

    private int m_instanceID = -1;
    private int m_computerID = -1;
    protected String label = null;
    private boolean m_on = false;
    boolean m_startOn = false;
    private boolean m_fresh = false;

    private final ComputerFamily family;

    public TileComputerBase( BlockEntityType<? extends TileGeneric> type, ComputerFamily family )
    {
        super( type );
        this.family = family;
    }

    protected void unload()
    {
        if( m_instanceID >= 0 )
        {
            if( !getWorld().isClient ) ComputerCraft.serverComputerRegistry.remove( m_instanceID );
            m_instanceID = -1;
        }
    }

    @Override
    public void destroy()
    {
        unload();
        for( Direction dir : DirectionUtil.FACINGS )
        {
            RedstoneUtil.propagateRedstoneOutput( getWorld(), getPos(), dir );
        }
    }

    @Override
    public void markRemoved()
    {
        unload();
        super.markRemoved();
    }

    protected boolean canNameWithTag( PlayerEntity player )
    {
        return false;
    }

    @Nonnull
    @Override
    public ActionResult onActivate( PlayerEntity player, Hand hand, BlockHitResult hit )
    {
        ItemStack currentItem = player.getStackInHand( hand );
        if( !currentItem.isEmpty() && currentItem.getItem() == Items.NAME_TAG && canNameWithTag( player ) && currentItem.hasCustomName() )
        {
            // Label to rename computer
            if( !getWorld().isClient )
            {
                setLabel( currentItem.getName().getString() );
                currentItem.decrement( 1 );
            }
            return ActionResult.SUCCESS;
        }
        else if( !player.isInSneakingPose() )
        {
            // Regular right click to activate computer
            if( !getWorld().isClient && isUsable( player, false ) )
            {
                createServerComputer().turnOn();
                createServerComputer().sendTerminalState(player);
                new ComputerContainerData( createServerComputer() ).open( player, this );
            }
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
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

    @Override
    public void tick()
    {
        if( !getWorld().isClient )
        {
            ServerComputer computer = createServerComputer();
            if( computer == null ) return;

            // If the computer isn't on and should be, then turn it on
            if( m_startOn || (m_fresh && m_on) )
            {
                computer.turnOn();
                m_startOn = false;
            }

            computer.keepAlive();

            m_fresh = false;
            m_computerID = computer.getID();
            label = computer.getLabel();
            m_on = computer.isOn();

            if( computer.hasOutputChanged() ) updateOutput();

            // Update the block state if needed. We don't fire a block update intentionally,
            // as this only really is needed on the client side.
            updateBlockState( computer.getState() );

            if( computer.hasOutputChanged() ) updateOutput();
        }
    }

    protected abstract void updateBlockState( ComputerState newState );

    @Nonnull
    @Override
    public CompoundTag toTag( @Nonnull CompoundTag nbt )
    {
        // Save ID, label and power state
        if( m_computerID >= 0 ) nbt.putInt( NBT_ID, m_computerID );
        if( label != null ) nbt.putString( NBT_LABEL, label );
        nbt.putBoolean( NBT_ON, m_on );

        return super.toTag( nbt );
    }

    @Override
    public void fromTag( @Nonnull BlockState state, @Nonnull CompoundTag nbt )
    {
        super.fromTag( state, nbt );

        // Load ID, label and power state
        m_computerID = nbt.contains( NBT_ID ) ? nbt.getInt( NBT_ID ) : -1;
        label = nbt.contains( NBT_LABEL ) ? nbt.getString( NBT_LABEL ) : null;
        m_on = m_startOn = nbt.getBoolean( NBT_ON );
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

        computer.setRedstoneInput( localDir, getRedstoneInput( world, offset, dir ) );
        computer.setBundledRedstoneInput( localDir, BundledRedstone.getOutput( getWorld(), offset, offsetSide ) );
        if( !isPeripheralBlockedOnSide( localDir ) )
        {
            IPeripheral peripheral = Peripherals.getPeripheral( getWorld(), offset, offsetSide);
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
     */
    protected static int getRedstoneInput( World world, BlockPos pos, Direction side )
    {
        int power = world.getEmittedRedstonePower( pos, side );
        if( power >= 15 ) return power;

        BlockState neighbour = world.getBlockState( pos );
        return neighbour.getBlock() == Blocks.REDSTONE_WIRE
            ? Math.max( power, neighbour.get( RedstoneWireBlock.POWER ) )
            : power;
    }

    public void updateInput()
    {
        if( getWorld() == null || getWorld().isClient ) return;

        // Update all sides
        ServerComputer computer = getServerComputer();
        if( computer == null ) return;

        BlockPos pos = computer.getPosition();
        for( Direction dir : DirectionUtil.FACINGS )
        {
            updateSideInput( computer, dir, pos.offset( dir ) );
        }
    }

    private void updateInput( BlockPos neighbour )
    {
        if( getWorld() == null || getWorld().isClient ) return;

        ServerComputer computer = getServerComputer();
        if( computer == null ) return;

        for( Direction dir : DirectionUtil.FACINGS )
        {
            BlockPos offset = pos.offset( dir );
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
        if( getWorld() == null || getWorld().isClient ) return;

        ServerComputer computer = getServerComputer();
        if( computer == null ) return;

        updateSideInput( computer, dir, pos.offset( dir ) );
    }

    public void updateOutput()
    {
        // Update redstone
        updateBlock();
        for( Direction dir : DirectionUtil.FACINGS )
        {
            RedstoneUtil.propagateRedstoneOutput( getWorld(), getPos(), dir );
        }
    }

    protected abstract ServerComputer createComputer( int instanceID, int id );

    @Override
    public final int getComputerID()
    {
        return m_computerID;
    }

    @Override
    public final String getLabel()
    {
        return label;
    }

    @Override
    public final void setComputerID( int id )
    {
        if( getWorld().isClient || m_computerID == id ) return;

        m_computerID = id;
        ServerComputer computer = getServerComputer();
        if( computer != null ) computer.setID( m_computerID );
        markDirty();
    }

    @Override
    public final void setLabel( String label )
    {
        if( getWorld().isClient || Objects.equals( this.label, label ) ) return;

        this.label = label;
        ServerComputer computer = getServerComputer();
        if( computer != null ) computer.setLabel( label );
        markDirty();
    }

    @Override
    public ComputerFamily getFamily()
    {
        return family;
    }

    public ServerComputer createServerComputer()
    {
        if( getWorld().isClient ) return null;

        boolean changed = false;
        if( m_instanceID < 0 )
        {
            m_instanceID = ComputerCraft.serverComputerRegistry.getUnusedInstanceID();
            changed = true;
        }
        if( !ComputerCraft.serverComputerRegistry.contains( m_instanceID ) )
        {
            ServerComputer computer = createComputer( m_instanceID, m_computerID );
            ComputerCraft.serverComputerRegistry.add( m_instanceID, computer );
            m_fresh = true;
            changed = true;
        }
        if( changed )
        {
            updateBlock();
            updateInput();
        }
        return ComputerCraft.serverComputerRegistry.get( m_instanceID );
    }

    public ServerComputer getServerComputer()
    {
        return getWorld().isClient ? null : ComputerCraft.serverComputerRegistry.get( m_instanceID );
    }

    // Networking stuff

    @Override
    protected void writeDescription( @Nonnull CompoundTag nbt )
    {
        super.writeDescription( nbt );
        if( label != null ) nbt.putString( NBT_LABEL, label );
        if( m_computerID >= 0 ) nbt.putInt( NBT_ID, m_computerID );
    }

    @Override
    protected void readDescription( @Nonnull CompoundTag nbt )
    {
        super.readDescription( nbt );
        label = nbt.contains( NBT_LABEL ) ? nbt.getString( NBT_LABEL ) : null;
        m_computerID = nbt.contains( NBT_ID ) ? nbt.getInt( NBT_ID ) : -1;
    }

    protected void transferStateFrom( TileComputerBase copy )
    {
        if( copy.m_computerID != m_computerID || copy.m_instanceID != m_instanceID )
        {
            unload();
            m_instanceID = copy.m_instanceID;
            m_computerID = copy.m_computerID;
            label = copy.label;
            m_on = copy.m_on;
            m_startOn = copy.m_startOn;
            updateBlock();
        }
        copy.m_instanceID = -1;
    }

    @Nonnull
    @Override
    public IPeripheral getPeripheral(Direction side) {
        return new ComputerPeripheral("computer", this.createProxy());
    }

    public abstract ComputerProxy createProxy();

    @Nonnull
    @Override
    public Text getName()
    {
        return hasCustomName()
            ? new LiteralText( label )
            : new TranslatableText( getCachedState().getBlock().getTranslationKey() );
    }

    @Override
    public boolean hasCustomName()
    {
        return !Strings.isNullOrEmpty( label );
    }

    @Nullable
    @Override
    public Text getCustomName()
    {
        return hasCustomName() ? new LiteralText( label ) : null;
    }

    @Nonnull
    @Override
    public Text getDisplayName()
    {
        return Nameable.super.getDisplayName();
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity serverPlayerEntity, PacketByteBuf packetByteBuf) {
        packetByteBuf.writeInt(getServerComputer().getInstanceID());
        packetByteBuf.writeEnumConstant(getServerComputer().getFamily());
    }
}
