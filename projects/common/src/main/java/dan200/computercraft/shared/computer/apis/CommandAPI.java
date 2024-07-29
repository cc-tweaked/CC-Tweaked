// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.computer.apis;

import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dan200.computercraft.api.component.AdminComputer;
import dan200.computercraft.api.detail.BlockReference;
import dan200.computercraft.api.detail.VanillaDetailRegistries;
import dan200.computercraft.api.lua.*;
import dan200.computercraft.core.Logging;
import dan200.computercraft.shared.util.NBTUtil;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @cc.module commands
 * @cc.since 1.7
 */
public class CommandAPI implements ILuaAPI {
    private static final Logger LOG = LoggerFactory.getLogger(CommandAPI.class);

    private final IComputerSystem computer;
    private final AdminComputer admin;
    private final OutputReceiver receiver = new OutputReceiver();

    public CommandAPI(IComputerSystem computer, AdminComputer admin) {
        this.computer = computer;
        this.admin = admin;
    }

    @Override
    public String[] getNames() {
        return new String[]{ "commands" };
    }

    private static Object createOutput(String output) {
        return new Object[]{ output };
    }

    private Object[] doCommand(String command) {
        var server = computer.getLevel().getServer();
        if (!server.isCommandBlockEnabled()) {
            return new Object[]{ false, createOutput("Command blocks disabled by server") };
        }

        var commandManager = server.getCommands();
        try {
            receiver.clearOutput();
            var result = commandManager.performPrefixedCommand(getSource(), command);
            return new Object[]{ result > 0, receiver.copyOutput(), result };
        } catch (Throwable t) {
            LOG.error(Logging.JAVA_ERROR, "Error running command.", t);
            return new Object[]{ false, createOutput("Java Exception Thrown: " + t) };
        }
    }

