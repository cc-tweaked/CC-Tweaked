/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.commandblock;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.block.entity.CommandBlockBlockEntity;

import javax.annotation.Nonnull;

import static dan200.computercraft.core.apis.ArgumentHelper.getString;

public class CommandBlockPeripheral implements IPeripheral
{
    private final CommandBlockBlockEntity m_commandBlock;

    public CommandBlockPeripheral( CommandBlockBlockEntity commandBlock )
    {
        m_commandBlock = commandBlock;
    }

    // IPeripheral methods

    @Nonnull
    @Override
    public String getType()
    {
        return "command";
    }

    @Nonnull
    @Override
    public String[] getMethodNames()
    {
        return new String[] {
            "getCommand",
            "setCommand",
            "runCommand",
        };
    }

    @Override
    public Object[] callMethod( @Nonnull IComputerAccess computer, @Nonnull ILuaContext context, int method, @Nonnull final Object[] arguments ) throws LuaException, InterruptedException
    {
        switch( method )
        {
            case 0: // getCommand
                return context.executeMainThreadTask( () -> new Object[] {
                    m_commandBlock.getCommandExecutor().getCommand()
                } );
            case 1:
            {
                // setCommand
                final String command = getString( arguments, 0 );
                context.issueMainThreadTask( () ->
                {
                    m_commandBlock.getCommandExecutor().setCommand( command );
                    m_commandBlock.getCommandExecutor().markDirty();
                    return null;
                } );
                return null;
            }
            case 2: // runCommand
                return context.executeMainThreadTask( () ->
                {
                    m_commandBlock.getCommandExecutor().execute( m_commandBlock.getWorld() );
                    int result = m_commandBlock.getCommandExecutor().getSuccessCount();
                    if( result > 0 )
                    {
                        return new Object[] { true };
                    }
                    else
                    {
                        return new Object[] { false, "Command failed" };
                    }
                } );
        }
        return null;
    }

    @Override
    public boolean equals( IPeripheral other )
    {
        return other != null && other.getClass() == getClass();
    }
}
