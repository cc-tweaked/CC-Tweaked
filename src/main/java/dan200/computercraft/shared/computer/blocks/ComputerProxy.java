/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.blocks;

import dan200.computercraft.shared.computer.core.IComputer;
import dan200.computercraft.shared.computer.core.ServerComputer;

import java.util.function.Supplier;

/**
 * A proxy object for computer objects, delegating to {@link IComputer} or {@link TileComputer} where appropriate.
 */
public final class ComputerProxy
{
    private final Supplier<TileComputerBase> get;

    public ComputerProxy( Supplier<TileComputerBase> get )
    {
        this.get = get;
    }

    TileComputerBase getTile()
    {
        return get.get();
    }

    public void turnOn()
    {
        TileComputerBase tile = getTile();
        ServerComputer computer = tile.getServerComputer();
        if( computer == null )
        {
            tile.startOn = true;
        }
        else
        {
            computer.turnOn();
        }
    }

    public void shutdown()
    {
        TileComputerBase tile = getTile();
        ServerComputer computer = tile.getServerComputer();
        if( computer == null )
        {
            tile.startOn = false;
        }
        else
        {
            computer.shutdown();
        }
    }

    public void reboot()
    {
        TileComputerBase tile = getTile();
        ServerComputer computer = tile.getServerComputer();
        if( computer == null )
        {
            tile.startOn = true;
        }
        else
        {
            computer.reboot();
        }
    }

    public int assignID()
    {
        TileComputerBase tile = getTile();
        ServerComputer computer = tile.getServerComputer();
        return computer == null ? tile.getComputerID() : computer.getID();
    }

    public boolean isOn()
    {
        ServerComputer computer = getTile().getServerComputer();
        return computer != null && computer.isOn();
    }

    public String getLabel()
    {
        TileComputerBase tile = getTile();
        ServerComputer computer = tile.getServerComputer();
        return computer == null ? tile.getLabel() : computer.getLabel();
    }
}
