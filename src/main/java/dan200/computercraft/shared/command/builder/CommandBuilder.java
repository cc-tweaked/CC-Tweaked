/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.command.builder;

import static dan200.computercraft.shared.command.Exceptions.ARGUMENT_EXPECTED;
import static dan200.computercraft.shared.command.builder.HelpingArgumentBuilder.literal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.CommandNode;
import dan200.computercraft.shared.command.arguments.RepeatArgumentType;

import net.minecraft.server.command.ServerCommandSource;

/**
 * An alternative way of building command nodes, so one does not have to nest. {@link ArgumentBuilder#then(CommandNode)}s.
 *
 * @param <S> The command source we consume.
 */
public class CommandBuilder<S> implements CommandNodeBuilder<S, Command<S>> {
    private List<ArgumentBuilder<S, ?>> args = new ArrayList<>();
    private Predicate<S> requires;

    public static CommandBuilder<ServerCommandSource> args() {
        return new CommandBuilder<>();
    }

    public static CommandBuilder<ServerCommandSource> command(String literal) {
        CommandBuilder<ServerCommandSource> builder = new CommandBuilder<>();
        builder.args.add(literal(literal));
        return builder;
    }

    public CommandBuilder<S> requires(Predicate<S> predicate) {
        this.requires = this.requires == null ? predicate : this.requires.and(predicate);
        return this;
    }

    public CommandBuilder<S> arg(String name, ArgumentType<?> type) {
        this.args.add(RequiredArgumentBuilder.argument(name, type));
        return this;
    }

    public <T> CommandNodeBuilder<S, ArgCommand<S, List<T>>> argManyValue(String name, ArgumentType<T> type, T defaultValue) {
        return this.argManyValue(name, type, Collections.singletonList(defaultValue));
    }

    public <T> CommandNodeBuilder<S, ArgCommand<S, List<T>>> argManyValue(String name, ArgumentType<T> type, List<T> empty) {
        return this.argMany(name, type, () -> empty);
    }

    public <T> CommandNodeBuilder<S, ArgCommand<S, List<T>>> argMany(String name, ArgumentType<T> type, Supplier<List<T>> empty) {
        return this.argMany(name, RepeatArgumentType.some(type, ARGUMENT_EXPECTED), empty);
    }

    private <T, U> CommandNodeBuilder<S, ArgCommand<S, List<T>>> argMany(String name, RepeatArgumentType<T, ?> type, Supplier<List<T>> empty) {
        if (this.args.isEmpty()) {
            throw new IllegalStateException("Cannot have empty arg chain builder");
        }

        return command -> {
            // The node for no arguments
            ArgumentBuilder<S, ?> tail = this.tail(ctx -> command.run(ctx, empty.get()));

            // The node for one or more arguments
            ArgumentBuilder<S, ?> moreArg = RequiredArgumentBuilder.<S, List<T>>argument(name, type).executes(ctx -> command.run(ctx, getList(ctx, name)));

            // Chain all of them together!
            tail.then(moreArg);
            return this.link(tail);
        };
    }

    private ArgumentBuilder<S, ?> tail(Command<S> command) {
        ArgumentBuilder<S, ?> defaultTail = this.args.get(this.args.size() - 1);
        defaultTail.executes(command);
        if (this.requires != null) {
            defaultTail.requires(this.requires);
        }
        return defaultTail;
    }

    @SuppressWarnings ("unchecked")
    private static <T> List<T> getList(CommandContext<?> context, String name) {
        return (List<T>) context.getArgument(name, List.class);
    }

    private CommandNode<S> link(ArgumentBuilder<S, ?> tail) {
        for (int i = this.args.size() - 2; i >= 0; i--) {
            tail = this.args.get(i)
                            .then(tail);
        }
        return tail.build();
    }

    public <T> CommandNodeBuilder<S, ArgCommand<S, List<T>>> argManyFlatten(String name, ArgumentType<List<T>> type, Supplier<List<T>> empty) {
        return this.argMany(name, RepeatArgumentType.someFlat(type, ARGUMENT_EXPECTED), empty);
    }

    @Override
    public CommandNode<S> executes(Command<S> command) {
        if (this.args.isEmpty()) {
            throw new IllegalStateException("Cannot have empty arg chain builder");
        }

        return this.link(this.tail(command));
    }
}
