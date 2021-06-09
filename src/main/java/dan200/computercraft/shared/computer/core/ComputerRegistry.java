/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.core;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ComputerRegistry<T extends IComputer>
{
    private final Map<Integer, T> computers;
    private int nextUnusedInstanceID;
    private int sessionID;

    protected ComputerRegistry()
    {
        this.computers = new HashMap<>();
        this.reset();
    }

    public void reset()
    {
        this.computers.clear();
        this.nextUnusedInstanceID = 0;
        this.sessionID = new Random().nextInt();
    }

    public int getSessionID()
    {
        return this.sessionID;
    }

    public int getUnusedInstanceID()
    {
        return this.nextUnusedInstanceID++;
    }

    public Collection<T> getComputers()
    {
        return this.computers.values();
    }

    public T get( int instanceID )
    {
        if( instanceID >= 0 )
        {
            if( this.computers.containsKey( instanceID ) )
            {
                return this.computers.get( instanceID );
            }
        }
        return null;
    }

    public boolean contains( int instanceID )
    {
        return this.computers.containsKey( instanceID );
    }

    public void add( int instanceID, T computer )
    {
        if( this.computers.containsKey( instanceID ) )
        {
            this.remove( instanceID );
        }
        this.computers.put( instanceID, computer );
        this.nextUnusedInstanceID = Math.max( this.nextUnusedInstanceID, instanceID + 1 );
    }

    public void remove( int instanceID )
    {
        this.computers.remove( instanceID );
    }
}
