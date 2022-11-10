/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.blocks;

import com.google.common.base.Strings;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.impl.BundledRedstone;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ComputerState;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.core.ServerContext;
import dan200.computercraft.shared.network.container.ComputerContainerData;
import dan200.computercraft.shared.platform.ComponentAccess;
import dan200.computercraft.shared.platform.PlatformHelper;
import dan200.computercraft.shared.util.BlockEntityHelpers;
import dan200.computercraft.shared.util.DirectionUtil;
import dan200.computercraft.shared.util.IDAssigner;
import dan200.computercraft.shared.util.RedstoneUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.Objects;

public abstract class AbstractComputerBlockEntity extends BlockEntity implements IComputerBlockEntity, Nameable, MenuProvider {
    private static final String NBT_ID = "ComputerId";
    private static final String NBT_LABEL = "Label";
    private static final String NBT_ON = "On";

    private int instanceID = -1;
    private int computerID = -1;
    protected @Nullable String label = null;
    private boolean on = false;
    boolean startOn = false;
    private boolean fresh = false;

    private int invalidSides = 0;
    private final ComponentAccess<IPeripheral> peripherals = PlatformHelper.get().createPeripheralAccess(d -> invalidSides |= 1 << d.ordinal());

    private LockCode lockCode = LockCode.NO_LOCK;

    private final ComputerFamily family;

    public AbstractComputerBlockEntity(BlockEntityType<? extends AbstractComputerBlockEntity> type, BlockPos pos, BlockState state, ComputerFamily family) {
        super(type, pos, state);
        this.family = family;
    }

    protected void unload() {
        if (getLevel().isClientSide) return;

        var computer = getServerComputer();
        if (computer != null) computer.close();
        instanceID = -1;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        unload();
    }

    protected boolean canNameWithTag(Player player) {
        return false;
    }

    protected double getInteractRange() {
        return BlockEntityHelpers.DEFAULT_INTERACT_RANGE;
    }

    public boolean isUsable(Player player) {
        return BaseContainerBlockEntity.canUnlock(player, lockCode, getDisplayName())
            && BlockEntityHelpers.isUsable(this, player, getInteractRange());
    }

