/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.apis;

import com.google.common.collect.ImmutableMap;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.shared.computer.blocks.TileCommandComputer;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandManager;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

import static dan200.computercraft.core.apis.ArgumentHelper.getInt;
import static dan200.computercraft.core.apis.ArgumentHelper.getString;

public class CommandAPI implements ILuaAPI
{
    private TileCommandComputer m_computer;

    public CommandAPI( TileCommandComputer computer )
    {
        m_computer = computer;
    }

    // ILuaAPI implementation

    @Override
    public String[] getNames()
    {
        return new String[] {
            "commands"
        };
    }

    @Nonnull
    @Override
    public String[] getMethodNames()
    {
        return new String[] {
            "exec",
            "execAsync",
            "list",
            "getBlockPosition",
            "getBlockInfos",
            "getBlockInfo"
        };
    }

    private Map<Object, Object> createOutput( String output )
    {
        Map<Object, Object> result = new HashMap<>( 1 );
        result.put( 1, output );
        return result;
    }

    private Object[] doCommand( String command )
    {
        MinecraftServer server = m_computer.getWorld().getMinecraftServer();
        if( server != null && server.isCommandBlockEnabled() )
        {
            ICommandManager commandManager = server.getCommandManager();
            try
            {
                TileCommandComputer.CommandSender sender = m_computer.getCommandSender();
                sender.clearOutput();

                int result = commandManager.executeCommand( sender, command );
                return new Object[] { (result > 0), sender.copyOutput() };
            }
            catch( Throwable t )
            {
                if( ComputerCraft.logPeripheralErrors )
                {
                    ComputerCraft.log.error( "Error running command.", t );
                }
                return new Object[] { false, createOutput( "Java Exception Thrown: " + t.toString() ) };
            }
        }
        else
        {
            return new Object[] { false, createOutput( "Command blocks disabled by server" ) };
        }
    }

    private Object getBlockInfo( World world, BlockPos pos )
    {
        // Get the details of the block
        IBlockState state = world.getBlockState( pos );
        Block block = state.getBlock();
        String name = Block.REGISTRY.getNameForObject( block ).toString();
        int metadata = block.getMetaFromState( state );

        Map<Object, Object> table = new HashMap<>();
        table.put( "name", name );
        table.put( "metadata", metadata );

        Map<Object, Object> stateTable = new HashMap<>();
        for( ImmutableMap.Entry<IProperty<?>, Comparable<?>> entry : state.getActualState( world, pos ).getProperties().entrySet() )
        {
            String propertyName = entry.getKey().getName();
            Object value = entry.getValue();
            if( value instanceof String || value instanceof Number || value instanceof Boolean )
            {
                stateTable.put( propertyName, value );
            }
            else
            {
                stateTable.put( propertyName, value.toString() );
            }
        }
        table.put( "state", stateTable );
        // TODO: NBT data?
        return table;
    }

    @Override
    public Object[] callMethod( @Nonnull ILuaContext context, int method, @Nonnull Object[] arguments ) throws LuaException, InterruptedException
    {
        switch( method )
        {
            case 0:
            {
                // exec
                final String command = getString( arguments, 0 );
                return context.executeMainThreadTask( () -> doCommand( command ) );
            }
            case 1:
            {
                // execAsync
                final String command = getString( arguments, 0 );
                long taskID = context.issueMainThreadTask( () -> doCommand( command ) );
                return new Object[] { taskID };
            }
            case 2:
            {
                // list
                return context.executeMainThreadTask( () ->
                {
                    int i = 1;
                    Map<Object, Object> result = new HashMap<>();
                    MinecraftServer server = m_computer.getWorld().getMinecraftServer();
                    if( server != null )
                    {
                        ICommandManager commandManager = server.getCommandManager();
                        ICommandSender commmandSender = m_computer.getCommandSender();
                        Map<String, ICommand> commands = commandManager.getCommands();
                        for( Map.Entry<String, ICommand> entry : commands.entrySet() )
                        {
                            String name = entry.getKey();
                            ICommand command = entry.getValue();
                            try
                            {
                                if( command.checkPermission( server, commmandSender ) )
                                {
                                    result.put( i++, name );
                                }
                            }
                            catch( Throwable t )
                            {
                                // Ignore buggy command
                                if( ComputerCraft.logPeripheralErrors )
                                {
                                    ComputerCraft.log.error( "Error checking permissions of command.", t );
                                }
                            }
                        }
                    }
                    return new Object[] { result };
                } );
            }
            case 3:
            {
                // getBlockPosition
                // This is probably safe to do on the Lua thread. Probably.
                BlockPos pos = m_computer.getPos();
                return new Object[] { pos.getX(), pos.getY(), pos.getZ() };
            }
            case 4:
            {
                // getBlockInfos
                final int minx = getInt( arguments, 0 );
                final int miny = getInt( arguments, 1 );
                final int minz = getInt( arguments, 2 );
                final int maxx = getInt( arguments, 3 );
                final int maxy = getInt( arguments, 4 );
                final int maxz = getInt( arguments, 5 );
                return context.executeMainThreadTask( () ->
                {
                    // Get the details of the block
                    World world = m_computer.getWorld();
                    BlockPos min = new BlockPos(
                        Math.min( minx, maxx ),
                        Math.min( miny, maxy ),
                        Math.min( minz, maxz )
                    );
                    BlockPos max = new BlockPos(
                        Math.max( minx, maxx ),
                        Math.max( miny, maxy ),
                        Math.max( minz, maxz )
                    );
                    if( !WorldUtil.isBlockInWorld( world, min ) || !WorldUtil.isBlockInWorld( world, max ) )
                    {
                        throw new LuaException( "Co-ordinates out or range" );
                    }
                    if( (max.getX() - min.getX() + 1) * (max.getY() - min.getY() + 1) * (max.getZ() - min.getZ() + 1) > 4096 )
                    {
                        throw new LuaException( "Too many blocks" );
                    }
                    int i = 1;
                    Map<Object, Object> results = new HashMap<>();
                    for( int y = min.getY(); y <= max.getY(); y++ )
                    {
                        for( int z = min.getZ(); z <= max.getZ(); z++ )
                        {
                            for( int x = min.getX(); x <= max.getX(); x++ )
                            {
                                BlockPos pos = new BlockPos( x, y, z );
                                results.put( i++, getBlockInfo( world, pos ) );
                            }
                        }
                    }
                    return new Object[] { results };
                } );
            }
            case 5:
            {
                // getBlockInfo
                final int x = getInt( arguments, 0 );
                final int y = getInt( arguments, 1 );
                final int z = getInt( arguments, 2 );
                return context.executeMainThreadTask( () ->
                {
                    // Get the details of the block
                    World world = m_computer.getWorld();
                    BlockPos position = new BlockPos( x, y, z );
                    if( WorldUtil.isBlockInWorld( world, position ) )
                    {
                        return new Object[] { getBlockInfo( world, position ) };
                    }
                    else
                    {
                        throw new LuaException( "co-ordinates out or range" );
                    }
                } );
            }
            default:
            {
                return null;
            }
        }
    }
}
