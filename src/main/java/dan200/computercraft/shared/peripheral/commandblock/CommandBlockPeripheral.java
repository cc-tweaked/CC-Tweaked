/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.commandblock;

import dan200.computercraft.api.lua.ICallContext;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.tileentity.TileEntityCommandBlock;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static dan200.computercraft.core.apis.ArgumentHelper.getString;

public class CommandBlockPeripheral implements IPeripheral
{
    private final TileEntityCommandBlock m_commandBlock;

    public CommandBlockPeripheral( TileEntityCommandBlock commandBlock )
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

    @Nonnull
    @Override
    public MethodResult callMethod( @Nonnull IComputerAccess computer, @Nonnull ICallContext context, int method, @Nonnull final Object[] arguments ) throws LuaException
    {
        switch( method )
        {
            case 0:
            {
                // getCommand
                return MethodResult.onMainThread( () ->
                    MethodResult.of( m_commandBlock.getCommandBlockLogic().getCommand() )
                );
            }
            case 1:
            {
                // setCommand
                final String command = getString( arguments, 0 );
                context.issueMainThreadTask( () ->
                {
                    BlockPos pos = m_commandBlock.getPos();
                    m_commandBlock.getCommandBlockLogic().setCommand( command );
                    m_commandBlock.getWorld().markBlockRangeForRenderUpdate( pos, pos );
                    return null;
                } );
                return MethodResult.empty();
            }
            case 2:
            {
                // runCommand
                return MethodResult.onMainThread( () ->
                {
                    m_commandBlock.getCommandBlockLogic().trigger( m_commandBlock.getWorld() );
                    int result = m_commandBlock.getCommandBlockLogic().getSuccessCount();
                    if( result > 0 )
                    {
                        return MethodResult.of( true );
                    }
                    else
                    {
                        return MethodResult.of( false, "Command failed" );
                    }
                } );
            }
        }
        return MethodResult.empty();
    }

    @Nullable
    @Override
    @Deprecated
    public Object[] callMethod( @Nonnull IComputerAccess access, @Nonnull ILuaContext context, int method, @Nonnull Object[] arguments ) throws LuaException, InterruptedException
    {
        return callMethod( access, (ICallContext) context, method, arguments ).evaluate( context );
    }

    @Override
    public boolean equals( IPeripheral other )
    {
        return (other != null && other.getClass() == this.getClass());
    }
}
