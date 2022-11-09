/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.diskdrive;

import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.impl.MediaProviders;
import dan200.computercraft.shared.common.GenericTile;
import dan200.computercraft.shared.network.client.PlayRecordClientMessage;
import dan200.computercraft.shared.platform.PlatformHelper;
import dan200.computercraft.shared.util.DefaultInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static dan200.computercraft.core.util.Nullability.assertNonNull;

public final class DiskDriveBlockEntity extends GenericTile implements DefaultInventory, Nameable, MenuProvider {
    private static final String NBT_NAME = "CustomName";
    private static final String NBT_ITEM = "Item";

    private static class MountInfo {
        @Nullable
        String mountPath;
    }

    @Nullable
    Component customName;
    private LockCode lockCode = LockCode.NO_LOCK;

    private final Map<IComputerAccess, MountInfo> computers = new HashMap<>();

    private ItemStack diskStack = ItemStack.EMPTY;
    private @Nullable IPeripheral peripheral;
    private @Nullable IMount diskMount = null;

    private boolean recordQueued = false;
    private boolean recordPlaying = false;
    private boolean restartRecord = false;
    private boolean ejectQueued;

    public DiskDriveBlockEntity(BlockEntityType<DiskDriveBlockEntity> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void destroy() {
        ejectContents(true);
        if (recordPlaying) stopRecord();
    }

    @Override
    public boolean isUsable(Player player) {
        return super.isUsable(player) && BaseContainerBlockEntity.canUnlock(player, lockCode, getDisplayName());
    }

    @Override
    public InteractionResult onActivate(Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isCrouching()) {
            // Try to put a disk into the drive
            var disk = player.getItemInHand(hand);
            if (disk.isEmpty()) return InteractionResult.PASS;
            if (!getLevel().isClientSide && getItem(0).isEmpty() && MediaProviders.get(disk) != null) {
                setDiskStack(disk);
                player.setItemInHand(hand, ItemStack.EMPTY);
            }
            return InteractionResult.SUCCESS;
        } else {
            // Open the GUI
            if (!getLevel().isClientSide && isUsable(player)) player.openMenu(this);
            return InteractionResult.SUCCESS;
        }
    }

    public Direction getDirection() {
        return getBlockState().getValue(DiskDriveBlock.FACING);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        customName = nbt.contains(NBT_NAME) ? Component.Serializer.fromJson(nbt.getString(NBT_NAME)) : null;
        if (nbt.contains(NBT_ITEM)) {
            var item = nbt.getCompound(NBT_ITEM);
            diskStack = ItemStack.of(item);
            diskMount = null;
        }

        lockCode = LockCode.fromTag(nbt);
    }

    @Override
    public void saveAdditional(CompoundTag nbt) {
        if (customName != null) nbt.putString(NBT_NAME, Component.Serializer.toJson(customName));

        if (!diskStack.isEmpty()) {
            var item = new CompoundTag();
            diskStack.save(item);
            nbt.put(NBT_ITEM, item);
        }

        lockCode.addToTag(nbt);

        super.saveAdditional(nbt);
    }

    void serverTick() {
        // Ejection
        if (ejectQueued) {
            ejectContents(false);
            ejectQueued = false;
        }

        // Music
        synchronized (this) {
            if (recordPlaying != recordQueued || restartRecord) {
                restartRecord = false;
                if (recordQueued) {
                    var contents = getDiskMedia();
                    var record = contents != null ? contents.getAudio(diskStack) : null;
                    if (record != null) {
                        recordPlaying = true;
                        playRecord();
                    } else {
                        recordQueued = false;
                    }
                } else {
                    stopRecord();
                    recordPlaying = false;
                }
            }
        }
    }

    // IInventory implementation

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return diskStack.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return diskStack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        var result = diskStack;
        diskStack = ItemStack.EMPTY;
        diskMount = null;

        return result;
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        if (diskStack.isEmpty()) return ItemStack.EMPTY;

        if (diskStack.getCount() <= count) {
            var disk = diskStack;
            setItem(slot, ItemStack.EMPTY);
            return disk;
        }

        var part = diskStack.split(count);
        setItem(slot, diskStack.isEmpty() ? ItemStack.EMPTY : diskStack);
        return part;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (getLevel().isClientSide) {
            diskStack = stack;
            diskMount = null;
            setChanged();
            return;
        }

