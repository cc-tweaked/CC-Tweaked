/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.blocks;

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;

public abstract class TileComputerBase extends TileGeneric implements IComputerTile, Tickable, IPeripheralTile, Nameable, NamedScreenHandlerFactory,
                                                                          ExtendedScreenHandlerFactory {
    private static final String NBT_ID = "ComputerId";
    private static final String NBT_LABEL = "Label";
    private static final String NBT_ON = "On";
    private final ComputerFamily family;
    protected String label = null;
    boolean m_startOn = false;
    private int m_instanceID = -1;
    private int m_computerID = -1;
    private boolean m_on = false;
    private boolean m_fresh = false;

    public TileComputerBase(BlockEntityType<? extends TileGeneric> type, ComputerFamily family) {
        super(type);
        this.family = family;
    }

    @Override
    public void destroy() {
        this.unload();
        for (Direction dir : DirectionUtil.FACINGS) {
            RedstoneUtil.propagateRedstoneOutput(this.getWorld(), this.getPos(), dir);
        }
    }

    protected void unload() {
        if (this.m_instanceID >= 0) {
            if (!this.getWorld().isClient) {
                ComputerCraft.serverComputerRegistry.remove(this.m_instanceID);
            }
            this.m_instanceID = -1;
        }
    }

    @Nonnull
    @Override
    public ActionResult onActivate(PlayerEntity player, Hand hand, BlockHitResult hit) {
        ItemStack currentItem = player.getStackInHand(hand);
        if (!currentItem.isEmpty() && currentItem.getItem() == Items.NAME_TAG && this.canNameWithTag(player) && currentItem.hasCustomName()) {
            // Label to rename computer
            if (!this.getWorld().isClient) {
                this.setLabel(currentItem.getName()
                                         .getString());
                currentItem.decrement(1);
            }
            return ActionResult.SUCCESS;
        } else if (!player.isInSneakingPose()) {
            // Regular right click to activate computer
            if (!this.getWorld().isClient && this.isUsable(player, false)) {
                this.createServerComputer().turnOn();
                this.createServerComputer().sendTerminalState(player);
                new ComputerContainerData(this.createServerComputer()).open(player, this);
            }
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    protected boolean canNameWithTag(PlayerEntity player) {
        return false;
    }

    public ServerComputer createServerComputer() {
        if (this.getWorld().isClient) {
            return null;
        }

        boolean changed = false;
        if (this.m_instanceID < 0) {
            this.m_instanceID = ComputerCraft.serverComputerRegistry.getUnusedInstanceID();
            changed = true;
        }
        if (!ComputerCraft.serverComputerRegistry.contains(this.m_instanceID)) {
            ServerComputer computer = this.createComputer(this.m_instanceID, this.m_computerID);
            ComputerCraft.serverComputerRegistry.add(this.m_instanceID, computer);
            this.m_fresh = true;
            changed = true;
        }
        if (changed) {
            this.updateBlock();
            this.updateInput();
        }
        return ComputerCraft.serverComputerRegistry.get(this.m_instanceID);
    }

    public ServerComputer getServerComputer() {
        return this.getWorld().isClient ? null : ComputerCraft.serverComputerRegistry.get(this.m_instanceID);
    }

    protected abstract ServerComputer createComputer(int instanceID, int id);

    public void updateInput() {
        if (this.getWorld() == null || this.getWorld().isClient) {
            return;
        }

        // Update all sides
        ServerComputer computer = this.getServerComputer();
        if (computer == null) {
            return;
        }

        BlockPos pos = computer.getPosition();
        for (Direction dir : DirectionUtil.FACINGS) {
            this.updateSideInput(computer, dir, pos.offset(dir));
        }
    }

    private void updateSideInput(ServerComputer computer, Direction dir, BlockPos offset) {
        Direction offsetSide = dir.getOpposite();
        ComputerSide localDir = this.remapToLocalSide(dir);

        computer.setRedstoneInput(localDir, getRedstoneInput(this.world, offset, dir));
        computer.setBundledRedstoneInput(localDir, BundledRedstone.getOutput(this.getWorld(), offset, offsetSide));
        if (!this.isPeripheralBlockedOnSide(localDir)) {
            IPeripheral peripheral = Peripherals.getPeripheral(this.getWorld(), offset, offsetSide);
            computer.setPeripheral(localDir, peripheral);
        }
    }

    protected ComputerSide remapToLocalSide(Direction globalSide) {
        return this.remapLocalSide(DirectionUtil.toLocal(this.getDirection(), globalSide));
    }

    /**
     * Gets the redstone input for an adjacent block.
     *
     * @param world The world we exist in
     * @param pos The position of the neighbour
     * @param side The side we are reading from
     * @return The effective redstone power
     */
    protected static int getRedstoneInput(World world, BlockPos pos, Direction side) {
        int power = world.getEmittedRedstonePower(pos, side);
        if (power >= 15) {
            return power;
        }

        BlockState neighbour = world.getBlockState(pos);
        return neighbour.getBlock() == Blocks.REDSTONE_WIRE ? Math.max(power, neighbour.get(RedstoneWireBlock.POWER)) : power;
    }

    protected boolean isPeripheralBlockedOnSide(ComputerSide localSide) {
        return false;
    }

    protected ComputerSide remapLocalSide(ComputerSide localSide) {
        return localSide;
    }

    protected abstract Direction getDirection();

    @Override
    public void onNeighbourChange(@Nonnull BlockPos neighbour) {
        this.updateInput(neighbour);
    }

    @Override
    public void onNeighbourTileEntityChange(@Nonnull BlockPos neighbour) {
        this.updateInput(neighbour);
    }

    @Override
    protected void readDescription(@Nonnull CompoundTag nbt) {
        super.readDescription(nbt);
        this.label = nbt.contains(NBT_LABEL) ? nbt.getString(NBT_LABEL) : null;
        this.m_computerID = nbt.contains(NBT_ID) ? nbt.getInt(NBT_ID) : -1;
    }

    @Override
    protected void writeDescription(@Nonnull CompoundTag nbt) {
        super.writeDescription(nbt);
        if (this.label != null) {
            nbt.putString(NBT_LABEL, this.label);
        }
        if (this.m_computerID >= 0) {
            nbt.putInt(NBT_ID, this.m_computerID);
        }
    }

    @Override
    public void tick() {
        if (!this.getWorld().isClient) {
            ServerComputer computer = this.createServerComputer();
            if (computer == null) {
                return;
            }

            // If the computer isn't on and should be, then turn it on
            if (this.m_startOn || (this.m_fresh && this.m_on)) {
                computer.turnOn();
                this.m_startOn = false;
            }

            computer.keepAlive();

            this.m_fresh = false;
            this.m_computerID = computer.getID();
            this.label = computer.getLabel();
            this.m_on = computer.isOn();

            if (computer.hasOutputChanged()) {
                this.updateOutput();
            }

            // Update the block state if needed. We don't fire a block update intentionally,
            // as this only really is needed on the client side.
            this.updateBlockState(computer.getState());

            if (computer.hasOutputChanged()) {
                this.updateOutput();
            }
        }
    }

    public void updateOutput() {
        // Update redstone
        this.updateBlock();
        for (Direction dir : DirectionUtil.FACINGS) {
            RedstoneUtil.propagateRedstoneOutput(this.getWorld(), this.getPos(), dir);
        }
    }

    protected abstract void updateBlockState(ComputerState newState);

    @Override
    public void fromTag(@Nonnull BlockState state, @Nonnull CompoundTag nbt) {
        super.fromTag(state, nbt);

        // Load ID, label and power state
        this.m_computerID = nbt.contains(NBT_ID) ? nbt.getInt(NBT_ID) : -1;
        this.label = nbt.contains(NBT_LABEL) ? nbt.getString(NBT_LABEL) : null;
        this.m_on = this.m_startOn = nbt.getBoolean(NBT_ON);
    }

    @Nonnull
    @Override
    public CompoundTag toTag(@Nonnull CompoundTag nbt) {
        // Save ID, label and power state
        if (this.m_computerID >= 0) {
            nbt.putInt(NBT_ID, this.m_computerID);
        }
        if (this.label != null) {
            nbt.putString(NBT_LABEL, this.label);
        }
        nbt.putBoolean(NBT_ON, this.m_on);

        return super.toTag(nbt);
    }

    @Override
    public void markRemoved() {
        this.unload();
        super.markRemoved();
    }

    private void updateInput(BlockPos neighbour) {
        if (this.getWorld() == null || this.getWorld().isClient) {
            return;
        }

        ServerComputer computer = this.getServerComputer();
        if (computer == null) {
            return;
        }

        for (Direction dir : DirectionUtil.FACINGS) {
            BlockPos offset = this.pos.offset(dir);
            if (offset.equals(neighbour)) {
                this.updateSideInput(computer, dir, offset);
                return;
            }
        }

        // If the position is not any adjacent one, update all inputs.
        this.updateInput();
    }

    private void updateInput(Direction dir) {
        if (this.getWorld() == null || this.getWorld().isClient) {
            return;
        }

        ServerComputer computer = this.getServerComputer();
        if (computer == null) {
            return;
        }

        this.updateSideInput(computer, dir, this.pos.offset(dir));
    }

    @Override
    public final int getComputerID() {
        return this.m_computerID;
    }

    @Override
    public final void setComputerID(int id) {
        if (this.getWorld().isClient || this.m_computerID == id) {
            return;
        }

        this.m_computerID = id;
        ServerComputer computer = this.getServerComputer();
        if (computer != null) {
            computer.setID(this.m_computerID);
        }
        this.markDirty();
    }

    @Override
    public final String getLabel() {
        return this.label;
    }

    // Networking stuff

    @Override
    public final void setLabel(String label) {
        if (this.getWorld().isClient || Objects.equals(this.label, label)) {
            return;
        }

        this.label = label;
        ServerComputer computer = this.getServerComputer();
        if (computer != null) {
            computer.setLabel(label);
        }
        this.markDirty();
    }

    @Override
    public ComputerFamily getFamily() {
        return this.family;
    }

    protected void transferStateFrom(TileComputerBase copy) {
        if (copy.m_computerID != this.m_computerID || copy.m_instanceID != this.m_instanceID) {
            this.unload();
            this.m_instanceID = copy.m_instanceID;
            this.m_computerID = copy.m_computerID;
            this.label = copy.label;
            this.m_on = copy.m_on;
            this.m_startOn = copy.m_startOn;
            this.updateBlock();
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
    public Text getName() {
        return this.hasCustomName() ? new LiteralText(this.label) : new TranslatableText(this.getCachedState().getBlock()
                                                                                             .getTranslationKey());
    }

    @Override
    public boolean hasCustomName() {
        return !Strings.isNullOrEmpty(this.label);
    }

    @Nonnull
    @Override
    public Text getDisplayName() {
        return Nameable.super.getDisplayName();
    }

    @Nullable
    @Override
    public Text getCustomName() {
        return this.hasCustomName() ? new LiteralText(this.label) : null;
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity serverPlayerEntity, PacketByteBuf packetByteBuf) {
        packetByteBuf.writeInt(this.getServerComputer().getInstanceID());
        packetByteBuf.writeEnumConstant(this.getServerComputer().getFamily());
    }
}
