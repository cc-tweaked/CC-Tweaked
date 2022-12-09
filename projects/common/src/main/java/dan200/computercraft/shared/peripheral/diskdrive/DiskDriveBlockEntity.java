/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.diskdrive;

import com.google.errorprone.annotations.concurrent.GuardedBy;
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

    private static class MountInfo {
        @Nullable
        String mountPath;
    }

    private final DiskDrivePeripheral peripheral = new DiskDrivePeripheral(this);

    private final @GuardedBy("this") Map<IComputerAccess, MountInfo> computers = new HashMap<>();

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(1, ItemStack.EMPTY);

    private MediaStack media = MediaStack.EMPTY;
    private boolean recordPlaying = false;
    // In order to avoid main-thread calls in the peripheral, we set flags to mark which operation should be performed,
    // then read them when ticking.
    private final AtomicReference<RecordCommand> recordQueued = new AtomicReference<>(null);
    private final AtomicBoolean ejectQueued = new AtomicBoolean(false);

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
        if (ItemStack.isSame(newDisk, media.stack)) return;

        var media = new MediaStack(newDisk.copy());

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

            this.media = media;

            // Mount new disk
            if (!this.media.stack.isEmpty()) {
                for (var computer : computers.entrySet()) mountDisk(computer.getKey(), computer.getValue(), this.media);
            }
        }
    }

    ItemStack getDiskStack() {
        return getItem(0);
    }

    MediaStack getMedia() {
        return media;
    }

    void setDiskStack(ItemStack stack) {
        setItem(0, stack);
        setChanged();
    }

    @Nullable
    String getDiskMountPath(IComputerAccess computer) {
        synchronized (this) {
            var info = computers.get(computer);
            return info != null ? info.mountPath : null;
        }
    }

    void mount(IComputerAccess computer) {
        synchronized (this) {
            var info = new MountInfo();
            computers.put(computer, info);
            mountDisk(computer, info, media);
        }
    }

    void unmount(IComputerAccess computer) {
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

    @GuardedBy("this")
    private void mountDisk(IComputerAccess computer, MountInfo info, MediaStack disk) {
        var mount = disk.getMount((ServerLevel) getLevel());
        if (mount != null) {
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
        } else {
            info.mountPath = null;
        }

        computer.queueEvent("disk", computer.getAttachmentName());
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

        WorldUtil.dropItemStack(stack, getLevel(), getBlockPos(), getDirection());
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
