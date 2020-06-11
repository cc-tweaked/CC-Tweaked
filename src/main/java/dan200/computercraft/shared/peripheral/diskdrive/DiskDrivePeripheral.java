/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.diskdrive;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.MediaProviders;
import dan200.computercraft.shared.media.items.ItemDisk;
import dan200.computercraft.shared.util.StringUtil;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.Optional;

public class DiskDrivePeripheral implements IPeripheral
{
    private final TileDiskDrive diskDrive;

    DiskDrivePeripheral( TileDiskDrive diskDrive )
    {
        this.diskDrive = diskDrive;
    }

    @Nonnull
    @Override
    public String getType()
    {
        return "drive";
    }

    @LuaFunction
    public final boolean isDiskPresent()
    {
        return !diskDrive.getDiskStack().isEmpty();
    }

    @LuaFunction
    public final Object[] getDiskLabel()
    {
        ItemStack stack = diskDrive.getDiskStack();
        IMedia media = MediaProviders.get( stack );
        return media == null ? null : new Object[] { media.getLabel( stack ) };
    }

    @LuaFunction( mainThread = true )
    public final void setDiskLabel( Optional<String> labelA ) throws LuaException
    {
        String label = labelA.orElse( null );
        ItemStack stack = diskDrive.getDiskStack();
        IMedia media = MediaProviders.get( stack );
        if( media == null ) return;

        if( !media.setLabel( stack, StringUtil.normaliseLabel( label ) ) )
        {
            throw new LuaException( "Disk label cannot be changed" );
        }
        diskDrive.setDiskStack( stack );
    }

    @LuaFunction
    public final boolean hasData( IComputerAccess computer )
    {
        return diskDrive.getDiskMountPath( computer ) != null;
    }

    @LuaFunction
    public final String getMountPath( IComputerAccess computer )
    {
        return diskDrive.getDiskMountPath( computer );
    }

    @LuaFunction
    public final boolean hasAudio()
    {
        ItemStack stack = diskDrive.getDiskStack();
        IMedia media = MediaProviders.get( stack );
        return media != null && media.getAudio( stack ) != null;
    }

    @LuaFunction
    public final Object getAudioTitle()
    {
        ItemStack stack = diskDrive.getDiskStack();
        IMedia media = MediaProviders.get( stack );
        return media != null ? media.getAudioTitle( stack ) : false;
    }

    @LuaFunction
    public final void playAudio()
    {
        diskDrive.playDiskAudio();
    }

    @LuaFunction
    public final void stopAudio()
    {
        diskDrive.stopDiskAudio();
    }

    @LuaFunction
    public final void ejectDisk()
    {
        diskDrive.ejectDisk();
    }

    @LuaFunction
    public final Object[] getDiskID()
    {
        ItemStack disk = diskDrive.getDiskStack();
        return disk.getItem() instanceof ItemDisk ? new Object[] { ItemDisk.getDiskID( disk ) } : null;
    }

    @Override
    public void attach( @Nonnull IComputerAccess computer )
    {
        diskDrive.mount( computer );
    }

    @Override
    public void detach( @Nonnull IComputerAccess computer )
    {
        diskDrive.unmount( computer );
    }

    @Override
    public boolean equals( IPeripheral other )
    {
        return this == other || other instanceof DiskDrivePeripheral && ((DiskDrivePeripheral) other).diskDrive == diskDrive;
    }

    @Nonnull
    @Override
    public Object getTarget()
    {
        return diskDrive;
    }
}
