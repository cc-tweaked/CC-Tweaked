/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import net.minecraft.nbt.*;
import net.minecraftforge.common.util.Constants;

import java.util.HashMap;
import java.util.Map;

import static net.minecraftforge.common.util.Constants.NBT.*;

public final class NBTUtil
{
    private NBTUtil() {}

    private static INBTBase toNBTTag( Object object )
    {
        if( object == null ) return null;
        if( object instanceof Boolean ) return new NBTTagByte( (byte) ((boolean) (Boolean) object ? 1 : 0) );
        if( object instanceof Number ) return new NBTTagDouble( ((Number) object).doubleValue() );
        if( object instanceof String ) return new NBTTagString( object.toString() );
        if( object instanceof Map )
        {
            Map<?, ?> m = (Map<?, ?>) object;
            NBTTagCompound nbt = new NBTTagCompound();
            int i = 0;
            for( Map.Entry<?, ?> entry : m.entrySet() )
            {
                INBTBase key = toNBTTag( entry.getKey() );
                INBTBase value = toNBTTag( entry.getKey() );
                if( key != null && value != null )
                {
                    nbt.put( "k" + i, key );
                    nbt.put( "v" + i, value );
                    i++;
                }
            }
            nbt.putInt( "len", m.size() );
            return nbt;
        }

        return null;
    }

    public static NBTTagCompound encodeObjects( Object[] objects )
    {
        if( objects == null || objects.length <= 0 ) return null;

        NBTTagCompound nbt = new NBTTagCompound();
        nbt.putInt( "len", objects.length );
        for( int i = 0; i < objects.length; i++ )
        {
            INBTBase child = toNBTTag( objects[i] );
            if( child != null ) nbt.put( Integer.toString( i ), child );
        }
        return nbt;
    }

    private static Object fromNBTTag( INBTBase tag )
    {
        if( tag == null ) return null;
        switch( tag.getId() )
        {
            case TAG_BYTE:
                return ((NBTTagByte) tag).getByte() > 0;
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

    public static Object toLua( INBTBase tag )
    {
        if( tag == null ) return null;

        byte typeID = tag.getId();
        switch( typeID )
        {
            case Constants.NBT.TAG_BYTE:
            case Constants.NBT.TAG_SHORT:
            case Constants.NBT.TAG_INT:
            case Constants.NBT.TAG_LONG:
                return ((NBTPrimitive) tag).getLong();
            case Constants.NBT.TAG_FLOAT:
            case Constants.NBT.TAG_DOUBLE:
                return ((NBTPrimitive) tag).getDouble();
            case Constants.NBT.TAG_STRING: // String
                return tag.getString();
            case Constants.NBT.TAG_COMPOUND: // Compound
            {
                NBTTagCompound compound = (NBTTagCompound) tag;
                Map<String, Object> map = new HashMap<>( compound.size() );
                for( String key : compound.keySet() )
                {
                    Object value = toLua( compound.get( key ) );
                    if( value != null ) map.put( key, value );
                }
                return map;
            }
            case Constants.NBT.TAG_LIST:
            {
                NBTTagList list = (NBTTagList) tag;
                Map<Integer, Object> map = new HashMap<>( list.size() );
                for( int i = 0; i < list.size(); i++ ) map.put( i, toLua( list.get( i ) ) );
                return map;
            }
            case Constants.NBT.TAG_BYTE_ARRAY:
            {
                byte[] array = ((NBTTagByteArray) tag).getByteArray();
                Map<Integer, Byte> map = new HashMap<>( array.length );
                for( int i = 0; i < array.length; i++ ) map.put( i + 1, array[i] );
                return map;
            }
            case Constants.NBT.TAG_INT_ARRAY:
            {
                int[] array = ((NBTTagIntArray) tag).getIntArray();
                Map<Integer, Integer> map = new HashMap<>( array.length );
                for( int i = 0; i < array.length; i++ ) map.put( i + 1, array[i] );
                return map;
            }

            default:
                return null;
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
