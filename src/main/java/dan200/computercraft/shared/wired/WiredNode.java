/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.wired;

import dan200.computercraft.api.network.IPacketReceiver;
import dan200.computercraft.api.network.Packet;
import dan200.computercraft.api.network.wired.IWiredElement;
import dan200.computercraft.api.network.wired.IWiredNetwork;
import dan200.computercraft.api.network.wired.IWiredNode;
import dan200.computercraft.api.network.wired.IWiredSender;
import dan200.computercraft.api.peripheral.IPeripheral;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.locks.Lock;

public final class WiredNode implements IWiredNode
{
    private Set<IPacketReceiver> receivers;

    final IWiredElement element;
    Map<String, IPeripheral> peripherals = Collections.emptyMap();

    final HashSet<WiredNode> neighbours = new HashSet<>();
    volatile WiredNetwork network;

    public WiredNode( IWiredElement element )
    {
        this.element = element;
        network = new WiredNetwork( this );
    }

    @Override
    public synchronized void addReceiver( @Nonnull IPacketReceiver receiver )
    {
        if( receivers == null ) receivers = new HashSet<>();
        receivers.add( receiver );
    }

    @Override
    public synchronized void removeReceiver( @Nonnull IPacketReceiver receiver )
    {
        if( receivers != null ) receivers.remove( receiver );
    }

    synchronized void tryTransmit( Packet packet, double packetDistance, boolean packetInterdimensional, double range, boolean interdimensional )
    {
        if( receivers == null ) return;

        for( IPacketReceiver receiver : receivers )
        {
            if( !packetInterdimensional )
            {
                double receiveRange = Math.max( range, receiver.getRange() ); // Ensure range is symmetrical
                if( interdimensional || receiver.isInterdimensional() || packetDistance < receiveRange )
                {
                    receiver.receiveSameDimension( packet, packetDistance + element.getPosition().distanceTo( receiver.getPosition() ) );
                }
            }
            else
            {
                if( interdimensional || receiver.isInterdimensional() )
                {
                    receiver.receiveDifferentDimension( packet );
                }
            }
        }
    }

    @Override
    public boolean isWireless()
    {
        return false;
    }

    @Override
    public void transmitSameDimension( @Nonnull Packet packet, double range )
    {
        Objects.requireNonNull( packet, "packet cannot be null" );
        if( !(packet.sender() instanceof IWiredSender) || ((IWiredSender) packet.sender()).getNode() != this )
        {
            throw new IllegalArgumentException( "Sender is not in the network" );
        }

        acquireReadLock();
        try
        {
            WiredNetwork.transmitPacket( this, packet, range, false );
        }
        finally
        {
            network.lock.readLock().unlock();
        }
    }

    @Override
    public void transmitInterdimensional( @Nonnull Packet packet )
    {
        Objects.requireNonNull( packet, "packet cannot be null" );
        if( !(packet.sender() instanceof IWiredSender) || ((IWiredSender) packet.sender()).getNode() != this )
        {
            throw new IllegalArgumentException( "Sender is not in the network" );
        }

        acquireReadLock();
        try
        {
            WiredNetwork.transmitPacket( this, packet, 0, true );
        }
        finally
        {
            network.lock.readLock().unlock();
        }
    }

    @Nonnull
    @Override
    public IWiredElement getElement()
    {
        return element;
    }

    @Nonnull
    @Override
    public IWiredNetwork getNetwork()
    {
        return network;
    }

    @Override
    public String toString()
    {
        return "WiredNode{@" + element.getPosition() + " (" + element.getClass().getSimpleName() + ")}";
    }

    private void acquireReadLock()
    {
        WiredNetwork currentNetwork = network;
        while( true )
        {
            Lock lock = currentNetwork.lock.readLock();
            lock.lock();
            if( currentNetwork == network ) return;


            lock.unlock();
        }
    }
}
