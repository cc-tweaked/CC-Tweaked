// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.peripheral.commandblock;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.computer.apis.CommandAPI;
import net.minecraft.world.level.block.entity.CommandBlockEntity;

import javax.annotation.Nullable;

/**
 * This peripheral allows you to interact with command blocks.
 * <p>
 * Command blocks are only wrapped as peripherals if the {@code enable_command_block} option is true within the
 * config.
 * <p>
 * This API is <em>not</em> the same as the {@link CommandAPI} API, which is exposed on command computers.
 *
 * @cc.module command
 */
public class CommandBlockPeripheral implements IPeripheral {
    private final CommandBlockEntity commandBlock;

    public CommandBlockPeripheral(CommandBlockEntity commandBlock) {
        this.commandBlock = commandBlock;
    }

    @Override
    public String getType() {
        return "command";
    }

    /**
     * Get the command this command block will run.
     *
     * @return The current command.
     */
    @LuaFunction(mainThread = true)
    public final String getCommand() {
        return commandBlock.getCommandBlock().getCommand();
    }

    /**
     * Set the command block's command.
     *
     * @param command The new command.
     */
    @LuaFunction(mainThread = true)
    public final void setCommand(String command) {
        commandBlock.getCommandBlock().setCommand(command);
        commandBlock.getCommandBlock().onUpdated();
    }

    /**
     * Execute the command block once.
     *
     * @return The result of executing.
     * @cc.treturn boolean If the command completed successfully.
     * @cc.treturn string|nil A failure message.
     */
    @LuaFunction(mainThread = true)
    public final Object[] runCommand() {
        commandBlock.getCommandBlock().performCommand(commandBlock.getLevel());
        var result = commandBlock.getCommandBlock().getSuccessCount();
        return result > 0 ? new Object[]{ true } : new Object[]{ false, "Command failed" };
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return other != null && other.getClass() == getClass();
    }

    @Override
    public Object getTarget() {
        return commandBlock;
    }
}
