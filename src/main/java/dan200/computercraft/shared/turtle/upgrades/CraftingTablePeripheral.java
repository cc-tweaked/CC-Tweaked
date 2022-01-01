/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
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
import java.util.Optional;

/**
 * The workbench peripheral allows you to craft items within the turtle's inventory.
 *
 * @cc.module workbench
 * @hidden
 * @cc.see turtle.craft This uses the {@link CraftingTablePeripheral} peripheral to craft items.
 */
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

    @LuaFunction
    public final MethodResult craft( Optional<Integer> count ) throws LuaException
    {
        int limit = count.orElse( 64 );
        if( limit < 0 || limit > 64 ) throw new LuaException( "Crafting count " + limit + " out of range" );
        return turtle.executeCommand( new TurtleCraftCommand( limit ) );
    }

    @Override
    public boolean equals( IPeripheral other )
    {
        return other instanceof CraftingTablePeripheral;
    }

    @Nonnull
    @Override
    public Object getTarget()
    {
        return turtle;
    }
}
