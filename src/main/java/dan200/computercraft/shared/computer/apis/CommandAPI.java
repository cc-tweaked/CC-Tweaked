/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.apis;

import com.google.common.collect.ImmutableMap;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.shared.computer.blocks.TileCommandComputer;
import dan200.computercraft.shared.util.NBTUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.Collections;
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

    private static Map<Object, Object> createOutput( String output )
    {
        return Collections.singletonMap( 1, output );
    }

    private Object[] doCommand( String command )
    {
        MinecraftServer server = m_computer.getWorld().getServer();
        if( server == null || !server.areCommandBlocksEnabled() )
        {
            return new Object[] { false, createOutput( "Command blocks disabled by server" ) };
        }

        CommandManager commandManager = server.getCommandManager();
        TileCommandComputer.CommandReceiver receiver = m_computer.getReceiver();
        try
        {
            receiver.clearOutput();
            int result = commandManager.execute( m_computer.getSource(), command );
            return new Object[] { result > 0, receiver.copyOutput() };
        }
        catch( Throwable t )
        {
            if( ComputerCraft.logPeripheralErrors ) ComputerCraft.log.error( "Error running command.", t );
            return new Object[] { false, createOutput( "Java Exception Thrown: " + t ) };
        }
    }

    private static Object getBlockInfo( World world, BlockPos pos )
    {
        // Get the details of the block
        BlockState state = world.getBlockState( pos );
        Block block = state.getBlock();

        Map<Object, Object> table = new HashMap<>();
        table.put( "name", Registry.BLOCK.getId( block ).toString() );

        Map<Object, Object> stateTable = new HashMap<>();
        for( ImmutableMap.Entry<Property<?>, Comparable<?>> entry : state.getEntries().entrySet() )
        {
            Property<?> property = entry.getKey();
            stateTable.put( property.getName(), getPropertyValue( property, entry.getValue() ) );
        }
        table.put( "state", stateTable );

        BlockEntity tile = world.getBlockEntity( pos );
        if( tile != null ) table.put( "nbt", NBTUtil.toLua( tile.toTag( new CompoundTag() ) ) );

        return table;
    }

    @SuppressWarnings( { "unchecked", "rawtypes" } )
    private static Object getPropertyValue( Property property, Comparable value )
    {
        if( value instanceof String || value instanceof Number || value instanceof Boolean ) return value;
        return property.getValueAsString( value );
    }

    @Override
    public Object[] callMethod( @Nonnull ILuaContext context, int method, @Nonnull Object[] arguments ) throws LuaException, InterruptedException
    {
        switch( method )
        {
            case 0: // exec
            {
                final String command = getString( arguments, 0 );
                return context.executeMainThreadTask( () -> doCommand( command ) );
            }
            case 1: // execAsync
            {
                final String command = getString( arguments, 0 );
                long taskID = context.issueMainThreadTask( () -> doCommand( command ) );
                return new Object[] { taskID };
            }
            case 2:
                // list
                return context.executeMainThreadTask( () ->
                {
                    MinecraftServer server = m_computer.getWorld().getServer();

                    if( server == null ) return new Object[] { Collections.emptyMap() };
                    CommandNode<ServerCommandSource> node = server.getCommandManager().getDispatcher().getRoot();
                    for( int j = 0; j < arguments.length; j++ )
                    {
                        String name = getString( arguments, j );
                        node = node.getChild( name );
                        if( !(node instanceof LiteralCommandNode) ) return new Object[] { Collections.emptyMap() };
                    }

                    int i = 1;
                    Map<Object, Object> result = new HashMap<>();
                    for( CommandNode<?> child : node.getChildren() )
                    {
                        if( child instanceof LiteralCommandNode<?> ) result.put( i++, child.getName() );
                    }
                    return new Object[] { result };
                } );
            case 3: // getBlockPosition
            {
                // This is probably safe to do on the Lua thread. Probably.
                BlockPos pos = m_computer.getPos();
                return new Object[] { pos.getX(), pos.getY(), pos.getZ() };
            }
            case 4:
            {
                // getBlockInfos
                final int minX = getInt( arguments, 0 );
                final int minY = getInt( arguments, 1 );
                final int minZ = getInt( arguments, 2 );
                final int maxX = getInt( arguments, 3 );
                final int maxY = getInt( arguments, 4 );
                final int maxZ = getInt( arguments, 5 );
                return context.executeMainThreadTask( () ->
                {
                    // Get the details of the block
                    World world = m_computer.getWorld();
                    BlockPos min = new BlockPos(
                        Math.min( minX, maxX ),
                        Math.min( minY, maxY ),
                        Math.min( minZ, maxZ )
                    );
                    BlockPos max = new BlockPos(
                        Math.max( minX, maxX ),
                        Math.max( minY, maxY ),
                        Math.max( minZ, maxZ )
                    );
                    if( !World.isValid( min ) || !World.isValid( max ) )
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
                    if( World.isValid( position ) )
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
