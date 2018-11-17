/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.modem;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
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

import static dan200.computercraft.core.apis.ArgumentHelper.getInt;

public abstract class ModemPeripheral implements IPeripheral, IPacketSender, IPacketReceiver
{
    private IPacketNetwork m_network;
    private Set<IComputerAccess> m_computers = new HashSet<>( 1 );
    private final ModemState m_state;

    protected ModemPeripheral( ModemState state )
    {
        this.m_state = state;
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

    protected void switchNetwork()
    {
        setNetwork( getNetwork() );
    }

    public synchronized void destroy()
    {
        setNetwork( null );
    }

    @Override
    public void receiveSameDimension( @Nonnull Packet packet, double distance )
    {
        if( packet.getSender() == this || !m_state.isOpen( packet.getChannel() ) ) return;

        synchronized( this )
        {
            for( IComputerAccess computer : m_computers )
            {
                computer.queueEvent( "modem_message", new Object[]{
                    computer.getAttachmentName(), packet.getChannel(), packet.getReplyChannel(), packet.getPayload(), distance
                } );
            }
        }
    }

    @Override
    public void receiveDifferentDimension( @Nonnull Packet packet )
    {
        if( packet.getSender() == this || !m_state.isOpen( packet.getChannel() ) ) return;

        synchronized( this )
        {
            for( IComputerAccess computer : m_computers )
            {
                computer.queueEvent( "modem_message", new Object[]{
                    computer.getAttachmentName(), packet.getChannel(), packet.getReplyChannel(), packet.getPayload()
                } );
            }
        }
    }

    protected abstract IPacketNetwork getNetwork();

    // IPeripheral implementation

    @Nonnull
    @Override
    public String getType()
    {
        return "modem";
    }

    @Nonnull
    @Override
    public String[] getMethodNames()
    {
        return new String[]{
            "open",
            "isOpen",
            "close",
            "closeAll",
            "transmit",
            "isWireless",
        };
    }

    private static int parseChannel( Object[] arguments, int index ) throws LuaException
    {
        int channel = getInt( arguments, index );
        if( channel < 0 || channel > 65535 )
        {
            throw new LuaException( "Expected number in range 0-65535" );
        }
        return channel;
    }

    @Override
    public Object[] callMethod( @Nonnull IComputerAccess computer, @Nonnull ILuaContext context, int method, @Nonnull Object[] arguments ) throws LuaException, InterruptedException
    {
        switch( method )
        {
            case 0:
            {
                // open
                int channel = parseChannel( arguments, 0 );
                m_state.open( channel );
                return null;
            }
            case 1:
            {
                // isOpen
                int channel = parseChannel( arguments, 0 );
                return new Object[]{ m_state.isOpen( channel ) };
            }
            case 2:
            {
                // close
                int channel = parseChannel( arguments, 0 );
                m_state.close( channel );
                return null;
            }
            case 3:
            {
                // closeAll
                m_state.closeAll();
                return null;
            }
            case 4:
            {
                // transmit
                int channel = parseChannel( arguments, 0 );
                int replyChannel = parseChannel( arguments, 1 );
                Object payload = arguments.length > 2 ? arguments[2] : null;
                synchronized( this )
                {
                    World world = getWorld();
                    Vec3d position = getPosition();
                    if( world != null && position != null && m_network != null )
                    {
                        Packet packet = new Packet( channel, replyChannel, payload, this );
                        if( isInterdimensional() )
                        {
                            m_network.transmitInterdimensional( packet );
                        }
                        else
                        {
                            m_network.transmitSameDimension( packet, getRange() );
                        }
                    }
                }
                return null;
            }
            case 5:
            {
                // isWireless
                IPacketNetwork network = m_network;
                return new Object[]{ network != null && network.isWireless() };
            }
            default:
            {
                return null;
            }
        }
    }

    @Override
    public synchronized void attach( @Nonnull IComputerAccess computer )
    {
        m_computers.add( computer );
        setNetwork( getNetwork() );
    }

    @Override
    public synchronized void detach( @Nonnull IComputerAccess computer )
    {
        m_computers.remove( computer );
        if( m_computers.isEmpty() ) setNetwork( null );
    }

    @Nonnull
    @Override
    public synchronized String getSenderID()
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
