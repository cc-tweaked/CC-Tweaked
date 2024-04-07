// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.peripheral.diskdrive;

import com.google.errorprone.annotations.concurrent.GuardedBy;
import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.api.filesystem.WritableMount;
import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.common.AbstractContainerBlockEntity;
import dan200.computercraft.shared.network.client.PlayRecordClientMessage;
import dan200.computercraft.shared.network.server.ServerNetworking;
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

/**
 * The underlying block entity for disk drives. This holds the main logic for the {@linkplain DiskDrivePeripheral disk
 * drive peripheral}, such as handling mounts and {@linkplain DiskDrivePeripheral#playAudio() playing audio}.
 * <p>
 * Most disk drive peripheral methods execute on the computer thread (largely due to historic reasons). This causes some
 * problems, as the disk item could be read by both the computer thread (via peripheral calls) and main thread (via
 * Minecraft inventory interaction).
 * <p>
 * To solve this, we use an immutable {@link MediaStack}, which holds an immutable version of the current
 * {@link ItemStack} (and its corresponding {@link IMedia}). When the {@linkplain #setChanged() inventory is changed},
 * we {@linkplain #updateMedia() update the media stack} and recompute mounts.
 * <p>
 * This is somewhat complicated by {@link #attach(IComputerAccess)}. As that can happen on the computer thread and
 * may mutate the stack (when {@link IMedia#createDataMount(ItemStack, ServerLevel)} assigns an ID for the first time),
 * we need a way to safely update the inventory. To solve this, all internal non-inventory interactions with disk drives
 * treat the media stack as the "primary" stack. This allows us to atomically update it, and then sync it back to the
 * main inventory ({@link #updateMediaStack(ItemStack, boolean)}) either directly ({@link #updateDiskFromMedia()}) or
 * on the next block tick ({@link #stackDirty}). This does mean there's a one-tick delay where the inventory may be
 * out-of-date, but that should happen very rarely.
 *
 * @see DiskDrivePeripheral
 */
public final class DiskDriveBlockEntity extends AbstractContainerBlockEntity {
    private static final String NBT_ITEM = "Item";

    private static final class MountInfo {
        @Nullable
        String mountPath;
    }

    private final DiskDrivePeripheral peripheral = new DiskDrivePeripheral(this);

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(1, ItemStack.EMPTY);

    @GuardedBy("this")
    private final Map<IComputerAccess, MountInfo> computers = new HashMap<>();
    @GuardedBy("this")
    private MediaStack media = MediaStack.EMPTY;
    @GuardedBy("this")
    private @Nullable Mount mount;

    private boolean recordPlaying = false;
    // In order to avoid main-thread calls in the peripheral, we set flags to mark which operation should be performed,
    // then read them when ticking.
    private final AtomicReference<RecordCommand> recordQueued = new AtomicReference<>(null);
    private final AtomicBoolean ejectQueued = new AtomicBoolean(false);

    /**
     * Whether the stack in {@link #media} has been modified on the computer thread, and needs to be written back to the
     * inventory on the main thread.
     */
    private final AtomicBoolean stackDirty = new AtomicBoolean(false);

