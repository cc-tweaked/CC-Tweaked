/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2018. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.datafix;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.datafix.IFixableData;

import javax.annotation.Nonnull;

import static dan200.computercraft.ComputerCraft.MOD_ID;
import static dan200.computercraft.shared.datafix.Fixes.VERSION;

/**
 * Fixes up the botched tile entity IDs from the 1.11 port.
 */
public class TileEntityDataFixer implements IFixableData
{
    @Override
    public int getFixVersion()
    {
        return VERSION;
    }

    @Nonnull
    @Override
    public NBTTagCompound fixTagCompound( @Nonnull NBTTagCompound tag )
    {
        String id = tag.getString( "id" );
        if( id.startsWith( MOD_ID + " : " ) )
        {
            tag.setString( "id", id.replaceFirst( MOD_ID + " : ", MOD_ID + ":" ) );
        }
        return tag;
    }
}
