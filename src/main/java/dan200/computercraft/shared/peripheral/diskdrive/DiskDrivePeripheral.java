/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.diskdrive;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.media.items.ItemDisk;
import dan200.computercraft.shared.util.StringUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

import static dan200.computercraft.core.apis.ArgumentHelper.optString;

public class DiskDrivePeripheral implements IPeripheral
{
    private final TileDiskDrive m_diskDrive;

    public DiskDrivePeripheral( TileDiskDrive diskDrive )
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
            "getDiskID"
        };
    }

    @Override
    public Object[] callMethod( @Nonnull IComputerAccess computer, @Nonnull ILuaContext context, int method, @Nonnull Object[] arguments ) throws LuaException
    {
        switch( method )
        {
            case 0:
            {
                // isDiskPresent
                return new Object[] {
                    !m_diskDrive.getDiskStack().isEmpty()
                };
            }
            case 1:
            {
                // getDiskLabel
                IMedia media = m_diskDrive.getDiskMedia();
                if( media != null )
                {
                    return new Object[] { media.getLabel( m_diskDrive.getDiskStack() ) };
                }
                return null;
            }
            case 2:
            {
                // setDiskLabel
                String label = optString( arguments, 0, null );

                IMedia media = m_diskDrive.getDiskMedia();
                if( media != null )
                {
                    ItemStack disk = m_diskDrive.getDiskStack();
                    label = StringUtil.normaliseLabel( label );
                    if( media.setLabel( disk, label ) )
                    {
                        m_diskDrive.setDiskStack( disk );
                    }
                    else
                    {
                        throw new LuaException( "Disk label cannot be changed" );
                    }
                }
                return null;
            }
            case 3:
            {
                // hasData
                return new Object[] {
                    m_diskDrive.getDiskMountPath( computer ) != null
                };
            }
            case 4:
            {
                // getMountPath
                return new Object[] {
                    m_diskDrive.getDiskMountPath( computer )
                };
            }
            case 5:
            {
                // hasAudio
                IMedia media = m_diskDrive.getDiskMedia();
                if( media != null )
                {
                    return new Object[] { media.getAudio( m_diskDrive.getDiskStack() ) != null };
                }
                return new Object[] { false };
            }
            case 6:
            {
                // getAudioTitle
                IMedia media = m_diskDrive.getDiskMedia();
                if( media != null )
                {
                    return new Object[] { media.getAudioTitle( m_diskDrive.getDiskStack() ) };
                }
                return new Object[] { false };
            }
            case 7:
            {
                // playAudio
                m_diskDrive.playDiskAudio();
                return null;
            }
            case 8:
            {
                // stopAudio
                m_diskDrive.stopDiskAudio();
                return null;
            }
            case 9:
            {
                // eject
                m_diskDrive.ejectDisk();
                return null;
            }
            case 10:
            {
                // getDiskID
                ItemStack disk = m_diskDrive.getDiskStack();
                if( disk != null )
                {
                    Item item = disk.getItem();
                    if( item instanceof ItemDisk )
                    {
                        return new Object[] { ((ItemDisk)item).getDiskID( disk ) };
                    }
                }
                return null;
            }
            default:
            {
                return null;
            }
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
        if( other instanceof DiskDrivePeripheral )
        {
            DiskDrivePeripheral otherDiskDrive = (DiskDrivePeripheral)other;
            if( otherDiskDrive.m_diskDrive == this.m_diskDrive )
            {
                return true;
            }
        }
        return false;
    }

    @Nonnull
    @Override
    public Object getTarget()
    {
        return m_diskDrive;
    }
}
