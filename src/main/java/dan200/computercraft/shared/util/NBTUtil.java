/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import dan200.computercraft.ComputerCraft;
import net.minecraft.nbt.*;
import net.minecraftforge.common.util.Constants;
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

import static net.minecraftforge.common.util.Constants.NBT.*;

public final class NBTUtil
{
    private NBTUtil() {}

    private static INBT toNBTTag( Object object )
    {
        if( object == null ) return null;
        if( object instanceof Boolean ) return ByteNBT.valueOf( (byte) ((boolean) (Boolean) object ? 1 : 0) );
        if( object instanceof Number ) return DoubleNBT.valueOf( ((Number) object).doubleValue() );
        if( object instanceof String ) return StringNBT.valueOf( object.toString() );
        if( object instanceof Map )
        {
            Map<?, ?> m = (Map<?, ?>) object;
            CompoundNBT nbt = new CompoundNBT();
            int i = 0;
            for( Map.Entry<?, ?> entry : m.entrySet() )
            {
                INBT key = toNBTTag( entry.getKey() );
                INBT value = toNBTTag( entry.getKey() );
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

    public static CompoundNBT encodeObjects( Object[] objects )
    {
        if( objects == null || objects.length <= 0 ) return null;

        CompoundNBT nbt = new CompoundNBT();
        nbt.putInt( "len", objects.length );
        for( int i = 0; i < objects.length; i++ )
        {
            INBT child = toNBTTag( objects[i] );
            if( child != null ) nbt.put( Integer.toString( i ), child );
        }
        return nbt;
    }

    private static Object fromNBTTag( INBT tag )
    {
        if( tag == null ) return null;
        switch( tag.getId() )
        {
            case TAG_BYTE:
                return ((ByteNBT) tag).getAsByte() > 0;
            case TAG_DOUBLE:
                return ((DoubleNBT) tag).getAsDouble();
            default:
            case TAG_STRING:
                return tag.getAsString();
            case TAG_COMPOUND:
            {
                CompoundNBT c = (CompoundNBT) tag;
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

    public static Object toLua( INBT tag )
    {
        if( tag == null ) return null;

        byte typeID = tag.getId();
        switch( typeID )
        {
            case Constants.NBT.TAG_BYTE:
            case Constants.NBT.TAG_SHORT:
            case Constants.NBT.TAG_INT:
            case Constants.NBT.TAG_LONG:
                return ((NumberNBT) tag).getAsLong();
            case Constants.NBT.TAG_FLOAT:
            case Constants.NBT.TAG_DOUBLE:
                return ((NumberNBT) tag).getAsDouble();
            case Constants.NBT.TAG_STRING: // String
                return tag.getAsString();
            case Constants.NBT.TAG_COMPOUND: // Compound
            {
                CompoundNBT compound = (CompoundNBT) tag;
                Map<String, Object> map = new HashMap<>( compound.size() );
                for( String key : compound.getAllKeys() )
                {
                    Object value = toLua( compound.get( key ) );
                    if( value != null ) map.put( key, value );
                }
                return map;
            }
            case Constants.NBT.TAG_LIST:
            {
                ListNBT list = (ListNBT) tag;
                Map<Integer, Object> map = new HashMap<>( list.size() );
                for( int i = 0; i < list.size(); i++ ) map.put( i, toLua( list.get( i ) ) );
                return map;
            }
            case Constants.NBT.TAG_BYTE_ARRAY:
            {
                byte[] array = ((ByteArrayNBT) tag).getAsByteArray();
                Map<Integer, Byte> map = new HashMap<>( array.length );
                for( int i = 0; i < array.length; i++ ) map.put( i + 1, array[i] );
                return map;
            }
            case Constants.NBT.TAG_INT_ARRAY:
            {
                int[] array = ((IntArrayNBT) tag).getAsIntArray();
                Map<Integer, Integer> map = new HashMap<>( array.length );
                for( int i = 0; i < array.length; i++ ) map.put( i + 1, array[i] );
                return map;
            }

            default:
                return null;
        }
    }

    public static Object[] decodeObjects( CompoundNBT tag )
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
    public static String getNBTHash( @Nullable CompoundNBT tag )
    {
        if( tag == null ) return null;

        try
        {
            MessageDigest digest = MessageDigest.getInstance( "MD5" );
            DataOutput output = new DataOutputStream( new DigestOutputStream( digest ) );
            CompressedStreamTools.write( tag, output );
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
