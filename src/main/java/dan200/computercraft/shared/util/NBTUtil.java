/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import net.minecraft.nbt.*;
import net.minecraftforge.common.util.Constants;

import java.util.HashMap;
import java.util.Map;

public class NBTUtil
{
    private static NBTBase toNBTTag( Object object )
    {
        if( object != null )
        {
            if( object instanceof Boolean )
            {
                boolean b = (Boolean) object;
                return new NBTTagByte( b ? (byte) 1 : (byte) 0 );
            }
            else if( object instanceof Number )
            {
                Double d = ((Number) object).doubleValue();
                return new NBTTagDouble( d );
            }
            else if( object instanceof String )
            {
                String s = object.toString();
                return new NBTTagString( s );
            }
            else if( object instanceof Map )
            {
                Map<?, ?> m = (Map<?, ?>) object;
                NBTTagCompound nbt = new NBTTagCompound();
                int i = 0;
                for( Map.Entry<?, ?> entry : m.entrySet() )
                {
                    NBTBase key = toNBTTag( entry.getKey() );
                    NBTBase value = toNBTTag( entry.getKey() );
                    if( key != null && value != null )
                    {
                        nbt.setTag( "k" + Integer.toString( i ), key );
                        nbt.setTag( "v" + Integer.toString( i ), value );
                        i++;
                    }
                }
                nbt.setInteger( "len", m.size() );
                return nbt;
            }
        }
        return null;
    }

    public static NBTTagCompound encodeObjects( Object[] objects )
    {
        if( objects != null && objects.length > 0 )
        {
            NBTTagCompound nbt = new NBTTagCompound();
            nbt.setInteger( "len", objects.length );
            for( int i = 0; i < objects.length; i++ )
            {
                Object object = objects[i];
                NBTBase tag = toNBTTag( object );
                if( tag != null )
                {
                    nbt.setTag( Integer.toString( i ), tag );
                }
            }
            return nbt;
        }
        return null;
    }

    private static Object fromNBTTag( NBTBase tag )
    {
        if( tag != null )
        {
            byte typeID = tag.getId();
            switch( typeID )
            {
                case Constants.NBT.TAG_BYTE: // byte
                {
                    return (((NBTTagByte) tag).getByte() > 0);
                }
                case Constants.NBT.TAG_DOUBLE: // Double
                {
                    return ((NBTTagDouble) tag).getDouble();
                }
                case Constants.NBT.TAG_STRING: // String
                {
                    return ((NBTTagString) tag).getString();
                }
                case Constants.NBT.TAG_COMPOUND: // Compound
                {
                    NBTTagCompound c = (NBTTagCompound) tag;
                    int len = c.getInteger( "len" );
                    Map<Object, Object> map = new HashMap<>( len );
                    for( int i = 0; i < len; i++ )
                    {
                        Object key = fromNBTTag( c.getTag( "k" + Integer.toString( i ) ) );
                        Object value = fromNBTTag( c.getTag( "v" + Integer.toString( i ) ) );
                        if( key != null && value != null )
                        {
                            map.put( key, value );
                        }
                    }
                    return map;
                }
            }
        }
        return null;
    }

    public static Object[] decodeObjects( NBTTagCompound tagCompound )
    {
        int len = tagCompound.getInteger( "len" );
        if( len > 0 )
        {
            Object[] objects = new Object[len];
            for( int i = 0; i < len; i++ )
            {
                String key = Integer.toString( i );
                if( tagCompound.hasKey( key ) )
                {
                    NBTBase tag = tagCompound.getTag( key );
                    objects[i] = fromNBTTag( tag );
                }
            }
            return objects;
        }
        return null;
    }
}
