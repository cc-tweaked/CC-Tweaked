// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.peripheral.diskdrive;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.util.StringUtil;
import dan200.computercraft.shared.media.items.DiskItem;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Disk drives are a peripheral which allow you to read and write to floppy disks and other "mountable media" (such as
 * computers or turtles). They also allow you to {@link #playAudio play records}.
 * <p>
 * When a disk drive attaches some mount (such as a floppy disk or computer), it attaches a folder called {@code disk},
 * {@code disk2}, etc... to the root directory of the computer. This folder can be used to interact with the files on
 * that disk.
 * <p>
 * When a disk is inserted, a {@code disk} event is fired, with the side peripheral is on. Likewise, when the disk is
 * detached, a {@code disk_eject} event is fired.
 * <p>
 * ## Recipe
 * <div class="recipe-container">
 *     <mc-recipe recipe="computercraft:disk_drive"></mc-recipe>
 * </div>
 *
 * @cc.module drive
 */
public class DiskDrivePeripheral implements IPeripheral {
    private final DiskDriveBlockEntity diskDrive;

    DiskDrivePeripheral(DiskDriveBlockEntity diskDrive) {
        this.diskDrive = diskDrive;
    }

    @Override
    public String getType() {
        return "drive";
    }

    /**
     * Returns whether a disk is currently inserted in the drive.
     *
     * @return Whether a disk is currently inserted in the drive.
     */
    @LuaFunction
    public final boolean isDiskPresent() {
        return !diskDrive.getMedia().stack().isEmpty();
    }

    /**
     * Returns the label of the disk in the drive if available.
     *
     * @return The label of the disk, or {@code nil} if either no disk is inserted or the disk doesn't have a label.
     * @cc.treturn string|nil The label of the disk, or {@code nil} if either no disk is inserted or the disk doesn't have a label.
     */
    @Nullable
    @LuaFunction
    public final Object[] getDiskLabel() {
        var media = diskDrive.getMedia();
        return media.media() == null ? null : new Object[]{ media.media().getLabel(media.stack()) };
    }

    /**
     * Sets or clears the label for a disk.
     * <p>
     * If no label or {@code nil} is passed, the label will be cleared.
     * <p>
     * If the inserted disk's label can't be changed (for example, a record),
     * an error will be thrown.
     *
     * @param label The new label of the disk, or {@code nil} to clear.
     * @throws LuaException If the disk's label can't be changed.
     */
    @LuaFunction(mainThread = true)
    public final void setDiskLabel(Optional<String> label) throws LuaException {
        switch (diskDrive.setDiskLabel(label.map(StringUtil::normaliseLabel).orElse(null))) {
            case NOT_ALLOWED -> throw new LuaException("Disk label cannot be changed");
            case CHANGED, NO_MEDIA -> {
            }
        }
    }

    /**
     * Returns whether a disk with data is inserted.
     *
     * @param computer The computer object
     * @return Whether a disk with data is inserted.
     */
    @LuaFunction
    public final boolean hasData(IComputerAccess computer) {
        return diskDrive.getDiskMountPath(computer) != null;
    }

    /**
     * Returns the mount path for the inserted disk.
     *
     * @param computer The computer object
     * @return The mount path for the disk, or {@code nil} if no data disk is inserted.
     */
    @LuaFunction
    @Nullable
    public final String getMountPath(IComputerAccess computer) {
        return diskDrive.getDiskMountPath(computer);
    }

    /**
     * Returns whether a disk with audio is inserted.
     *
     * @return Whether a disk with audio is inserted.
     */
    @LuaFunction
    public final boolean hasAudio() {
        return diskDrive.getMedia().getAudio() != null;
    }

    /**
     * Returns the title of the inserted audio disk.
     *
     * @return The title of the audio, or {@code false} if no audio disk is inserted.
     * @cc.treturn string|nil|false The title of the audio, {@code false} if no disk is inserted, or {@code nil} if the disk has no audio.
     */
    @LuaFunction
    @Nullable
    public final Object getAudioTitle() {
        var stack = diskDrive.getMedia();
        return stack.media() != null ? stack.getAudioTitle() : false;
    }

    /**
     * Plays the audio in the inserted disk, if available.
     */
    @LuaFunction
    public final void playAudio() {
        diskDrive.playDiskAudio();
    }

    /**
     * Stops any audio that may be playing.
     *
     * @see #playAudio
     */
    @LuaFunction
    public final void stopAudio() {
        diskDrive.stopDiskAudio();
    }

    /**
     * Ejects any disk that may be in the drive.
     */
    @LuaFunction
    public final void ejectDisk() {
        diskDrive.ejectDisk();
    }

    /**
     * Returns the ID of the disk inserted in the drive.
     *
     * @return The ID of the disk in the drive, or {@code nil} if no disk with an ID is inserted.
     * @cc.treturn number|nil The ID of the disk in the drive, or {@code nil} if no disk with an ID is inserted.
     * @cc.since 1.4
     */
    @Nullable
    @LuaFunction
    public final Object[] getDiskID() {
        var disk = diskDrive.getMedia().stack();
        return disk.getItem() instanceof DiskItem ? new Object[]{ DiskItem.getDiskID(disk) } : null;
    }

    @Override
    public void attach(IComputerAccess computer) {
        diskDrive.attach(computer);
    }

    @Override
    public void detach(IComputerAccess computer) {
        diskDrive.detach(computer);
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return this == other || other instanceof DiskDrivePeripheral drive && drive.diskDrive == diskDrive;
    }

    @Override
    public Object getTarget() {
        return diskDrive;
    }
}
