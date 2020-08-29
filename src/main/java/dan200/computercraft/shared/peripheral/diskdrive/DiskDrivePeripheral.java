/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.diskdrive;

import static dan200.computercraft.core.apis.ArgumentHelper.optString;

import javax.annotation.Nonnull;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.media.items.ItemDisk;
import dan200.computercraft.shared.util.StringUtil;

import net.minecraft.item.ItemStack;

public class DiskDrivePeripheral implements IPeripheral {
    private final TileDiskDrive m_diskDrive;

    public DiskDrivePeripheral(TileDiskDrive diskDrive) {
        this.m_diskDrive = diskDrive;
    }

    @Nonnull
    @Override
    public String getType() {
        return "drive";
    }

    @Nonnull
    @Override
    public String[] getMethodNames() {
        return new String[] {
            "isDiskPresent",
            "getDiskLabel",
            "setDiskLabel",
            "hasData",
            "getMountPath",
            "hasAudio",
            "getAudioTitle",
            "playAudio",
            "stopAudio",
            "ejectDisk",
            "getDiskID"
        };
    }

    @Override
    public Object[] callMethod(@Nonnull IComputerAccess computer, @Nonnull ILuaContext context, int method, @Nonnull Object[] arguments) throws LuaException {
        switch (method) {
        case 0: // isDiskPresent
            return new Object[] {
                !this.m_diskDrive.getDiskStack()
                                 .isEmpty()
            };
        case 1: // getDiskLabel
        {
            IMedia media = this.m_diskDrive.getDiskMedia();
            return media == null ? null : new Object[] {media.getLabel(this.m_diskDrive.getDiskStack())};
        }
        case 2: // setDiskLabel
        {
            String label = optString(arguments, 0, null);

            IMedia media = this.m_diskDrive.getDiskMedia();
            if (media == null) {
                return null;
            }

            ItemStack disk = this.m_diskDrive.getDiskStack();
            label = StringUtil.normaliseLabel(label);
            if (!media.setLabel(disk, label)) {
                throw new LuaException("Disk label cannot be changed");
            }
            this.m_diskDrive.setDiskStack(disk);
            return null;
        }
        case 3: // hasData
            return new Object[] {this.m_diskDrive.getDiskMountPath(computer) != null};
        case 4: // getMountPath
            return new Object[] {this.m_diskDrive.getDiskMountPath(computer)};
        case 5: {
            // hasAudio
            IMedia media = this.m_diskDrive.getDiskMedia();
            return new Object[] {media != null && media.getAudio(this.m_diskDrive.getDiskStack()) != null};
        }
        case 6: {
            // getAudioTitle
            IMedia media = this.m_diskDrive.getDiskMedia();
            return new Object[] {media != null ? media.getAudioTitle(this.m_diskDrive.getDiskStack()) : false};
        }
        case 7: // playAudio
            this.m_diskDrive.playDiskAudio();
            return null;
        case 8: // stopAudio
            this.m_diskDrive.stopDiskAudio();
            return null;
        case 9: // eject
            this.m_diskDrive.ejectDisk();
            return null;
        case 10: // getDiskID
        {
            ItemStack disk = this.m_diskDrive.getDiskStack();
            return disk.getItem() instanceof ItemDisk ? new Object[] {ItemDisk.getDiskID(disk)} : null;
        }
        default:
            return null;
        }
    }

    @Override
    public void attach(@Nonnull IComputerAccess computer) {
        this.m_diskDrive.mount(computer);
    }

    @Override
    public void detach(@Nonnull IComputerAccess computer) {
        this.m_diskDrive.unmount(computer);
    }

    @Nonnull
    @Override
    public Object getTarget() {
        return this.m_diskDrive;
    }

    @Override
    public boolean equals(IPeripheral other) {
        if (this == other) {
            return true;
        }
        return other instanceof DiskDrivePeripheral && ((DiskDrivePeripheral) other).m_diskDrive == this.m_diskDrive;
    }
}
