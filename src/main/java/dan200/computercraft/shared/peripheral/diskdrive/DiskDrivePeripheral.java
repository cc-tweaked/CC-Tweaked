/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.diskdrive;

import dan200.computercraft.api.lua.ICallContext;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.media.items.ItemDiskLegacy;
import dan200.computercraft.shared.util.StringUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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

    @Nonnull
    @Override
    public MethodResult callMethod( @Nonnull IComputerAccess computer, @Nonnull ICallContext context, int method, @Nonnull Object[] arguments ) throws LuaException
    {
        switch( method )
        {
            case 0:
            {
                // isDiskPresent
                return MethodResult.of(
                    !m_diskDrive.getDiskStack().isEmpty()
                );
            }
            case 1:
            {
                // getDiskLabel
                IMedia media = m_diskDrive.getDiskMedia();
                if( media != null )
                {
                    return MethodResult.of( media.getLabel( m_diskDrive.getDiskStack() ) );
                }
                return MethodResult.empty();
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
                return MethodResult.empty();
            }
            case 3:
            {
                // hasData
                return MethodResult.of(
                    m_diskDrive.getDiskMountPath( computer ) != null
                );
            }
            case 4:
            {
                // getMountPath
                return MethodResult.of(
                    m_diskDrive.getDiskMountPath( computer )
                );
            }
            case 5:
            {
                // hasAudio
                IMedia media = m_diskDrive.getDiskMedia();
                if( media != null )
                {
                    return MethodResult.of( media.getAudio( m_diskDrive.getDiskStack() ) != null );
                }
                return MethodResult.of( false );
            }
            case 6:
            {
                // getAudioTitle
                IMedia media = m_diskDrive.getDiskMedia();
                if( media != null )
                {
                    return MethodResult.of( media.getAudioTitle( m_diskDrive.getDiskStack() ) );
                }
                return MethodResult.of( false );
            }
            case 7:
            {
                // playAudio
                m_diskDrive.playDiskAudio();
                return MethodResult.empty();
            }
            case 8:
            {
                // stopAudio
                m_diskDrive.stopDiskAudio();
                return MethodResult.empty();
            }
            case 9:
            {
                // eject
                m_diskDrive.ejectDisk();
                return MethodResult.empty();
            }
            case 10:
            {
                // getDiskID
                ItemStack disk = m_diskDrive.getDiskStack();
                if( disk != null )
                {
                    Item item = disk.getItem();
                    if( item instanceof ItemDiskLegacy )
                    {
                        return MethodResult.of( ((ItemDiskLegacy)item).getDiskID( disk ) );
                    }
                }
                return MethodResult.empty();
            }
            default:
            {
                return MethodResult.empty();
            }
        }
    }

    @Nullable
    @Override
    @Deprecated
    public Object[] callMethod( @Nonnull IComputerAccess access, @Nonnull ILuaContext context, int method, @Nonnull Object[] arguments ) throws LuaException, InterruptedException
    {
        return callMethod( access, (ICallContext) context, method, arguments ).evaluate( context );
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
