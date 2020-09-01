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

/**
 * The modem peripheral allows you to send messages between computers.
 *
 * @cc.module modem
 */
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

    /**
     * Open a channel on a modem. A channel must be open in order to receive messages. Modems can have up to 128
     * channels open at one time.
     *
     * @param channel The channel to open. This must be a number between 0 and 65535.
     * @throws LuaException If the channel is out of range.
     * @throws LuaException If there are too many open channels.
     */
    @LuaFunction
    public final void open( int channel ) throws LuaException
    {
        m_state.open( parseChannel( channel ) );
    }

    /**
     * Check if a channel is open.
     *
     * @param channel The channel to check.
     * @return Whether the channel is open.
     * @throws LuaException If the channel is out of range.
     */
    @LuaFunction
    public final boolean isOpen( int channel ) throws LuaException
    {
        return m_state.isOpen( parseChannel( channel ) );
    }

    /**
     * Close an open channel, meaning it will no longer receive messages.
     *
     * @param channel The channel to close.
     * @throws LuaException If the channel is out of range.
     */
    @LuaFunction
    public final void close( int channel ) throws LuaException
    {
        m_state.close( parseChannel( channel ) );
    }

    /**
     * Close all open channels.
     */
    @LuaFunction
    public final void closeAll()
    {
        m_state.closeAll();
    }

    /**
     * Sends a modem message on a certain channel. Modems listening on the channel will queue a {@code modem_message}
     * event on adjacent computers.
     *
     * <blockquote><strong>Note:</strong> The channel does not need be open to send a message.</blockquote>
     *
     * @param channel      The channel to send messages on.
     * @param replyChannel The channel that responses to this message should be sent on.
     * @param payload      The object to send. This can be a string, number, or table.
     * @throws LuaException If the channel is out of range.
     */
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

    /**
     * Determine if this is a wired or wireless modem.
     *
     * Some methods (namely those dealing with wired networks and remote peripherals) are only available on wired
     * modems.
     *
     * @return {@code true} if this is a wireless modem.
     */
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
