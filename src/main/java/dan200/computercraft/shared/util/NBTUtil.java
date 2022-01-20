/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import com.google.common.io.BaseEncoding;
import dan200.computercraft.ComputerCraft;
import net.minecraft.nbt.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public final class NBTUtil
{
    private static final BaseEncoding ENCODING = BaseEncoding.base16().lowerCase();

    private NBTUtil() {}

    private static Tag toNBTTag( Object object )
    {
        if( object == null ) return null;
        if( object instanceof Boolean ) return ByteTag.valueOf( (byte) ((boolean) (Boolean) object ? 1 : 0) );
        if( object instanceof Number ) return DoubleTag.valueOf( ((Number) object).doubleValue() );
        if( object instanceof String ) return StringTag.valueOf( object.toString() );
        if( object instanceof Map<?, ?> m )
        {
            CompoundTag nbt = new CompoundTag();
            int i = 0;
            for( Map.Entry<?, ?> entry : m.entrySet() )
            {
                Tag key = toNBTTag( entry.getKey() );
                Tag value = toNBTTag( entry.getKey() );
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

    public static CompoundTag encodeObjects( Object[] objects )
    {
        if( objects == null || objects.length <= 0 ) return null;

        CompoundTag nbt = new CompoundTag();
        nbt.putInt( "len", objects.length );
        for( int i = 0; i < objects.length; i++ )
        {
            Tag child = toNBTTag( objects[i] );
            if( child != null ) nbt.put( Integer.toString( i ), child );
        }
        return nbt;
    }

    private static Object fromNBTTag( Tag tag )
    {
        if( tag == null ) return null;
        switch( tag.getId() )
        {
            case Tag.TAG_BYTE:
                return ((ByteTag) tag).getAsByte() > 0;
            case Tag.TAG_DOUBLE:
                return ((DoubleTag) tag).getAsDouble();
            default:
            case Tag.TAG_STRING:
                return tag.getAsString();
            case Tag.TAG_COMPOUND:
            {
                CompoundTag c = (CompoundTag) tag;
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

    public static Object toLua( Tag tag )
    {
        if( tag == null ) return null;

        byte typeID = tag.getId();
        switch( typeID )
        {
            case Tag.TAG_BYTE:
            case Tag.TAG_SHORT:
            case Tag.TAG_INT:
            case Tag.TAG_LONG:
                return ((NumericTag) tag).getAsLong();
            case Tag.TAG_FLOAT:
            case Tag.TAG_DOUBLE:
                return ((NumericTag) tag).getAsDouble();
            case Tag.TAG_STRING: // String
                return tag.getAsString();
            case Tag.TAG_COMPOUND: // Compound
            {
                CompoundTag compound = (CompoundTag) tag;
                Map<String, Object> map = new HashMap<>( compound.size() );
                for( String key : compound.getAllKeys() )
                {
                    Object value = toLua( compound.get( key ) );
                    if( value != null ) map.put( key, value );
                }
                return map;
            }
            case Tag.TAG_LIST:
            {
                ListTag list = (ListTag) tag;
                Map<Integer, Object> map = new HashMap<>( list.size() );
                for( int i = 0; i < list.size(); i++ ) map.put( i, toLua( list.get( i ) ) );
                return map;
            }
            case Tag.TAG_BYTE_ARRAY:
            {
                byte[] array = ((ByteArrayTag) tag).getAsByteArray();
                Map<Integer, Byte> map = new HashMap<>( array.length );
                for( int i = 0; i < array.length; i++ ) map.put( i + 1, array[i] );
                return map;
            }
            case Tag.TAG_INT_ARRAY:
            {
                int[] array = ((IntArrayTag) tag).getAsIntArray();
                Map<Integer, Integer> map = new HashMap<>( array.length );
                for( int i = 0; i < array.length; i++ ) map.put( i + 1, array[i] );
                return map;
            }

            default:
                return null;
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
            if( tag.contains( key ) )
            {
                objects[i] = fromNBTTag( tag.get( key ) );
            }
        }
        return objects;
    }

    @Nullable
    public static String getNBTHash( @Nullable CompoundTag tag )
    {
        if( tag == null ) return null;

        try
        {
            MessageDigest digest = MessageDigest.getInstance( "MD5" );
            DataOutput output = new DataOutputStream( new DigestOutputStream( digest ) );
            NbtIo.write( tag, output );
            byte[] hash = digest.digest();
            return ENCODING.encode( hash );
        }
        catch( NoSuchAlgorithmException | IOException e )
        {
            ComputerCraft.log.error( "Cannot hash NBT", e );
            return null;
        }
    }

    private static final class DigestOutputStream extends OutputStream
    {
        private final MessageDigest digest;

        DigestOutputStream( MessageDigest digest )
        {
            this.digest = digest;
        }

        @Override
        public void write( @Nonnull byte[] b, int off, int len )
        {
            digest.update( b, off, len );
        }

        @Override
        public void write( int b )
        {
            digest.update( (byte) b );
        }
    }
}
