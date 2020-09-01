/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.commandblock;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.computer.apis.CommandAPI;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.CommandBlockBlockEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This peripheral allows you to interact with command blocks.
 *
 * Command blocks are only wrapped as peripherals if the {@literal enable_command_block} option is true within the
 * config.
 *
 * This API is <em>not</em> the same as the {@link CommandAPI} API, which is exposed on command computers.
 *
 * @cc.module command
 */
public class CommandBlockPeripheral implements IPeripheral
{
    private static final Identifier CAP_ID = new Identifier( ComputerCraft.MOD_ID, "command_block" );

    private final CommandBlockBlockEntity commandBlock;

    public CommandBlockPeripheral( CommandBlockBlockEntity commandBlock )
    {
        this.commandBlock = commandBlock;
    }

    @Nonnull
    @Override
    public String getType()
    {
        return "command";
    }

    /**
     * Get the command this command block will run.
     *
     * @return The current command.
     */
    @LuaFunction( mainThread = true )
    public final String getCommand()
    {
        return commandBlock.getCommandExecutor().getCommand();
    }

    /**
     * Set the command block's command.
     *
     * @param command The new command.
     */
    @LuaFunction( mainThread = true )
    public final void setCommand( String command )
    {
        commandBlock.getCommandExecutor().setCommand( command );
        commandBlock.getCommandExecutor().markDirty();
    }

    /**
     * Execute the command block once.
     *
     * @return The result of executing.
     * @cc.treturn boolean If the command completed successfully.
     * @cc.treturn string|nil A failure message.
     */
    @LuaFunction( mainThread = true )
    public final Object[] runCommand()
    {
        commandBlock.getCommandExecutor().execute( commandBlock.getWorld() );
        int result = commandBlock.getCommandExecutor().getSuccessCount();
        return result > 0 ? new Object[] { true } : new Object[] { false, "Command failed" };
    }

    @Override
    public boolean equals( IPeripheral other )
    {
        return other != null && other.getClass() == getClass();
    }

    @Nonnull
    @Override
    public Object getTarget()
    {
        return commandBlock;
    }
}
