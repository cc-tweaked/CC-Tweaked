/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.upgrades;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.shared.turtle.core.TurtleCraftCommand;

import javax.annotation.Nonnull;

import static dan200.computercraft.api.lua.ArgumentHelper.optInt;

public class CraftingTablePeripheral implements IPeripheral
{
    private final ITurtleAccess turtle;

    public CraftingTablePeripheral( ITurtleAccess turtle )
    {
        this.turtle = turtle;
    }

    @Nonnull
    @Override
    public String getType()
    {
        return "workbench";
    }

    private static int parseCount( Object[] arguments ) throws LuaException
    {
        int count = optInt( arguments, 0, 64 );
        if( count < 0 || count > 64 ) throw new LuaException( "Crafting count " + count + " out of range" );
        return count;
    }

    @LuaFunction
    public final MethodResult craft( Object[] args ) throws LuaException
    {
        int limit = parseCount( args );
        return turtle.executeCommand( new TurtleCraftCommand( limit ) );
    }

    @Override
    public boolean equals( IPeripheral other )
    {
        return other instanceof CraftingTablePeripheral;
    }
}
