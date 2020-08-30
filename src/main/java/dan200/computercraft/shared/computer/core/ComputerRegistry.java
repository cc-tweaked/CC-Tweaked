/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.core;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ComputerRegistry<T extends IComputer>
{
    private Map<Integer, T> m_computers;
    private int m_nextUnusedInstanceID;
    private int m_sessionID;

    protected ComputerRegistry()
    {
        m_computers = new HashMap<>();
        reset();
    }

    public int getSessionID()
    {
        return m_sessionID;
    }

    public int getUnusedInstanceID()
    {
        return m_nextUnusedInstanceID++;
    }

    public Collection<T> getComputers()
    {
        return m_computers.values();
    }

    public T get( int instanceID )
    {
        if( instanceID >= 0 )
        {
            if( m_computers.containsKey( instanceID ) )
            {
                return m_computers.get( instanceID );
            }
        }
        return null;
    }

    public boolean contains( int instanceID )
    {
        return m_computers.containsKey( instanceID );
    }

    public void add( int instanceID, T computer )
    {
        if( m_computers.containsKey( instanceID ) )
        {
            remove( instanceID );
        }
        m_computers.put( instanceID, computer );
        m_nextUnusedInstanceID = Math.max( m_nextUnusedInstanceID, instanceID + 1 );
    }

    public void remove( int instanceID )
    {
        m_computers.remove( instanceID );
    }

    public void reset()
    {
        m_computers.clear();
        m_nextUnusedInstanceID = 0;
        m_sessionID = new Random().nextInt();
    }
}