        synchronized (this) {
            if (ItemStack.isSameItemSameTags(stack, diskStack)) {
                diskStack = stack;
                return;
            }

            // Unmount old disk
            if (!diskStack.isEmpty()) {
                // TODO: Is this iteration thread safe?
                var computers = this.computers.keySet();
                for (var computer : computers) unmountDisk(computer);
            }

            // Stop music
            if (recordPlaying) {
                stopRecord();
                recordPlaying = false;
                recordQueued = false;
            }

            // Swap disk over
            diskStack = stack;
            diskMount = null;
            setChanged();

            // Mount new disk
            if (!diskStack.isEmpty()) {
                var computers = this.computers.keySet();
                for (var computer : computers) mountDisk(computer);
            }
        }
    }

    @Override
    public void setChanged() {
        if (!level.isClientSide) updateBlockState();
        super.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return isUsable(player);
    }

    @Override
    public void clearContent() {
        setItem(0, ItemStack.EMPTY);
    }

    ItemStack getDiskStack() {
        return getItem(0);
    }

    void setDiskStack(ItemStack stack) {
        setItem(0, stack);
    }

    private @Nullable IMedia getDiskMedia() {
        return MediaProviders.get(getDiskStack());
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
            computers.put(computer, new MountInfo());
            mountDisk(computer);
        }
    }

    void unmount(IComputerAccess computer) {
        synchronized (this) {
            unmountDisk(computer);
            computers.remove(computer);
        }
    }

    void playDiskAudio() {
        synchronized (this) {
            var media = getDiskMedia();
            if (media != null && media.getAudioTitle(diskStack) != null) {
                recordQueued = true;
                restartRecord = recordPlaying;
            }
        }
    }

    void stopDiskAudio() {
        synchronized (this) {
            recordQueued = false;
            restartRecord = false;
        }
    }

    void ejectDisk() {
        synchronized (this) {
            ejectQueued = true;
        }
    }

    // private methods

    private synchronized void mountDisk(IComputerAccess computer) {
        if (!diskStack.isEmpty()) {
            var info = assertNonNull(computers.get(computer));
            var contents = getDiskMedia();
            if (contents != null) {
                if (diskMount == null) {
                    diskMount = contents.createDataMount(diskStack, getLevel());
                }
                if (diskMount != null) {
                    if (diskMount instanceof IWritableMount) {
                        // Try mounting at the lowest numbered "disk" name we can
                        var n = 1;
                        while (info.mountPath == null) {
                            info.mountPath = computer.mountWritable(n == 1 ? "disk" : "disk" + n, (IWritableMount) diskMount);
                            n++;
                        }
                    } else {
                        // Try mounting at the lowest numbered "disk" name we can
                        var n = 1;
                        while (info.mountPath == null) {
                            info.mountPath = computer.mount(n == 1 ? "disk" : "disk" + n, diskMount);
                            n++;
                        }
                    }
                } else {
                    info.mountPath = null;
                }
            }
            computer.queueEvent("disk", computer.getAttachmentName());
        }
    }

    private synchronized void unmountDisk(IComputerAccess computer) {
        if (!diskStack.isEmpty()) {
            var info = Objects.requireNonNull(computers.get(computer), "No mount info");
            if (info.mountPath != null) {
                computer.unmount(info.mountPath);
                info.mountPath = null;
            }
            computer.queueEvent("disk_eject", computer.getAttachmentName());
        }
    }

    private void updateBlockState() {
        if (remove || level == null) return;

        if (!diskStack.isEmpty()) {
            var contents = getDiskMedia();
            updateBlockState(contents != null ? DiskDriveState.FULL : DiskDriveState.INVALID);
        } else {
            updateBlockState(DiskDriveState.EMPTY);
        }
    }

    private void updateBlockState(DiskDriveState state) {
        var blockState = getBlockState();
        if (blockState.getValue(DiskDriveBlock.STATE) == state) return;

        getLevel().setBlockAndUpdate(getBlockPos(), blockState.setValue(DiskDriveBlock.STATE, state));
    }

    private synchronized void ejectContents(boolean destroyed) {
        if (getLevel().isClientSide || diskStack.isEmpty()) return;

        // Remove the disks from the inventory
        var disks = diskStack;
        setDiskStack(ItemStack.EMPTY);

        // Spawn the item in the world
        var xOff = 0;
        var zOff = 0;
        if (!destroyed) {
            var dir = getDirection();
            xOff = dir.getStepX();
            zOff = dir.getStepZ();
        }

        var pos = getBlockPos();
        var x = pos.getX() + 0.5 + xOff * 0.5;
        var y = pos.getY() + 0.75;
        var z = pos.getZ() + 0.5 + zOff * 0.5;
        var entityitem = new ItemEntity(getLevel(), x, y, z, disks);
        entityitem.setDeltaMovement(xOff * 0.15, 0, zOff * 0.15);

        getLevel().addFreshEntity(entityitem);
        if (!destroyed) getLevel().globalLevelEvent(1000, getBlockPos(), 0);
    }

    // Private methods

    private void playRecord() {
        var contents = getDiskMedia();
        var record = contents != null ? contents.getAudio(diskStack) : null;
        if (record != null) {
            playRecord(new PlayRecordClientMessage(getBlockPos(), record, assertNonNull(contents).getAudioTitle(diskStack)));
        } else {
            stopRecord();
        }
    }

    private void stopRecord() {
        playRecord(new PlayRecordClientMessage(getBlockPos()));
    }

    private void playRecord(PlayRecordClientMessage message) {
        PlatformHelper.get().sendToAllAround(message, (ServerLevel) getLevel(), Vec3.atCenterOf(getBlockPos()), 64);
    }

    @Override
    public boolean hasCustomName() {
        return customName != null;
    }

    @Nullable
    @Override
    public Component getCustomName() {
        return customName;
    }

    @Override
    public Component getName() {
        return customName != null ? customName : Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public Component getDisplayName() {
        return Nameable.super.getDisplayName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new DiskDriveMenu(id, inventory, this);
    }

    public IPeripheral peripheral() {
        if (peripheral != null) return peripheral;
        return peripheral = new DiskDrivePeripheral(this);
    }
}
