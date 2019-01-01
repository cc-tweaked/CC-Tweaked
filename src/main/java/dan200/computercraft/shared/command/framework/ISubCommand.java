/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.command.framework;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * A slightly different implementation of {@link ICommand} which is delegated to.
 */
public interface ISubCommand
{
    /**
     * Get the name of this command
     *
     * @return The name of this command
     * @see ICommand#getName()
     */
    @Nonnull
    String getName();

    /**
     * Get the usage of this command
     *
     * @param context The context this command is executed in
     * @return The usage of this command
     * @see ICommand#getUsage(ICommandSender)
     */
    @Nonnull
    String getUsage( CommandContext context );

    /**
     * Get a short description of this command, including its usage.
     *
     * @return The command's synopsis
     */
    @Nonnull
    String getSynopsis();

    /**
     * Get the lengthy description of this command. This synopsis is prepended to this.
     *
     * @return The command's description
     */
    @Nonnull
    String getDescription();

    /**
     * Determine whether a given command sender has permission to execute this command.
     *
     * @param context The current command context.
     * @return Whether this command can be executed.
     * @see ICommand#checkPermission(MinecraftServer, ICommandSender)
     */
    boolean checkPermission( @Nonnull CommandContext context );

    /**
     * Execute this command
     *
     * @param context   The current command context.
     * @param arguments The arguments passed  @throws CommandException When an error occurs
     * @see ICommand#execute(MinecraftServer, ICommandSender, String[])
     */
    void execute( @Nonnull CommandContext context, @Nonnull List<String> arguments ) throws CommandException;

    /**
     * Get a list of possible completions
     *
     * @param context   The current command context.
     * @param arguments The arguments passed. You should complete the last one.
     * @return List of possible completions
     * @see ICommand#getTabCompletions(MinecraftServer, ICommandSender, String[], BlockPos)
     */
    @Nonnull
    List<String> getCompletion( @Nonnull CommandContext context, @Nonnull List<String> arguments );
}