    private static Map<?, ?> getBlockInfo(Level world, BlockPos pos) {
        // Get the details of the block
        var block = new BlockReference(world, pos);
        var table = VanillaDetailRegistries.BLOCK_IN_WORLD.getDetails(block);

        var tile = block.blockEntity();
        if (tile != null) table.put("nbt", NBTUtil.toLua(tile.saveWithFullMetadata()));

        return table;
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
     * @cc.changed 1.71 Added return value with command output.
     * @cc.changed 1.85.0 Added return value with the number of affected objects.
     * @cc.usage Set the block above the command computer to stone.
     * <pre>{@code
     * commands.exec("setblock ~ ~1 ~ minecraft:stone")
     * }</pre>
     */
    @LuaFunction(mainThread = true)
    public final Object[] exec(String command) {
        return doCommand(command);
    }

    /**
     * Asynchronously execute a command.
     * <p>
     * Unlike {@link #exec}, this will immediately return, instead of waiting for the
     * command to execute. This allows you to run multiple commands at the same
     * time.
     * <p>
     * When this command has finished executing, it will queue a `task_complete`
     * event containing the result of executing this command (what {@link #exec} would
     * return).
     *
     * @param context The context this command executes under.
     * @param command The command to execute.
     * @return The "task id". When this command has been executed, it will queue a `task_complete` event with a matching id.
     * @throws LuaException (hidden) If the task cannot be created.
     * @cc.usage Asynchronously sets the block above the computer to stone.
     * <pre>{@code
     * commands.execAsync("setblock ~ ~1 ~ minecraft:stone")
     * }</pre>
     * @cc.see parallel One may also use the parallel API to run multiple commands at once.
     */
    @LuaFunction
    public final long execAsync(ILuaContext context, String command) throws LuaException {
        return context.issueMainThreadTask(() -> doCommand(command));
    }

    /**
     * List all available commands which the computer has permission to execute.
     *
     * @param args Arguments to this function.
     * @return A list of all available commands
     * @throws LuaException (hidden) On non-string arguments.
     * @cc.tparam string ... The sub-command to complete.
     */
    @LuaFunction(mainThread = true)
    public final List<String> list(IArguments args) throws LuaException {
        var server = computer.getLevel().getServer();

        CommandNode<CommandSourceStack> node = server.getCommands().getDispatcher().getRoot();
        for (var j = 0; j < args.count(); j++) {
            var name = args.getString(j);
            node = node.getChild(name);
            if (!(node instanceof LiteralCommandNode)) return List.of();
        }

        List<String> result = new ArrayList<>();
        for (CommandNode<?> child : node.getChildren()) {
            if (child instanceof LiteralCommandNode<?>) result.add(child.getName());
        }
        return Collections.unmodifiableList(result);
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
    public final Object[] getBlockPosition() {
        // This is probably safe to do on the Lua thread. Probably.
        var pos = computer.getPosition();
        return new Object[]{ pos.getX(), pos.getY(), pos.getZ() };
    }

    /**
     * Get information about a range of blocks.
     * <p>
     * This returns the same information as [`getBlockInfo`], just for multiple
     * blocks at once.
     * <p>
     * Blocks are traversed by ascending y level, followed by z and x - the returned
     * table may be indexed using `x + z*width + y*width*depth + 1`.
     *
     * @param minX      The start x coordinate of the range to query.
     * @param minY      The start y coordinate of the range to query.
     * @param minZ      The start z coordinate of the range to query.
     * @param maxX      The end x coordinate of the range to query.
     * @param maxY      The end y coordinate of the range to query.
     * @param maxZ      The end z coordinate of the range to query.
     * @param dimension The dimension to query (e.g. "minecraft:overworld"). Defaults to the current dimension.
     * @return A list of information about each block.
     * @throws LuaException If the coordinates are not within the world.
     * @throws LuaException If trying to get information about more than 4096 blocks.
     * @cc.since 1.76
     * @cc.changed 1.99 Added {@code dimension} argument.
     * @cc.usage Print out all blocks in a cube around the computer.
     *
     * <pre>{@code
     * -- Get a 3x3x3 cube around the computer
     * local x, y, z = commands.getBlockPosition()
     * local min_x, min_y, min_z, max_x, max_y, max_z = x - 1, y - 1, z - 1, x + 1, y + 1, z + 1
     * local blocks = commands.getBlockInfos(min_x, min_y, min_z, max_x, max_y, max_z)
     *
     * -- Then loop over all blocks and print them out.
     * local width, height, depth = max_x - min_x + 1, max_y - min_y + 1, max_z - min_z + 1
     * for x = min_x, max_x do
     *   for y = min_y, max_y do
     *     for z = min_z, max_z do
     *       print(("%d, %d %d => %s"):format(x, y, z, blocks[(x - min_x) + (z - min_z) * width + (y - min_y) * width * depth + 1].name))
     *     end
     *   end
     * end
     * }</pre>
     */
    @LuaFunction(mainThread = true)
    public final List<Map<?, ?>> getBlockInfos(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, Optional<String> dimension) throws LuaException {
        // Get the details of the block
        var world = getLevel(dimension);
        var min = new BlockPos(
            Math.min(minX, maxX),
            Math.min(minY, maxY),
            Math.min(minZ, maxZ)
        );
        var max = new BlockPos(
            Math.max(minX, maxX),
            Math.max(minY, maxY),
            Math.max(minZ, maxZ)
        );
        if (!world.isInWorldBounds(min) || !world.isInWorldBounds(max)) {
            throw new LuaException("Co-ordinates out of range");
        }

        var blocks = (max.getX() - min.getX() + 1) * (max.getY() - min.getY() + 1) * (max.getZ() - min.getZ() + 1);
        if (blocks > 4096) throw new LuaException("Too many blocks");

        List<Map<?, ?>> results = new ArrayList<>(blocks);
        for (var y = min.getY(); y <= max.getY(); y++) {
            for (var z = min.getZ(); z <= max.getZ(); z++) {
                for (var x = min.getX(); x <= max.getX(); x++) {
                    var pos = new BlockPos(x, y, z);
                    results.add(getBlockInfo(world, pos));
                }
            }
        }

        return results;
    }

    /**
     * Get some basic information about a block.
     * <p>
     * The returned table contains the current name, metadata and block state (as
     * with [`turtle.inspect`]). If there is a tile entity for that block, its NBT
     * will also be returned.
     *
     * @param x         The x position of the block to query.
     * @param y         The y position of the block to query.
     * @param z         The z position of the block to query.
     * @param dimension The dimension to query (e.g. "minecraft:overworld"). Defaults to the current dimension.
     * @return The given block's information.
     * @throws LuaException If the coordinates are not within the world, or are not currently loaded.
     * @cc.changed 1.76 Added block state info to return value
     * @cc.changed 1.99 Added {@code dimension} argument.
     */
    @LuaFunction(mainThread = true)
    public final Map<?, ?> getBlockInfo(int x, int y, int z, Optional<String> dimension) throws LuaException {
        var level = getLevel(dimension);
        var position = new BlockPos(x, y, z);
        if (!level.isInWorldBounds(position)) throw new LuaException("Co-ordinates out of range");
        return getBlockInfo(level, position);
    }

    private Level getLevel(Optional<String> id) throws LuaException {
        var currentLevel = computer.getLevel();

        if (id.isEmpty()) return currentLevel;

        var dimensionId = ResourceLocation.tryParse(id.get());
        if (dimensionId == null) throw new LuaException("Invalid dimension name");

        Level level = currentLevel.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
        if (level == null) throw new LuaException("Unknown dimension");

        return level;
    }

    private CommandSourceStack getSource() {
        var name = "@";
        var label = computer.getLabel();
        if (label != null) name = label;

        return new CommandSourceStack(receiver,
            Vec3.atCenterOf(computer.getPosition()), Vec2.ZERO,
            computer.getLevel(), admin.permissionLevel(),
            name, Component.literal(name),
            computer.getLevel().getServer(), null
        );
    }

    /**
     * A {@link CommandSource} that consumes output messages and stores them to a list.
     */
    private final class OutputReceiver implements CommandSource {
        private final List<String> output = new ArrayList<>();

        void clearOutput() {
            output.clear();
        }

        List<String> copyOutput() {
            return List.copyOf(output);
        }

        @Override
        public void sendSystemMessage(Component textComponent) {
            output.add(textComponent.getString());
        }

        @Override
        public boolean acceptsSuccess() {
            return true;
        }

        @Override
        public boolean acceptsFailure() {
            return true;
        }

        @Override
        public boolean shouldInformAdmins() {
            return computer.getLevel().getGameRules().getBoolean(GameRules.RULE_COMMANDBLOCKOUTPUT);
        }
    }
}
