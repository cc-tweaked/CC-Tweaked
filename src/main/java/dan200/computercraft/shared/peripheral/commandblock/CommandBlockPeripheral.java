/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.commandblock;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.tileentity.CommandBlockTileEntity;

import javax.annotation.Nonnull;

public class CommandBlockPeripheral implements IPeripheral
{
    private final CommandBlockTileEntity commandBlock;

    public CommandBlockPeripheral( CommandBlockTileEntity commandBlock )
    {
        this.commandBlock = commandBlock;
    }

    @Nonnull
    @Override
    public String getType()
    {
        return "command";
    }

    @LuaFunction( mainThread = true )
    public final String getCommand()
    {
        return commandBlock.getCommandBlockLogic().getCommand();
    }

    @LuaFunction( mainThread = true )
    public final void setCommand( String command )
    {
        commandBlock.getCommandBlockLogic().setCommand( command );
        commandBlock.getCommandBlockLogic().updateCommand();
    }

    @LuaFunction( mainThread = true )
    public final Object runCommand()
    {
        commandBlock.getCommandBlockLogic().trigger( commandBlock.getWorld() );
        int result = commandBlock.getCommandBlockLogic().getSuccessCount();
        return result > 0 ? new Object[] { true } : new Object[] { false, "Command failed" };
    }

    @Override
    public boolean equals( IPeripheral other )
    {
        return other != null && other.getClass() == getClass();
    }
}
