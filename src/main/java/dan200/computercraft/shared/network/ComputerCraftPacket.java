/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
 
public class ComputerCraftPacket
{
    // Packet types
    // To server
    public static final byte TurnOn = 1;
    public static final byte Reboot = 2;
    public static final byte Shutdown = 3;
    public static final byte QueueEvent = 4;
    public static final byte RequestComputerUpdate = 5;
    public static final byte SetLabel = 6;
    public static final byte RequestTileEntityUpdate = 9;

    // To client
    public static final byte ComputerChanged = 7;
    public static final byte ComputerTerminalChanged = 8;
    public static final byte ComputerDeleted = 9;

    // Packet class
    public byte m_packetType;
    public String[] m_dataString;
    public int[] m_dataInt;
    public byte[][] m_dataByte;
    public NBTTagCompound m_dataNBT;

    public ComputerCraftPacket()
    {
        m_packetType = 0;
        m_dataString = null;
        m_dataInt = null;
        m_dataByte = null;
        m_dataNBT = null;
    }

    public void toBytes( PacketBuffer buffer )
    {
        buffer.writeByte( m_packetType );
        if( m_dataString != null )
        {
            buffer.writeByte( m_dataString.length );
        }
        else
        {
            buffer.writeByte( 0 );
        }
        if( m_dataInt != null )
        {
            buffer.writeByte( m_dataInt.length );
        }
        else
        {
            buffer.writeByte( 0 );
        }
        if( m_dataByte != null )
        {
            buffer.writeInt( m_dataByte.length );
        }
        else
        {
            buffer.writeInt( 0 );
        }
        if( m_dataString != null )
        {
            for( String s : m_dataString )
            {
                if( s != null )
                {
                    try
                    {
                        byte[] b = s.getBytes( "UTF-8" );
                        buffer.writeBoolean( true );
                        buffer.writeInt( b.length );
                        buffer.writeBytes( b );
                    }
                    catch( UnsupportedEncodingException e )
                    {
                        buffer.writeBoolean( false );
                    }
                }
                else
                {
                    buffer.writeBoolean( false );
                }
            }
        }
        if( m_dataInt != null )
        {
            for( int i : m_dataInt )
            {
                buffer.writeInt( i );
            }
        }
        if( m_dataByte != null )
        {
            for( byte[] bytes : m_dataByte )
            {
                if( bytes != null )
                {
                    buffer.writeInt( bytes.length );
                    buffer.writeBytes( bytes );
                }
                else
                {
                    buffer.writeInt( 0 );
                }
            }
        }
        if( m_dataNBT != null )
        {
            try
            {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                CompressedStreamTools.writeCompressed( m_dataNBT, bos );
                byte[] bytes = bos.toByteArray();
                buffer.writeBoolean( true );
                buffer.writeInt( bytes.length );
                buffer.writeBytes( bytes );
            }
            catch( IOException e )
            {
                buffer.writeBoolean( false );
            }
        }
        else
        {
            buffer.writeBoolean( false );
        }
    }

    public void fromBytes( ByteBuf buffer )
    {
        m_packetType = buffer.readByte();
        byte nString = buffer.readByte();
        byte nInt = buffer.readByte();
        int nByte = buffer.readInt();
        if( nString == 0 )
        {
            m_dataString = null;
        }
        else
        {
            m_dataString = new String[ nString ];
            for( int k = 0; k < nString; k++ )
            {
                if( buffer.readBoolean() )
                {
                    int len = buffer.readInt();
                    byte[] b = new byte[len];
                    buffer.readBytes( b );
                    try
                    {
                        m_dataString[ k ] = new String( b, "UTF-8" );
                    }
                    catch( UnsupportedEncodingException e )
                    {
                        m_dataString[ k ] = null;
                    }
                }
            }
        }
        if( nInt == 0 )
        {
            m_dataInt = null;
        }
        else
        {
            m_dataInt = new int[ nInt ];
            for( int k = 0; k < nInt; k++ )
            {
                m_dataInt[ k ] = buffer.readInt();
            }
        }
        if( nByte == 0 )
        {
            m_dataByte = null;
        }
        else
        {
            m_dataByte = new byte[ nByte ][];
            for( int k = 0; k < nByte; k++ )
            {
                int length = buffer.readInt();
                if( length > 0 )
                {
                    m_dataByte[ k ] = new byte[ length ];
                    buffer.getBytes( buffer.readerIndex(), m_dataByte[ k ] );
                }
            }
        }
        boolean bNBT = buffer.readBoolean();
        if( !bNBT )
        {
            m_dataNBT = null;
        }
        else
        {
            int byteLength = buffer.readInt();
            byte[] bytes = new byte[ byteLength ];
            buffer.getBytes( buffer.readerIndex(), bytes );
            try
            {
                ByteArrayInputStream bis = new ByteArrayInputStream( bytes );
                m_dataNBT = CompressedStreamTools.readCompressed( bis );
            }
            catch( IOException e )
            {
                m_dataNBT = null;
            }
        }
    }

    /**
     * Determine whether this packet requires the player to be interacting with the
     * target.
     */
    public boolean requiresContainer() {
        return m_packetType != RequestComputerUpdate && m_packetType != RequestTileEntityUpdate;
    }
}
