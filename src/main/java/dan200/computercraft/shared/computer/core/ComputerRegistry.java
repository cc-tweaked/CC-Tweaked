/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.core;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ComputerRegistry<T extends IComputer>
{
    private final Map<Integer, T> computers = new HashMap<>();
    private int nextUnusedInstanceID;
    private int sessionID;

    protected ComputerRegistry()
    {
        reset();
    }

    public int getSessionID()
    {
        return sessionID;
    }

    public int getUnusedInstanceID()
    {
        return nextUnusedInstanceID++;
    }

    public Collection<T> getComputers()
    {
        return computers.values();
    }

    public T get( int instanceID )
    {
        if( instanceID >= 0 )
        {
            if( computers.containsKey( instanceID ) )
            {
                return computers.get( instanceID );
            }
        }
        return null;
    }

    public boolean contains( int instanceID )
    {
        return computers.containsKey( instanceID );
    }

    public void add( int instanceID, T computer )
    {
        if( computers.containsKey( instanceID ) )
        {
            remove( instanceID );
        }
        computers.put( instanceID, computer );
        nextUnusedInstanceID = Math.max( nextUnusedInstanceID, instanceID + 1 );
    }

    public void remove( int instanceID )
    {
        computers.remove( instanceID );
    }

    public void reset()
    {
        computers.clear();
        nextUnusedInstanceID = 0;
        sessionID = new Random().nextInt();
    }
}
