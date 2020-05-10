/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.apis;

import com.google.common.collect.ImmutableMap;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.lua.*;
import dan200.computercraft.shared.computer.blocks.TileCommandComputer;
import dan200.computercraft.shared.util.NBTUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.state.IProperty;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

public class CommandAPI implements ILuaAPI
{
    private final TileCommandComputer computer;

    public CommandAPI( TileCommandComputer computer )
    {
        this.computer = computer;
    }

    @Override
    public String[] getNames()
    {
        return new String[] { "commands" };
    }

    private static Object createOutput( String output )
    {
        return new Object[] { output };
    }

    private Object[] doCommand( String command )
    {
        MinecraftServer server = computer.getWorld().getServer();
        if( server == null || !server.isCommandBlockEnabled() )
        {
            return new Object[] { false, createOutput( "Command blocks disabled by server" ) };
        }

        Commands commandManager = server.getCommandManager();
        TileCommandComputer.CommandReceiver receiver = computer.getReceiver();
        try
        {
            receiver.clearOutput();
            int result = commandManager.handleCommand( computer.getSource(), command );
            return new Object[] { result > 0, receiver.copyOutput(), result };
        }
        catch( Throwable t )
        {
            if( ComputerCraft.logPeripheralErrors ) ComputerCraft.log.error( "Error running command.", t );
            return new Object[] { false, createOutput( "Java Exception Thrown: " + t ) };
        }
    }

    private static Map<?, ?> getBlockInfo( World world, BlockPos pos )
    {
        // Get the details of the block
        BlockState state = world.getBlockState( pos );
        Block block = state.getBlock();

        Map<Object, Object> table = new HashMap<>();
        table.put( "name", ForgeRegistries.BLOCKS.getKey( block ).toString() );

        Map<Object, Object> stateTable = new HashMap<>();
        for( ImmutableMap.Entry<IProperty<?>, Comparable<?>> entry : state.getValues().entrySet() )
        {
            IProperty<?> property = entry.getKey();
            stateTable.put( property.getName(), getPropertyValue( property, entry.getValue() ) );
        }
        table.put( "state", stateTable );

        TileEntity tile = world.getTileEntity( pos );
        if( tile != null ) table.put( "nbt", NBTUtil.toLua( tile.write( new CompoundNBT() ) ) );

        return table;
    }

    @SuppressWarnings( { "unchecked", "rawtypes" } )
    private static Object getPropertyValue( IProperty property, Comparable value )
    {
        if( value instanceof String || value instanceof Number || value instanceof Boolean ) return value;
        return property.getName( value );
    }

    @LuaFunction( mainThread = true )
    public final Object[] exec( String command )
    {
        return doCommand( command );
    }

    @LuaFunction
    public final long execAsync( ILuaContext context, String command ) throws LuaException
    {
        return context.issueMainThreadTask( () -> doCommand( command ) );
    }

    @LuaFunction( mainThread = true )
    public final List<String> list( IArguments args ) throws LuaException
    {
        MinecraftServer server = computer.getWorld().getServer();

        if( server == null ) return Collections.emptyList();
        CommandNode<CommandSource> node = server.getCommandManager().getDispatcher().getRoot();
        for( int j = 0; j < args.count(); j++ )
        {
            String name = args.getString( j );
            node = node.getChild( name );
            if( !(node instanceof LiteralCommandNode) ) return Collections.emptyList();
        }

        List<String> result = new ArrayList<>();
        for( CommandNode<?> child : node.getChildren() )
        {
            if( child instanceof LiteralCommandNode<?> ) result.add( child.getName() );
        }
        return result;
    }

    @LuaFunction
    public final Object[] getBlockPosition()
    {
        // This is probably safe to do on the Lua thread. Probably.
        BlockPos pos = computer.getPos();
        return new Object[] { pos.getX(), pos.getY(), pos.getZ() };
    }

    @LuaFunction( mainThread = true )
    public final List<Map<?, ?>> getBlockInfos( int minX, int minY, int minZ, int maxX, int maxY, int maxZ ) throws LuaException
    {
        // Get the details of the block
        World world = computer.getWorld();
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
            throw new LuaException( "Co-ordinates out of range" );
        }

        int blocks = (max.getX() - min.getX() + 1) * (max.getY() - min.getY() + 1) * (max.getZ() - min.getZ() + 1);
        if( blocks > 4096 ) throw new LuaException( "Too many blocks" );

        List<Map<?, ?>> results = new ArrayList<>( blocks );
        for( int y = min.getY(); y <= max.getY(); y++ )
        {
            for( int z = min.getZ(); z <= max.getZ(); z++ )
            {
                for( int x = min.getX(); x <= max.getX(); x++ )
                {
                    BlockPos pos = new BlockPos( x, y, z );
                    results.add( getBlockInfo( world, pos ) );
                }
            }
        }

        return results;
    }

    @LuaFunction( mainThread = true )
    public final Map<?, ?> getBlockInfo( int x, int y, int z ) throws LuaException
    {
        // Get the details of the block
        World world = computer.getWorld();
        BlockPos position = new BlockPos( x, y, z );
        if( World.isValid( position ) )
        {
            return getBlockInfo( world, position );
        }
        else
        {
            throw new LuaException( "Co-ordinates out of range" );
        }
    }
}
