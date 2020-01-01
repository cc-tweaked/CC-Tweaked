/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.diskdrive;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.MediaProviders;
import dan200.computercraft.shared.media.items.ItemDiskLegacy;
import dan200.computercraft.shared.util.StringUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

import static dan200.computercraft.api.lua.ArgumentHelper.optString;

class DiskDrivePeripheral implements IPeripheral
{
    private final TileDiskDrive m_diskDrive;

    DiskDrivePeripheral( TileDiskDrive diskDrive )
    {
        m_diskDrive = diskDrive;
    }

    @Nonnull
    @Override
    public String getType()
    {
        return "drive";
    }

    @Nonnull
    @Override
    public String[] getMethodNames()
    {
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
            "getDiskID",
        };
    }

    @Override
    public Object[] callMethod( @Nonnull IComputerAccess computer, @Nonnull ILuaContext context, int method, @Nonnull Object[] arguments ) throws LuaException, InterruptedException
    {
        switch( method )
        {
            case 0: // isDiskPresent
                return new Object[] { !m_diskDrive.getDiskStack().isEmpty() };
            case 1: // getDiskLabel
            {
                ItemStack stack = m_diskDrive.getDiskStack();
                IMedia media = MediaProviders.get( stack );
                return media == null ? null : new Object[] { media.getLabel( stack ) };
            }
            case 2: // setDiskLabel
            {
                String label = optString( arguments, 0, null );

                return context.executeMainThreadTask( () -> {
                    ItemStack stack = m_diskDrive.getDiskStack();
                    IMedia media = MediaProviders.get( stack );
                    if( media == null ) return null;

                    if( !media.setLabel( stack, StringUtil.normaliseLabel( label ) ) )
                    {
                        throw new LuaException( "Disk label cannot be changed" );
                    }
                    m_diskDrive.setDiskStack( stack );
                    return null;
                } );
            }
            case 3: // hasData
                return new Object[] { m_diskDrive.getDiskMountPath( computer ) != null };
            case 4: // getMountPath
                return new Object[] { m_diskDrive.getDiskMountPath( computer ) };
            case 5:
            {
                // hasAudio
                ItemStack stack = m_diskDrive.getDiskStack();
                IMedia media = MediaProviders.get( stack );
                return new Object[] { media != null && media.getAudio( stack ) != null };
            }
            case 6:
            {
                // getAudioTitle
                ItemStack stack = m_diskDrive.getDiskStack();
                IMedia media = MediaProviders.get( stack );
                return new Object[] { media != null ? media.getAudioTitle( stack ) : false };
            }
            case 7: // playAudio
                m_diskDrive.playDiskAudio();
                return null;
            case 8: // stopAudio
                m_diskDrive.stopDiskAudio();
                return null;
            case 9: // eject
                m_diskDrive.ejectDisk();
                return null;
            case 10: // getDiskID
            {
                ItemStack disk = m_diskDrive.getDiskStack();
                Item item = disk.getItem();
                return item instanceof ItemDiskLegacy ? new Object[] { ((ItemDiskLegacy) item).getDiskID( disk ) } : null;
            }
            default:
                return null;
        }
    }

    @Override
    public void attach( @Nonnull IComputerAccess computer )
    {
        m_diskDrive.mount( computer );
    }

    @Override
    public void detach( @Nonnull IComputerAccess computer )
    {
        m_diskDrive.unmount( computer );
    }

    @Override
    public boolean equals( IPeripheral other )
    {
        return this == other || other instanceof DiskDrivePeripheral && ((DiskDrivePeripheral) other).m_diskDrive == m_diskDrive;
    }

    @Nonnull
    @Override
    public Object getTarget()
    {
        return m_diskDrive;
    }
}
