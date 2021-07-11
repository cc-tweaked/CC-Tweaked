/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import dan200.computercraft.ComputerCraft;
import net.minecraft.nbt.*;
import org.apache.commons.codec.binary.Hex;

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

    private NBTUtil() {}

    private static NbtElement toNBTTag( Object object )
    {
        if( object == null )
        {
            return null;
        }
        if( object instanceof Boolean )
        {
            return NbtByte.of( (byte) ((boolean) (Boolean) object ? 1 : 0) );
        }
        if( object instanceof Number )
        {
            return NbtDouble.of( ((Number) object).doubleValue() );
        }
        if( object instanceof String )
        {
            return NbtString.of( object.toString() );
        }
        if( object instanceof Map )
        {
            Map<?, ?> m = (Map<?, ?>) object;
            NbtCompound nbt = new NbtCompound();
            int i = 0;
            for( Map.Entry<?, ?> entry : m.entrySet() )
            {
                NbtElement key = toNBTTag( entry.getKey() );
                NbtElement value = toNBTTag( entry.getKey() );
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

    public static NbtCompound encodeObjects( Object[] objects )
    {
        if( objects == null || objects.length <= 0 )
        {
            return null;
        }

        NbtCompound nbt = new NbtCompound();
        nbt.putInt( "len", objects.length );
        for( int i = 0; i < objects.length; i++ )
        {
            NbtElement child = toNBTTag( objects[i] );
            if( child != null )
            {
                nbt.put( Integer.toString( i ), child );
            }
        }
        return nbt;
    }

    private static Object fromNBTTag( NbtElement tag )
    {
        if( tag == null )
        {
            return null;
        }
        switch( tag.getType() )
        {
            case TAG_BYTE:
                return ((NbtByte) tag).byteValue() > 0;
            case TAG_DOUBLE:
                return ((NbtDouble) tag).doubleValue();
            default:
            case TAG_STRING:
                return tag.asString();
            case TAG_COMPOUND:
                NbtCompound c = (NbtCompound) tag;
                int len = c.getInt( "len" );
                Map<Object, Object> map = new HashMap<>( len );
                for( int i = 0; i < len; i++ )
                {
                    Object key = fromNBTTag( c.get( "k" + i ) );
                    Object value = fromNBTTag( c.get( "v" + i ) );
                    if( key != null && value != null )
                    {
                        map.put( key, value );
                    }
                }
                return map;
        }
    }

    public static Object toLua( NbtElement tag )
    {
        if( tag == null )
        {
            return null;
        }

        byte typeID = tag.getType();
        switch( typeID )
        {
            case TAG_BYTE:
            case TAG_SHORT:
            case TAG_INT:
            case TAG_LONG:
                return ((AbstractNbtNumber) tag).longValue();
            case TAG_FLOAT:
            case TAG_DOUBLE:
                return ((AbstractNbtNumber) tag).doubleValue();
            case TAG_STRING: // String
                return tag.asString();
            case TAG_COMPOUND: // Compound
            {
                NbtCompound compound = (NbtCompound) tag;
                Map<String, Object> map = new HashMap<>( compound.getSize() );
                for( String key : compound.getKeys() )
                {
                    Object value = toLua( compound.get( key ) );
                    if( value != null )
                    {
                        map.put( key, value );
                    }
                }
                return map;
            }
            case TAG_LIST:
            {
                NbtList list = (NbtList) tag;
                Map<Integer, Object> map = new HashMap<>( list.size() );
                for( int i = 0; i < list.size(); i++ )
                {
                    map.put( i, toLua( list.get( i ) ) );
                }
                return map;
            }
            case TAG_BYTE_ARRAY:
            {
                byte[] array = ((NbtByteArray) tag).getByteArray();
                Map<Integer, Byte> map = new HashMap<>( array.length );
                for( int i = 0; i < array.length; i++ )
                {
                    map.put( i + 1, array[i] );
                }
                return map;
            }
            case TAG_INT_ARRAY:
                int[] array = ((NbtIntArray) tag).getIntArray();
                Map<Integer, Integer> map = new HashMap<>( array.length );
                for( int i = 0; i < array.length; i++ )
                {
                    map.put( i + 1, array[i] );
                }
                return map;

            default:
                return null;
        }
    }

    public static Object[] decodeObjects( NbtCompound tag )
    {
        int len = tag.getInt( "len" );
        if( len <= 0 )
        {
            return null;
        }

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
    public static String getNBTHash( @Nullable NbtCompound tag )
    {
        if( tag == null )
        {
            return null;
        }

        try
        {
            MessageDigest digest = MessageDigest.getInstance( "MD5" );
            DataOutput output = new DataOutputStream( new DigestOutputStream( digest ) );
            NbtIo.write( tag, output );
            byte[] hash = digest.digest();
            return new String( Hex.encodeHex( hash ) );
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
        public void write( int b )
        {
            digest.update( (byte) b );
        }

        @Override
        public void write( @Nonnull byte[] b, int off, int len )
        {
            digest.update( b, off, len );
        }
    }
}
