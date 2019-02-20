/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import net.minecraft.nbt.*;

import java.util.HashMap;
import java.util.Map;

import static net.minecraftforge.common.util.Constants.NBT.*;

public class NBTUtil
{
    private static INBTBase toNBTTag( Object object )
    {
        if( object instanceof Boolean )
        {
            return new NBTTagByte( (Boolean) object ? (byte) 1 : (byte) 0 );
        }
        else if( object instanceof Number )
        {
            return new NBTTagDouble( ((Number) object).doubleValue() );
        }
        else if( object instanceof String )
        {
            return new NBTTagString( object.toString() );
        }
        else if( object instanceof Map )
        {
            Map<?, ?> map = (Map<?, ?>) object;
            NBTTagCompound tag = new NBTTagCompound();
            tag.putInt( "len", map.size() );
            int i = 0;
            for( Map.Entry<?, ?> entry : map.entrySet() )
            {
                INBTBase key = toNBTTag( entry.getKey() );
                INBTBase value = toNBTTag( entry.getKey() );
                if( key != null && value != null )
                {
                    tag.put( "k" + i, key );
                    tag.put( "v" + i, value );
                    i++;
                }
            }
            return tag;
        }
        else
        {
            return null;
        }
    }

    public static NBTTagCompound encodeObjects( Object[] objects )
    {
        if( objects == null || objects.length <= 0 ) return null;

        NBTTagCompound tag = new NBTTagCompound();
        tag.putInt( "len", objects.length );
        for( int i = 0; i < objects.length; i++ )
        {
            INBTBase child = toNBTTag( objects[i] );
            if( child != null ) tag.put( Integer.toString( i ), child );
        }
        return tag;
    }

    private static Object fromNBTTag( INBTBase tag )
    {
        if( tag == null ) return null;
        switch( tag.getId() )
        {
            case TAG_BYTE:
                return (((NBTTagByte) tag).getByte() > 0);
            case TAG_DOUBLE:
                return ((NBTTagDouble) tag).getDouble();
            default:
            case TAG_STRING:
                return tag.getString();
            case TAG_COMPOUND:
            {
                NBTTagCompound c = (NBTTagCompound) tag;
                int len = c.getInt( "len" );
                Map<Object, Object> map = new HashMap<>( len );
                for( int i = 0; i < len; i++ )
                {
                    Object key = fromNBTTag( c.get( "k" + i ) );
                    Object value = fromNBTTag( c.get( "v" + i ) );
                    if( key != null && value != null ) map.put( key, value );
                }
                return map;
            }
        }
    }

    public static Object[] decodeObjects( NBTTagCompound tag )
    {
        int len = tag.getInt( "len" );
        if( len <= 0 ) return null;

        Object[] objects = new Object[len];
        for( int i = 0; i < len; i++ )
        {
            String key = Integer.toString( i );
            if( tag.contains( key ) )
            {
                objects[i] = fromNBTTag( tag.get( key ) );
            }
        }
        return objects;
    }
}
