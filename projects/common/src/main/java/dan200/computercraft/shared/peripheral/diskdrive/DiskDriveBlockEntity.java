// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.peripheral.diskdrive;

import com.google.errorprone.annotations.concurrent.GuardedBy;
import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.api.filesystem.WritableMount;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.common.AbstractContainerBlockEntity;
import dan200.computercraft.shared.network.client.PlayRecordClientMessage;
import dan200.computercraft.shared.platform.PlatformHelper;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class DiskDriveBlockEntity extends AbstractContainerBlockEntity {
    private static final String NBT_ITEM = "Item";

    private static final class MountInfo {
        @Nullable
        String mountPath;
    }

    private final DiskDrivePeripheral peripheral = new DiskDrivePeripheral(this);

    private final @GuardedBy("this") Map<IComputerAccess, MountInfo> computers = new HashMap<>();

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(1, ItemStack.EMPTY);

    private MediaStack media = MediaStack.EMPTY;
    private @Nullable Mount mount;

    private boolean recordPlaying = false;
    // In order to avoid main-thread calls in the peripheral, we set flags to mark which operation should be performed,
    // then read them when ticking.
    private final AtomicReference<RecordCommand> recordQueued = new AtomicReference<>(null);
    private final AtomicBoolean ejectQueued = new AtomicBoolean(false);
    private final AtomicBoolean mountQueued = new AtomicBoolean(false);

    public DiskDriveBlockEntity(BlockEntityType<DiskDriveBlockEntity> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public IPeripheral peripheral() {
        return peripheral;
    }

    @Override
    public void clearRemoved() {
        updateItem();
    }

    @Override
    public void setRemoved() {
        if (recordPlaying) stopRecord();
    }

    public Direction getDirection() {
        return getBlockState().getValue(DiskDriveBlock.FACING);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        setDiskStack(nbt.contains(NBT_ITEM) ? ItemStack.of(nbt.getCompound(NBT_ITEM)) : ItemStack.EMPTY);
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        var stack = getDiskStack();
        if (!stack.isEmpty()) tag.put(NBT_ITEM, stack.save(new CompoundTag()));
    }

    void serverTick() {
        if (ejectQueued.getAndSet(false)) ejectContents();

        var recordQueued = this.recordQueued.getAndSet(null);
        if (recordQueued != null) {
            switch (recordQueued) {
                case PLAY -> {
                    var record = media.getAudio();
                    if (record != null) {
                        recordPlaying = true;
                        var title = media.getAudioTitle();
                        sendMessage(new PlayRecordClientMessage(getBlockPos(), record, title));
                    }
                }
                case STOP -> {
                    stopRecord();
                    recordPlaying = false;
                }
            }
        }

        if (mountQueued.get()) {
            synchronized (this) {
                mountAll();
            }
        }
    }

    @Override
    public NonNullList<ItemStack> getContents() {
        return inventory;
    }

    @Override
    public void setChanged() {
        if (level != null && !level.isClientSide) updateItem();
        super.setChanged();
    }

    private void updateItem() {
        var newDisk = getDiskStack();
        if (ItemStack.isSameItemSameTags(newDisk, media.stack)) return;

        var media = MediaStack.of(newDisk);

        if (newDisk.isEmpty()) {
            updateBlockState(DiskDriveState.EMPTY);
        } else {
            updateBlockState(media.media != null ? DiskDriveState.FULL : DiskDriveState.INVALID);
        }

        synchronized (this) {
            // Unmount old disk
            if (!this.media.stack.isEmpty()) {
                for (var computer : computers.entrySet()) unmountDisk(computer.getKey(), computer.getValue());
            }

            // Stop music
            if (recordPlaying) {
                stopRecord();
                recordPlaying = false;
            }

            mount = null;
            this.media = media;

            mountAll();
        }
    }

    ItemStack getDiskStack() {
        return getItem(0);
    }

    MediaStack getMedia() {
        return media;
    }

    /**
     * Set the current disk stack, mounting/unmounting if needed.
     *
     * @param stack The new disk stack.
     */
    void setDiskStack(ItemStack stack) {
        setItem(0, stack);
        setChanged();
    }

    /**
     * Update the current disk stack, assuming the underlying item does not change. Unlike
     * {@link #setDiskStack(ItemStack)} this will not change any mounts.
     *
     * @param stack The new disk stack.
     */
    void updateDiskStack(ItemStack stack) {
        setItem(0, stack);
        if (!ItemStack.isSameItemSameTags(stack, media.stack)) {
            media = MediaStack.of(stack);
            super.setChanged();
        }
    }

    @Nullable
    String getDiskMountPath(IComputerAccess computer) {
        synchronized (this) {
            var info = computers.get(computer);
            return info != null ? info.mountPath : null;
        }
    }

    /**
     * Attach a computer to this disk drive. This sets up the {@link MountInfo} map and flags us to mount next tick. We
     * don't mount here, as that might require mutating the current stack.
     *
     * @param computer The computer to attach.
     */
    void attach(IComputerAccess computer) {
        synchronized (this) {
            var info = new MountInfo();
            computers.put(computer, info);
            mountQueued.set(true);
        }
    }

    void detach(IComputerAccess computer) {
        synchronized (this) {
            unmountDisk(computer, computers.remove(computer));
        }
    }

    void playDiskAudio() {
        recordQueued.set(RecordCommand.PLAY);
    }

    void stopDiskAudio() {
        recordQueued.set(RecordCommand.STOP);
    }

    void ejectDisk() {
        ejectQueued.set(true);
    }

    /**
     * Add our mount to all computers.
     */
    @GuardedBy("this")
    private void mountAll() {
        doMountAll();
        mountQueued.set(false);
    }

    /**
     * The worker for {@link #mountAll()}. This is responsible for creating the mount and placing it on all computers.
     */
    @GuardedBy("this")
    private void doMountAll() {
        if (computers.isEmpty() || media.media == null) return;

        if (mount == null) {
            var stack = getDiskStack();
            mount = media.media.createDataMount(stack, (ServerLevel) level);
            setDiskStack(stack);
        }

        if (mount == null) return;

        for (var entry : computers.entrySet()) {
            var computer = entry.getKey();
            var info = entry.getValue();
            if (info.mountPath != null) continue;

            if (mount instanceof WritableMount writable) {
                // Try mounting at the lowest numbered "disk" name we can
                var n = 1;
                while (info.mountPath == null) {
                    info.mountPath = computer.mountWritable(n == 1 ? "disk" : "disk" + n, writable);
                    n++;
                }
            } else {
                // Try mounting at the lowest numbered "disk" name we can
                var n = 1;
                while (info.mountPath == null) {
                    info.mountPath = computer.mount(n == 1 ? "disk" : "disk" + n, mount);
                    n++;
                }
            }

            computer.queueEvent("disk", computer.getAttachmentName());
        }
    }

    private static void unmountDisk(IComputerAccess computer, MountInfo info) {
        if (info.mountPath != null) {
            computer.unmount(info.mountPath);
            info.mountPath = null;
        }

        computer.queueEvent("disk_eject", computer.getAttachmentName());
    }

    private void updateBlockState(DiskDriveState state) {
        var blockState = getBlockState();
        if (blockState.getValue(DiskDriveBlock.STATE) == state) return;

        getLevel().setBlockAndUpdate(getBlockPos(), blockState.setValue(DiskDriveBlock.STATE, state));
    }

    private void ejectContents() {
        if (getLevel().isClientSide) return;

        var stack = getDiskStack();
        if (stack.isEmpty()) return;
        setDiskStack(ItemStack.EMPTY);

        WorldUtil.dropItemStack(getLevel(), getBlockPos(), getDirection(), stack);
        getLevel().levelEvent(LevelEvent.SOUND_DISPENSER_DISPENSE, getBlockPos(), 0);
    }

    private void stopRecord() {
        sendMessage(new PlayRecordClientMessage(getBlockPos()));
    }

    private void sendMessage(PlayRecordClientMessage message) {
        PlatformHelper.get().sendToAllAround(message, (ServerLevel) getLevel(), Vec3.atCenterOf(getBlockPos()), 64);
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new DiskDriveMenu(id, inventory, this);
    }

    private enum RecordCommand {
        PLAY,
        STOP,
    }
}