    public InteractionResult use(Player player, InteractionHand hand) {
        var currentItem = player.getItemInHand(hand);
        if (!currentItem.isEmpty() && currentItem.getItem() == Items.NAME_TAG && canNameWithTag(player) && currentItem.hasCustomHoverName()) {
            // Label to rename computer
            if (!getLevel().isClientSide) {
                setLabel(currentItem.getHoverName().getString());
                currentItem.shrink(1);
            }
            return InteractionResult.SUCCESS;
        } else if (!player.isCrouching()) {
            // Regular right click to activate computer
            if (!getLevel().isClientSide && isUsable(player)) {
                var computer = createServerComputer();
                computer.turnOn();

                var stack = getBlockState().getBlock() instanceof AbstractComputerBlock<?>
                    ? ((AbstractComputerBlock<?>) getBlockState().getBlock()).getItem(this)
                    : ItemStack.EMPTY;
                new ComputerContainerData(computer, stack).open(player, this);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    public void neighborChanged(BlockPos neighbour) {
        updateInputAt(neighbour);
    }

    protected void serverTick() {
        if (getLevel().isClientSide) return;
        if (computerID < 0 && !startOn) return; // Don't tick if we don't need a computer!

        var computer = createServerComputer();

        if (invalidSides != 0) {
            for (var direction : DirectionUtil.FACINGS) {
                if ((invalidSides & (1 << direction.ordinal())) != 0) refreshPeripheral(computer, direction);
            }
        }

        // If the computer isn't on and should be, then turn it on
        if (startOn || (fresh && on)) {
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
        updateBlockState(computer.getState());

        // TODO: This should ideally be split up into label/id/on (which should save NBT and sync to client) and
        //  redstone (which should update outputs)
        if (computer.hasOutputChanged()) updateOutput();
    }

    protected abstract void updateBlockState(ComputerState newState);

    @Override
    public void saveAdditional(CompoundTag nbt) {
        // Save ID, label and power state
        if (computerID >= 0) nbt.putInt(NBT_ID, computerID);
        if (label != null) nbt.putString(NBT_LABEL, label);
        nbt.putBoolean(NBT_ON, on);

        lockCode.addToTag(nbt);

        super.saveAdditional(nbt);
    }

    @Override
    public final void load(CompoundTag nbt) {
        super.load(nbt);
        if (level != null && level.isClientSide) {
            loadClient(nbt);
        } else {
            loadServer(nbt);
        }
    }

    protected void loadServer(CompoundTag nbt) {
        // Load ID, label and power state
        computerID = nbt.contains(NBT_ID) ? nbt.getInt(NBT_ID) : -1;
        label = nbt.contains(NBT_LABEL) ? nbt.getString(NBT_LABEL) : null;
        on = startOn = nbt.getBoolean(NBT_ON);

        lockCode = LockCode.fromTag(nbt);
    }

    protected boolean isPeripheralBlockedOnSide(ComputerSide localSide) {
        return false;
    }

    protected abstract Direction getDirection();

    protected ComputerSide remapToLocalSide(Direction globalSide) {
        return remapLocalSide(DirectionUtil.toLocal(getDirection(), globalSide));
    }

    protected ComputerSide remapLocalSide(ComputerSide localSide) {
        return localSide;
    }

    private void updateRedstoneInput(ServerComputer computer, Direction dir, BlockPos targetPos) {
        var offsetSide = dir.getOpposite();
        var localDir = remapToLocalSide(dir);

        computer.setRedstoneInput(localDir, RedstoneUtil.getRedstoneInput(level, targetPos, dir));
        computer.setBundledRedstoneInput(localDir, BundledRedstone.getOutput(getLevel(), targetPos, offsetSide));
    }

    private void refreshPeripheral(ServerComputer computer, Direction dir) {
        invalidSides &= ~(1 << dir.ordinal());

        var localDir = remapToLocalSide(dir);
        if (isPeripheralBlockedOnSide(localDir)) return;

        var peripheral = peripherals.get((ServerLevel) getLevel(), getBlockPos(), dir);
        computer.setPeripheral(localDir, peripheral);
    }

    public void updateInputsImmediately() {
        var computer = getServerComputer();
        if (computer != null) updateInputsImmediately(computer);
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
    private void updateInputsImmediately(ServerComputer computer) {
        var pos = getBlockPos();
        for (var dir : DirectionUtil.FACINGS) {
            updateRedstoneInput(computer, dir, pos.relative(dir));
            refreshPeripheral(computer, dir);
        }
    }

    private void updateInputAt(BlockPos neighbour) {
        var computer = getServerComputer();
        if (computer == null) return;

        for (var dir : DirectionUtil.FACINGS) {
            var offset = getBlockPos().relative(dir);
            if (offset.equals(neighbour)) {
                updateRedstoneInput(computer, dir, offset);
                invalidSides |= 1 << dir.ordinal();
                return;
            }
        }

        // If the position is not any adjacent one, update all inputs. This is pretty terrible, but some redstone mods
        // handle this incorrectly.
        var pos = getBlockPos();
        for (var dir : DirectionUtil.FACINGS) updateRedstoneInput(computer, dir, pos.relative(dir));
        invalidSides = (1 << 6) - 1; // Mark all peripherals as dirty.
    }

    /**
     * Update the block's state and propagate redstone output.
     */
    public void updateOutput() {
        BlockEntityHelpers.updateBlock(this);
        for (var dir : DirectionUtil.FACINGS) {
            RedstoneUtil.propagateRedstoneOutput(getLevel(), getBlockPos(), dir);
        }
    }

    protected abstract ServerComputer createComputer(int id);

    @Override
    public final int getComputerID() {
        return computerID;
    }

    @Override
    public final @Nullable String getLabel() {
        return label;
    }

    @Override
    public final void setComputerID(int id) {
        if (getLevel().isClientSide || computerID == id) return;

        computerID = id;
        setChanged();
    }

    @Override
    public final void setLabel(@Nullable String label) {
        if (getLevel().isClientSide || Objects.equals(this.label, label)) return;

        this.label = label;
        var computer = getServerComputer();
        if (computer != null) computer.setLabel(label);
        setChanged();
    }

    @Override
    public ComputerFamily getFamily() {
        return family;
    }

    public final ServerComputer createServerComputer() {
        var server = getLevel().getServer();
        if (server == null) throw new IllegalStateException("Cannot access server computer on the client.");

        var changed = false;

        var computer = ServerContext.get(server).registry().get(instanceID);
        if (computer == null) {
            if (computerID < 0) {
                computerID = ComputerCraftAPI.createUniqueNumberedSaveDir(level, IDAssigner.COMPUTER);
                BlockEntityHelpers.updateBlock(this);
            }

            computer = createComputer(computerID);
            instanceID = computer.register();
            fresh = true;
            changed = true;
        }

        if (changed) updateInputsImmediately(computer);
        return computer;
    }

    @Nullable
    public ServerComputer getServerComputer() {
        return getLevel().isClientSide || getLevel().getServer() == null ? null : ServerContext.get(getLevel().getServer()).registry().get(instanceID);
    }

    // Networking stuff

    @Override
    public final ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        // We need this for pick block on the client side.
        var nbt = super.getUpdateTag();
        if (label != null) nbt.putString(NBT_LABEL, label);
        if (computerID >= 0) nbt.putInt(NBT_ID, computerID);
        return nbt;
    }

    protected void loadClient(CompoundTag nbt) {
        label = nbt.contains(NBT_LABEL) ? nbt.getString(NBT_LABEL) : null;
        computerID = nbt.contains(NBT_ID) ? nbt.getInt(NBT_ID) : -1;
    }

    protected void transferStateFrom(AbstractComputerBlockEntity copy) {
        if (copy.computerID != computerID || copy.instanceID != instanceID) {
            unload();
            instanceID = copy.instanceID;
            computerID = copy.computerID;
            label = copy.label;
            on = copy.on;
            startOn = copy.startOn;
            lockCode = copy.lockCode;
            BlockEntityHelpers.updateBlock(this);
        }
        copy.instanceID = -1;
    }

    @Override
    public Component getName() {
        return hasCustomName()
            ? Component.literal(label)
            : Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public boolean hasCustomName() {
        return !Strings.isNullOrEmpty(label);
    }

    @Nullable
    @Override
    public Component getCustomName() {
        return hasCustomName() ? Component.literal(label) : null;
    }

    @Override
    public Component getDisplayName() {
        return Nameable.super.getDisplayName();
    }
}
