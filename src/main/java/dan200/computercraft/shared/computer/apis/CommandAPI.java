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
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

import java.util.*;

/**
 * @cc.module commands
 */
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
        if( server == null || !server.areCommandBlocksEnabled() )
        {
            return new Object[] { false, createOutput( "Command blocks disabled by server" ) };
        }

        CommandManager commandManager = server.getCommandManager();
        TileCommandComputer.CommandReceiver receiver = computer.getReceiver();
        try
        {
            receiver.clearOutput();
            int result = commandManager.execute( computer.getSource(), command );
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
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        Map<Object, Object> table = new HashMap<>();
        table.put("name",
            Registry.BLOCK.getId(block)
                .toString());
        table.put("world", world.getRegistryKey());

        Map<Object, Object> stateTable = new HashMap<>();
        for (ImmutableMap.Entry<Property<?>, Comparable<?>> entry : state.getEntries()
            .entrySet()) {
            Property<?> property = entry.getKey();
            stateTable.put(property.getName(), getPropertyValue(property, entry.getValue()));
        }
        table.put("state", stateTable);

        BlockEntity tile = world.getBlockEntity(pos);
        if (tile != null) {
            table.put("nbt", NBTUtil.toLua(tile.toTag(new CompoundTag())));
        }

        return table;
    }

    @SuppressWarnings ({
        "unchecked",
        "rawtypes"
    })
    private static Object getPropertyValue(Property property, Comparable value) {
        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value;
        }
        return property.name(value);
    }

    /**
     * Execute a specific command.
     *
     * @param command The command to execute.
     * @return See {@code cc.treturn}.
     * @cc.treturn boolean Whether the command executed successfully.
     * @cc.treturn { string... } The output of this command, as a list of lines.
     * @cc.treturn number|nil The number of "affected" objects, or `nil` if the command failed. The definition of this
     * varies from command to command.
     * @cc.usage Set the block above the command computer to stone.
     * <pre>
     * commands.exec("setblock ~ ~1 ~ minecraft:stone")
     * </pre>
     */
    @LuaFunction( mainThread = true )
    public final Object[] exec( String command )
    {
        return doCommand( command );
    }

    /**
     * Asynchronously execute a command.
     *
     * Unlike {@link #exec}, this will immediately return, instead of waiting for the
     * command to execute. This allows you to run multiple commands at the same
     * time.
     *
     * When this command has finished executing, it will queue a `task_complete`
     * event containing the result of executing this command (what {@link #exec} would
     * return).
     *
     * @param context The context this command executes under.
     * @param command The command to execute.
     * @return The "task id". When this command has been executed, it will queue a `task_complete` event with a matching id.
     * @throws LuaException (hidden) If the task cannot be created.
     * @cc.tparam string command The command to execute.
     * @cc.usage Asynchronously sets the block above the computer to stone.
     * <pre>
     * commands.execAsync("~ ~1 ~ minecraft:stone")
     * </pre>
     * @cc.see parallel One may also use the parallel API to run multiple commands at once.
     */
    @LuaFunction
    public final long execAsync( ILuaContext context, String command ) throws LuaException
    {
        return context.issueMainThreadTask( () -> doCommand( command ) );
    }

    /**
     * List all available commands which the computer has permission to execute.
     *
     * @param args Arguments to this function.
     * @return A list of all available commands
     * @throws LuaException (hidden) On non-string arguments.
     * @cc.tparam string ... The sub-command to complete.
     */
    @LuaFunction( mainThread = true )
    public final List<String> list( IArguments args ) throws LuaException
    {
        MinecraftServer server = computer.getWorld().getServer();

        if( server == null ) return Collections.emptyList();
        CommandNode<ServerCommandSource> node = server.getCommandManager().getDispatcher().getRoot();
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

    /**
     * Get the position of the current command computer.
     *
     * @return The block's position.
     * @cc.treturn number This computer's x position.
     * @cc.treturn number This computer's y position.
     * @cc.treturn number This computer's z position.
     * @cc.see gps.locate To get the position of a non-command computer.
     */
    @LuaFunction
    public final Object[] getBlockPosition()
    {
        // This is probably safe to do on the Lua thread. Probably.
        BlockPos pos = computer.getPos();
        return new Object[] { pos.getX(), pos.getY(), pos.getZ() };
    }

    /**
     * Get information about a range of blocks.
     *
     * This returns the same information as @{getBlockInfo}, just for multiple
     * blocks at once.
     *
     * Blocks are traversed by ascending y level, followed by z and x - the returned
     * table may be indexed using `x + z*width + y*depth*depth`.
     *
     * @param minX The start x coordinate of the range to query.
     * @param minY The start y coordinate of the range to query.
     * @param minZ The start z coordinate of the range to query.
     * @param maxX The end x coordinate of the range to query.
     * @param maxY The end y coordinate of the range to query.
     * @param maxZ The end z coordinate of the range to query.
     * @return A list of information about each block.
     * @throws LuaException If the coordinates are not within the world.
     * @throws LuaException If trying to get information about more than 4096 blocks.
     */
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
        if( !World.method_24794( min ) || !World.method_24794( max ) )
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

    /**
     * Get some basic information about a block.
     *
     * The returned table contains the current name, metadata and block state (as
     * with @{turtle.inspect}). If there is a tile entity for that block, its NBT
     * will also be returned.
     *
     * @param x The x position of the block to query.
     * @param y The y position of the block to query.
     * @param z The z position of the block to query.
     * @return The given block's information.
     * @throws LuaException If the coordinates are not within the world, or are not currently loaded.
     */
    @LuaFunction( mainThread = true )
    public final Map<?, ?> getBlockInfo( int x, int y, int z ) throws LuaException
    {
        // Get the details of the block
        World world = computer.getWorld();
        BlockPos position = new BlockPos( x, y, z );
        if( World.method_24794( position ) )
        {
            return getBlockInfo( world, position );
        }
        else
        {
            throw new LuaException( "Co-ordinates out of range" );
        }
    }
}
