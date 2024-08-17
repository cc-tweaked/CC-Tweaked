// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

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
import net.minecraft.world.Container;
import net.minecraft.world.LockCode;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;

public abstract class AbstractComputerBlockEntity extends BlockEntity implements Nameable, MenuProvider {
    private static final String NBT_ID = "ComputerId";
    private static final String NBT_LABEL = "Label";
    private static final String NBT_ON = "On";

    private @Nullable UUID instanceID = null;
    private int computerID = -1;
    protected @Nullable String label = null;
    private boolean on = false;
    boolean startOn = false;
    private boolean fresh = false;

    private int invalidSides = 0;
    private final ComponentAccess<IPeripheral> peripherals = PlatformHelper.get().createPeripheralAccess(this, d -> invalidSides |= 1 << d.ordinal());

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
        instanceID = null;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        unload();
    }

    protected int getInteractRange() {
        return Container.DEFAULT_DISTANCE_LIMIT;
    }

    public boolean isUsable(Player player) {
        return getFamily().checkUsable(player)
            && BaseContainerBlockEntity.canUnlock(player, lockCode, getDisplayName())
            && Container.stillValidBlockEntity(this, player, getInteractRange());
    }

    protected void serverTick() {
        if (getLevel().isClientSide) return;
        if (computerID < 0 && !startOn) return; // Don't tick if we don't need a computer!

        var computer = createServerComputer();

        // Update any peripherals that have changed.
        if (invalidSides != 0) {
            for (var direction : DirectionUtil.FACINGS) {
                if (DirectionUtil.isSet(invalidSides, direction)) refreshPeripheral(computer, direction);
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

        // If the on state has changed, mark as dirty.
        var newOn = computer.isOn();
        if (on != newOn) {
            on = newOn;
            setChanged();
        }

        // If the label has changed, mark as dirty and sync to client.
        var newLabel = computer.getLabel();
        if (!Objects.equals(label, newLabel)) {
            label = newLabel;
            BlockEntityHelpers.updateBlock(this);
        }

        // Update the block state if needed.
        updateBlockState(computer.getState());

        var changes = computer.pollAndResetChanges();
        if (changes != 0) {
            for (var direction : DirectionUtil.FACINGS) {
                if ((changes & (1 << remapToLocalSide(direction).ordinal())) != 0) updateRedstoneTo(direction);
            }
        }
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

    /**
     * Update the redstone input on a particular side.
     * <p>
     * This is called <em>immediately</em> when a neighbouring block changes (see {@link #neighborChanged(BlockPos)}).
     *
     * @param computer  The current server computer.
     * @param dir       The direction to update in.
     * @param targetPos The position of the adjacent block, equal to {@code getBlockPos().offset(dir)}.
     */
    private void updateRedstoneInput(ServerComputer computer, Direction dir, BlockPos targetPos) {
        var offsetSide = dir.getOpposite();
        var localDir = remapToLocalSide(dir);

        computer.setRedstoneInput(localDir, RedstoneUtil.getRedstoneInput(getLevel(), targetPos, dir));
        computer.setBundledRedstoneInput(localDir, BundledRedstone.getOutput(getLevel(), targetPos, offsetSide));
    }

    /**
     * Update the peripheral on a particular side.
     * <p>
     * This is called from {@link #serverTick()}, after a peripheral has been marked as invalid (such as in
     * {@link #neighborChanged(BlockPos)})
     *
     * @param computer The current server computer.
     * @param dir      The direction to update in.
     */
    private void refreshPeripheral(ServerComputer computer, Direction dir) {
        invalidSides &= ~(1 << dir.ordinal());

        var localDir = remapToLocalSide(dir);
        if (isPeripheralBlockedOnSide(localDir)) return;

        var peripheral = peripherals.get(dir);
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

    /**
     * Called when a neighbour block changes.
     * <p>
     * This finds the side the neighbour block is on, and updates the inputs accordingly.
     * <p>
     * We do <strong>NOT</strong> update the peripheral immediately. Blocks and block entities are sometimes
     * inconsistent at the point where an update is received, and so we instead just mark that side as dirty (see
     * {@link #invalidSides}) and refresh it {@linkplain #serverTick() next tick}.
     *
     * @param neighbour The position of the neighbour block.
     */
    public void neighborChanged(BlockPos neighbour) {
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
        for (var dir : DirectionUtil.FACINGS) updateRedstoneInput(computer, dir, getBlockPos().relative(dir));
        invalidSides = DirectionUtil.ALL_SIDES; // Mark all peripherals as dirty.
    }

    /**
     * Called when a neighbour block's shape changes.
     * <p>
     * Unlike {@link #neighborChanged(BlockPos)}, we don't update redstone, only peripherals.
     *
     * @param direction The side that changed.
     */
    public void neighbourShapeChanged(Direction direction) {
        invalidSides |= 1 << direction.ordinal();
    }

    /**
     * Update outputs in a specific direction.
     *
     * @param direction The direction to propagate outputs in.
     */
    protected void updateRedstoneTo(Direction direction) {
        RedstoneUtil.propagateRedstoneOutput(getLevel(), getBlockPos(), direction);

        var computer = getServerComputer();
        if (computer != null) updateRedstoneInput(computer, direction, getBlockPos().relative(direction));
    }

    /**
     * Update all redstone outputs.
     */
    public void updateRedstone() {
        for (var dir : DirectionUtil.FACINGS) updateRedstoneTo(dir);
    }

    public final int getComputerID() {
        return computerID;
    }

    public final @Nullable String getLabel() {
        return label;
    }

    public final void setComputerID(int id) {
        if (getLevel().isClientSide || computerID == id) return;

        computerID = id;
        BlockEntityHelpers.updateBlock(this);
    }

    public final void setLabel(@Nullable String label) {
        if (getLevel().isClientSide || Objects.equals(this.label, label)) return;

        this.label = label;
        var computer = getServerComputer();
        if (computer != null) computer.setLabel(label);
        BlockEntityHelpers.updateBlock(this);
    }

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
                computerID = ComputerCraftAPI.createUniqueNumberedSaveDir(server, IDAssigner.COMPUTER);
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

    protected abstract ServerComputer createComputer(int id);

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
        if (copy.computerID != computerID || !Objects.equals(copy.instanceID, instanceID)) {
            unload();
            instanceID = copy.instanceID;
            computerID = copy.computerID;
            label = copy.label;
            on = copy.on;
            startOn = copy.startOn;
            lockCode = copy.lockCode;
            BlockEntityHelpers.updateBlock(this);
        }
        copy.instanceID = null;
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
