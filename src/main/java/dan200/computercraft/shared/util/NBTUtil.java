/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import net.minecraft.nbt.*;

import java.util.HashMap;
import java.util.Map;

public class NBTUtil
{
    public static final int TAG_END = 0;
    public static final int TAG_BYTE = 1;
    public static final int TAG_SHORT = 2;
    public static final int TAG_INT = 3;
    public static final int TAG_LONG = 4;
    public static final int TAG_FLOAT = 5;
    public static final int TAG_DOUBLE = 6;
    public static final int TAG_BYTE_ARRAY = 7;
    public static final int TAG_STRING = 8;
    public static final int TAG_LIST = 9;
    public static final int TAG_COMPOUND = 10;
    public static final int TAG_INT_ARRAY = 11;
    public static final int TAG_LONG_ARRAY = 12;
    public static final int TAG_ANY_NUMERIC = 99;

    private static Tag toNBTTag( Object object )
    {
        if( object instanceof Boolean )
        {
            return new ByteTag( (Boolean) object ? (byte) 1 : (byte) 0 );
        }
        else if( object instanceof Number )
        {
            return new DoubleTag( ((Number) object).doubleValue() );
        }
        else if( object instanceof String )
        {
            return new StringTag( object.toString() );
        }
        else if( object instanceof Map )
        {
            Map<?, ?> map = (Map<?, ?>) object;
            CompoundTag tag = new CompoundTag();
            tag.putInt( "len", map.size() );
            int i = 0;
            for( Map.Entry<?, ?> entry : map.entrySet() )
            {
                Tag key = toNBTTag( entry.getKey() );
                Tag value = toNBTTag( entry.getKey() );
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

    public static CompoundTag encodeObjects( Object[] objects )
    {
        if( objects == null || objects.length <= 0 ) return null;

        CompoundTag tag = new CompoundTag();
        tag.putInt( "len", objects.length );
        for( int i = 0; i < objects.length; i++ )
        {
            Tag child = toNBTTag( objects[i] );
            if( child != null ) tag.put( Integer.toString( i ), child );
        }
        return tag;
    }

    private static Object fromNBTTag( Tag tag )
    {
        if( tag == null ) return null;
        switch( tag.getType() )
        {
            case TAG_BYTE:
                return (((ByteTag) tag).getByte() > 0);
            case TAG_DOUBLE:
                return ((DoubleTag) tag).getDouble();
            default:
            case TAG_STRING:
                return tag.asString();
            case TAG_COMPOUND:
            {
                CompoundTag c = (CompoundTag) tag;
                int len = c.getInt( "len" );
                Map<Object, Object> map = new HashMap<>( len );
                for( int i = 0; i < len; i++ )
                {
                    Object key = fromNBTTag( c.getTag( "k" + i ) );
                    Object value = fromNBTTag( c.getTag( "v" + i ) );
                    if( key != null && value != null ) map.put( key, value );
                }
                return map;
            }
        }
    }

    public static Object[] decodeObjects( CompoundTag tag )
    {
        int len = tag.getInt( "len" );
        if( len <= 0 ) return null;

        Object[] objects = new Object[len];
        for( int i = 0; i < len; i++ )
        {
            String key = Integer.toString( i );
            if( tag.containsKey( key ) )
            {
                objects[i] = fromNBTTag( tag.getTag( key ) );
            }
        }
        return objects;
    }
}
