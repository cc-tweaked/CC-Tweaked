/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.command.builder;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

/**
 * A {@link Command} which accepts an argument
 */
@FunctionalInterface
public interface ArgCommand<S, T>
{
    int run( CommandContext<S> ctx, T arg ) throws CommandSyntaxException;
}
