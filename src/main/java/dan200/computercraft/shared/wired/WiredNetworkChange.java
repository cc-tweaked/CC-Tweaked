/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.wired;

import dan200.computercraft.api.network.wired.IWiredNetworkChange;
import dan200.computercraft.api.peripheral.IPeripheral;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class WiredNetworkChange implements IWiredNetworkChange
{
    private static final WiredNetworkChange EMPTY = new WiredNetworkChange( Collections.emptyMap(), Collections.emptyMap() );

    private final Map<String, IPeripheral> removed;
    private final Map<String, IPeripheral> added;

    private WiredNetworkChange( Map<String, IPeripheral> removed, Map<String, IPeripheral> added )
    {
        this.removed = removed;
        this.added = added;
    }

    public static WiredNetworkChange changed( Map<String, IPeripheral> removed, Map<String, IPeripheral> added )
    {
        return new WiredNetworkChange( Collections.unmodifiableMap( removed ), Collections.unmodifiableMap( added ) );
    }

    public static WiredNetworkChange added( Map<String, IPeripheral> added )
    {
        return added.isEmpty() ? EMPTY : new WiredNetworkChange( Collections.emptyMap(), Collections.unmodifiableMap( added ) );
    }

    public static WiredNetworkChange removed( Map<String, IPeripheral> removed )
    {
        return removed.isEmpty() ? EMPTY : new WiredNetworkChange( Collections.unmodifiableMap( removed ), Collections.emptyMap() );
    }

    public static WiredNetworkChange changeOf( Map<String, IPeripheral> oldPeripherals, Map<String, IPeripheral> newPeripherals )
    {
        // Handle the trivial cases, where all peripherals have been added or removed.
        if( oldPeripherals.isEmpty() && newPeripherals.isEmpty() )
        {
            return EMPTY;
        }
        else if( oldPeripherals.isEmpty() )
        {
            return new WiredNetworkChange( Collections.emptyMap(), newPeripherals );
        }
        else if( newPeripherals.isEmpty() )
        {
            return new WiredNetworkChange( oldPeripherals, Collections.emptyMap() );
        }

        Map<String, IPeripheral> added = new HashMap<>( newPeripherals );
        Map<String, IPeripheral> removed = new HashMap<>();

        for( Map.Entry<String, IPeripheral> entry : oldPeripherals.entrySet() )
        {
            String oldKey = entry.getKey();
            IPeripheral oldValue = entry.getValue();
            if( newPeripherals.containsKey( oldKey ) )
            {
                IPeripheral rightValue = added.get( oldKey );
                if( oldValue.equals( rightValue ) )
                {
                    added.remove( oldKey );
                }
                else
                {
                    removed.put( oldKey, oldValue );
                }
            }
            else
            {
                removed.put( oldKey, oldValue );
            }
        }

        return changed( removed, added );
    }

    @Nonnull
    @Override
    public Map<String, IPeripheral> peripheralsAdded()
    {
        return added;
    }

    @Nonnull
    @Override
    public Map<String, IPeripheral> peripheralsRemoved()
    {
        return removed;
    }

    public boolean isEmpty()
    {
        return added.isEmpty() && removed.isEmpty();
    }

    void broadcast( Iterable<WiredNode> nodes )
    {
        if( !isEmpty() )
        {
            for( WiredNode node : nodes ) node.element.networkChanged( this );
        }
    }

    void broadcast( WiredNode node )
    {
        if( !isEmpty() )
        {
            node.element.networkChanged( this );
        }
    }
}
