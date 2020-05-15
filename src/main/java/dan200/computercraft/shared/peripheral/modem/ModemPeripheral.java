/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.modem;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.network.IPacketNetwork;
import dan200.computercraft.api.network.IPacketReceiver;
import dan200.computercraft.api.network.IPacketSender;
import dan200.computercraft.api.network.Packet;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;

public abstract class ModemPeripheral implements IPeripheral, IPacketSender, IPacketReceiver
{
    private IPacketNetwork m_network;
    private final Set<IComputerAccess> m_computers = new HashSet<>( 1 );
    private final ModemState m_state;

    protected ModemPeripheral( ModemState state )
    {
        m_state = state;
    }

    public ModemState getModemState()
    {
        return m_state;
    }

    private synchronized void setNetwork( IPacketNetwork network )
    {
        if( m_network == network ) return;

        // Leave old network
        if( m_network != null ) m_network.removeReceiver( this );

        // Set new network
        m_network = network;

        // Join new network
        if( m_network != null ) m_network.addReceiver( this );
    }

    public void destroy()
    {
        setNetwork( null );
    }

    @Override
    public void receiveSameDimension( @Nonnull Packet packet, double distance )
    {
        if( packet.getSender() == this || !m_state.isOpen( packet.getChannel() ) ) return;

        synchronized( m_computers )
        {
            for( IComputerAccess computer : m_computers )
            {
                computer.queueEvent( "modem_message",
                    computer.getAttachmentName(), packet.getChannel(), packet.getReplyChannel(), packet.getPayload(), distance );
            }
        }
    }

    @Override
    public void receiveDifferentDimension( @Nonnull Packet packet )
    {
        if( packet.getSender() == this || !m_state.isOpen( packet.getChannel() ) ) return;

        synchronized( m_computers )
        {
            for( IComputerAccess computer : m_computers )
            {
                computer.queueEvent( "modem_message",
                    computer.getAttachmentName(), packet.getChannel(), packet.getReplyChannel(), packet.getPayload() );
            }
        }
    }

    protected abstract IPacketNetwork getNetwork();

    @Nonnull
    @Override
    public String getType()
    {
        return "modem";
    }

    private static int parseChannel( int channel ) throws LuaException
    {
        if( channel < 0 || channel > 65535 ) throw new LuaException( "Expected number in range 0-65535" );
        return channel;
    }

    @LuaFunction
    public final void open( int channel ) throws LuaException
    {
        m_state.open( parseChannel( channel ) );
    }

    @LuaFunction
    public final boolean isOpen( int channel ) throws LuaException
    {
        return m_state.isOpen( parseChannel( channel ) );
    }

    @LuaFunction
    public final void close( int channel ) throws LuaException
    {
        m_state.close( parseChannel( channel ) );
    }

    @LuaFunction
    public final void closeAll()
    {
        m_state.closeAll();
    }

    @LuaFunction
    public final void transmit( int channel, int replyChannel, Object payload ) throws LuaException
    {
        parseChannel( channel );
        parseChannel( replyChannel );

        World world = getWorld();
        Vec3d position = getPosition();
        IPacketNetwork network = m_network;

        if( world == null || position == null || network == null ) return;

        Packet packet = new Packet( channel, replyChannel, payload, this );
        if( isInterdimensional() )
        {
            network.transmitInterdimensional( packet );
        }
        else
        {
            network.transmitSameDimension( packet, getRange() );
        }
    }

    @LuaFunction
    public final boolean isWireless()
    {
        IPacketNetwork network = m_network;
        return network != null && network.isWireless();
    }

    @Override
    public synchronized void attach( @Nonnull IComputerAccess computer )
    {
        synchronized( m_computers )
        {
            m_computers.add( computer );
        }

        setNetwork( getNetwork() );
    }

    @Override
    public synchronized void detach( @Nonnull IComputerAccess computer )
    {
        boolean empty;
        synchronized( m_computers )
        {
            m_computers.remove( computer );
            empty = m_computers.isEmpty();
        }

        if( empty ) setNetwork( null );
    }

    @Nonnull
    @Override
    public String getSenderID()
    {
        synchronized( m_computers )
        {
            if( m_computers.size() != 1 )
            {
                return "unknown";
            }
            else
            {
                IComputerAccess computer = m_computers.iterator().next();
                return computer.getID() + "_" + computer.getAttachmentName();
            }
        }
    }
}