    public DiskDriveBlockEntity(BlockEntityType<DiskDriveBlockEntity> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public IPeripheral peripheral() {
        return peripheral;
    }

    @Override
    public void clearRemoved() {
        updateMedia();
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
        if (stackDirty.getAndSet(false)) updateDiskFromMedia();
        if (ejectQueued.getAndSet(false)) ejectContents();

        var recordQueued = this.recordQueued.getAndSet(null);
        if (recordQueued != null) {
            switch (recordQueued) {
                case PLAY -> {
                    var media = getMedia();
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
        if (level != null && !level.isClientSide) updateMedia();
        super.setChanged();
    }

    /**
     * Called on the server after the item has changed. This unmounts the old media and mounts the new one.
     */
    private synchronized void updateMedia() {
        var newStack = getDiskStack();
        if (ItemStack.isSameItemSameTags(newStack, media.stack())) return;

        var newMedia = MediaStack.of(newStack);

        if (newStack.isEmpty()) {
            updateBlockState(DiskDriveState.EMPTY);
        } else {
            updateBlockState(newMedia.media() != null ? DiskDriveState.FULL : DiskDriveState.INVALID);
        }

        // Unmount old disk
        if (!media.stack().isEmpty()) {
            for (var computer : computers.entrySet()) unmountDisk(computer.getKey(), computer.getValue());
        }

        // Stop music
        if (recordPlaying) {
            stopRecord();
            recordPlaying = false;
        }

        // Use our new media, and (if needed) mount the new disk.
        mount = null;
        media = newMedia;
        stackDirty.set(false);

        if (!newStack.isEmpty() && !computers.isEmpty()) {
            var mount = getOrCreateMount(true);
            for (var entry : computers.entrySet()) {
                mountDisk(entry.getKey(), entry.getValue(), mount);
            }
        }
    }

    ItemStack getDiskStack() {
        return getItem(0);
    }

    synchronized MediaStack getMedia() {
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
     * Update the inventory's disk stack from the media stack. Unlike {@link #setDiskStack(ItemStack)} this will not
     * change any mounts.
     */
    private synchronized void updateDiskFromMedia() {
        // Write back the item to the main inventory, and then mark it as dirty.
        setItem(0, media.stack().copy());
        super.setChanged();
    }

    /**
     * Atomically update {@link #media}'s stack, then sync it back to the main inventory.
     *
     * @param stack     The original stack.
     * @param immediate Whether to do this immediately (when called from the main thread) or asynchronously (when called
     *                  from the computer thread).
     */
    @GuardedBy("this")
    private void updateMediaStack(ItemStack stack, boolean immediate) {
        if (ItemStack.isSameItemSameTags(media.stack(), stack)) return;
        media = new MediaStack(stack, media.media());

        if (immediate) {
            updateDiskFromMedia();
        } else {
            stackDirty.set(true);
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
            if (!media.stack().isEmpty()) {
                mountDisk(computer, info, getOrCreateMount(level instanceof ServerLevel l && l.getServer().isSameThread()));
            }
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

    synchronized MountResult setDiskLabel(@Nullable String label) {
        if (media.media() == null) return MountResult.NO_MEDIA;

        // Set the label, and write it back to the media stack.
        var stack = media.stack().copy();
        if (!media.media().setLabel(stack, label)) return MountResult.NOT_ALLOWED;
        updateMediaStack(stack, true);

        return MountResult.CHANGED;
    }

    @GuardedBy("this")
    private @Nullable Mount getOrCreateMount(boolean immediate) {
        if (media.media() == null) return null;
        if (mount != null) return mount;

        // Set the id (if needed) and write it back to the media stack.
        var stack = media.stack().copy();
        mount = media.media().createDataMount(stack, (ServerLevel) getLevel());
        updateMediaStack(stack, immediate);

        return mount;
    }

    private static void mountDisk(IComputerAccess computer, MountInfo info, @Nullable Mount mount) {
        if (mount instanceof WritableMount writable) {
            // Try mounting at the lowest numbered "disk" name we can
            var n = 1;
            while (info.mountPath == null) {
                info.mountPath = computer.mountWritable(n == 1 ? "disk" : "disk" + n, writable);
                n++;
            }
        } else if (mount != null) {
            // Try mounting at the lowest numbered "disk" name we can
            var n = 1;
            while (info.mountPath == null) {
                info.mountPath = computer.mount(n == 1 ? "disk" : "disk" + n, mount);
                n++;
            }
        } else {
            assert info.mountPath == null : "Mount path should be null";
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

        WorldUtil.dropItemStack(getLevel(), getBlockPos(), getDirection(), stack);
        getLevel().levelEvent(LevelEvent.SOUND_DISPENSER_DISPENSE, getBlockPos(), 0);
    }

    private void stopRecord() {
        sendMessage(new PlayRecordClientMessage(getBlockPos()));
    }

    private void sendMessage(PlayRecordClientMessage message) {
        ServerNetworking.sendToAllAround(message, (ServerLevel) getLevel(), Vec3.atCenterOf(getBlockPos()), 64);
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new DiskDriveMenu(id, inventory, this);
    }

    private enum RecordCommand {
        PLAY,
        STOP,
    }

    enum MountResult {
        NO_MEDIA,
        NOT_ALLOWED,
        CHANGED,
    }
}
