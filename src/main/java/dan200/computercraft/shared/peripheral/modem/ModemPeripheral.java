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
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

import static dan200.computercraft.core.apis.ArgumentHelper.getInt;

public abstract class ModemPeripheral
    implements IPeripheral, IPacketSender, IPacketReceiver
{
    private IPacketNetwork m_network;
    private IComputerAccess m_computer;
    private final TIntSet m_channels;

    private boolean m_open;
    private boolean m_changed;

    public ModemPeripheral()
    {
        m_network = null;
        m_computer = null;
        m_channels = new TIntHashSet();
        m_open = false;
        m_changed = true;
    }

    private synchronized void setNetwork( IPacketNetwork network )
    {
        if( m_network != network )
        {
            // Leave old network
            if( m_network != null )
            {
                m_network.removeReceiver( this );
            }

            // Set new network
            m_network = network;

            // Join new network
            if( m_network != null )
            {
                m_network.addReceiver( this );
            }
        }
    }

    protected void switchNetwork()
    {
        setNetwork( getNetwork() );
    }

    public synchronized void destroy()
    {
        setNetwork( null );
        m_channels.clear();
        m_open = false;
    }
    
    public boolean pollChanged()
    {
        if( m_changed )
        {
            m_changed = false;
            return true;
        }
        return false;
    }

    public boolean isActive()
    {
        return m_computer != null && m_open;
    }

    @Override
    public void receiveSameDimension( @Nonnull Packet packet, double distance )
    {
        if( packet.getSender() == this ) return;

        synchronized (m_channels)
        {
            if( m_computer != null && m_channels.contains( packet.getChannel() ) )
            {
                m_computer.queueEvent( "modem_message", new Object[] {
                    m_computer.getAttachmentName(), packet.getChannel(), packet.getReplyChannel(), packet.getPayload(), distance
                } );
            }
        }
    }

    @Override
    public void receiveDifferentDimension( @Nonnull Packet packet )
    {
        if( packet.getSender() == this ) return;

        synchronized (m_channels)
        {
            if( m_computer != null && m_channels.contains( packet.getChannel() ) )
            {
                m_computer.queueEvent( "modem_message", new Object[] {
                    m_computer.getAttachmentName(), packet.getChannel(), packet.getReplyChannel(), packet.getPayload()
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
        return new String[] {
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
                synchronized( this )
                {
                    if( !m_channels.contains( channel ) )
                    {
                        if( m_channels.size() >= 128 )
                        {
                            throw new LuaException( "Too many open channels" );
                        }

                        m_channels.add( channel );
                        if( !m_open )
                        {
                            m_open = true;
                            m_changed = true;
                        }
                    }
                }
                return null;
            }
            case 1:
            {
                // isOpen
                int channel = parseChannel( arguments, 0 );
                synchronized( this )
                {
                    boolean open = m_channels.contains( channel );
                    return new Object[] { open };
                }
            }
            case 2:
            {
                // close
                int channel = parseChannel( arguments, 0 );
                synchronized( this )
                {
                    if( m_channels.remove( channel ) )
                    {
                        if( m_channels.size() == 0 )
                        {
                            m_open = false;
                            m_changed = true;
                        }
                    }
                }
                return null;
            }
            case 3:
            {
                // closeAll
                synchronized( this )
                {
                    if( m_channels.size() > 0 )
                    {
                        m_channels.clear();
                        
                        if( m_open )
                        {
                            m_open = false;
                            m_changed = true;
                        }
                    }
                }
                return null;
            }
            case 4:
            {
                // transmit
                int channel = parseChannel( arguments, 0 );
                int replyChannel = parseChannel( arguments, 1 );
                Object payload = (arguments.length >= 3) ? arguments[2] : null;
                synchronized( this )
                {
                    World world = getWorld();
                    Vec3d position = getPosition();
                    if( world != null && position != null && m_network != null)
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
                synchronized( this )
                {
                    if( m_network != null )
                    {
                        return new Object[] { m_network.isWireless() };
                    }
                }
                return new Object[] { false };
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
        m_computer = computer;
        setNetwork( getNetwork() );
        m_open = !m_channels.isEmpty();
    }
    
    @Override
    public synchronized void detach( @Nonnull IComputerAccess computer )
    {
        if( m_network != null )
        {
            m_network.removeReceiver( this );
            m_channels.clear();
            m_network = null;
        }

        m_computer = null;
            
        if( m_open )
        {
            m_open = false;        
            m_changed = true;
        }
    }

    public IComputerAccess getComputer()
    {
        return m_computer;
    }

    @Nonnull
    @Override
    public String getSenderID()
    {
        if( m_computer == null )
        {
            return "unknown";
        }
        else
        {
            return m_computer.getID() + "_" + m_computer.getAttachmentName();
        }
    }
}
