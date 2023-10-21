// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.command.builder;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.CommandNode;
import dan200.computercraft.shared.command.arguments.RepeatArgumentType;
import net.minecraft.commands.CommandSourceStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static dan200.computercraft.shared.command.Exceptions.ARGUMENT_EXPECTED;
import static dan200.computercraft.shared.command.builder.HelpingArgumentBuilder.literal;

/**
 * An alternative way of building command nodes, so one does not have to nest.
 * {@link ArgumentBuilder#then(CommandNode)}s.
 *
 * @param <S> The command source we consume.
 */
public class CommandBuilder<S> implements CommandNodeBuilder<S, Command<S>> {
    private final List<ArgumentBuilder<S, ?>> args = new ArrayList<>();
    private @Nullable Predicate<S> requires;

    public static CommandBuilder<CommandSourceStack> args() {
        return new CommandBuilder<>();
    }

    public static CommandBuilder<CommandSourceStack> command(String literal) {
        var builder = new CommandBuilder<CommandSourceStack>();
        builder.args.add(literal(literal));
        return builder;
    }

    public CommandBuilder<S> requires(Predicate<S> predicate) {
        if (requires != null) throw new IllegalStateException("Requires already set");
        requires = predicate;
        return this;
    }

    public CommandBuilder<S> arg(ArgumentBuilder<S, ?> arg) {
        args.add(arg);
        return this;
    }

    public CommandBuilder<S> arg(String name, ArgumentType<?> type) {
        return arg(RequiredArgumentBuilder.argument(name, type));
    }

    public <T> CommandNodeBuilder<S, ArgCommand<S, List<T>>> argManyValue(String name, ArgumentType<T> type, List<T> empty) {
        return argMany(name, type, () -> empty);
    }

    public <T> CommandNodeBuilder<S, ArgCommand<S, List<T>>> argManyValue(String name, ArgumentType<T> type, T defaultValue) {
        return argManyValue(name, type, List.of(defaultValue));
    }

    public <T> CommandNodeBuilder<S, ArgCommand<S, List<T>>> argMany(String name, ArgumentType<T> type, Supplier<List<T>> empty) {
        return argMany(name, RepeatArgumentType.some(type, ARGUMENT_EXPECTED), empty);
    }

    public <T> CommandNodeBuilder<S, ArgCommand<S, List<T>>> argManyFlatten(String name, ArgumentType<List<T>> type, Supplier<List<T>> empty) {
        return argMany(name, RepeatArgumentType.someFlat(type, ARGUMENT_EXPECTED), empty);
    }

    private <T> CommandNodeBuilder<S, ArgCommand<S, List<T>>> argMany(String name, RepeatArgumentType<T, ?> type, Supplier<List<T>> empty) {
        if (args.isEmpty()) throw new IllegalStateException("Cannot have empty arg chain builder");

        return command -> {
            // The node for no arguments
            var tail = setupTail(ctx -> command.run(ctx, empty.get()));

            // The node for one or more arguments
            ArgumentBuilder<S, ?> moreArg = RequiredArgumentBuilder
                .<S, List<T>>argument(name, type)
                .executes(ctx -> command.run(ctx, getList(ctx, name)));

            // Chain all of them together!
            tail.then(moreArg);
            return buildTail(tail);
        };
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> getList(CommandContext<?> context, String name) {
        return (List<T>) context.getArgument(name, List.class);
    }

    @Override
    public CommandNode<S> executes(Command<S> command) {
        return buildTail(setupTail(command));
    }

    private ArgumentBuilder<S, ?> setupTail(Command<S> command) {
        return args.get(args.size() - 1).executes(command);
    }

    private CommandNode<S> buildTail(ArgumentBuilder<S, ?> tail) {
        for (var i = args.size() - 2; i >= 0; i--) tail = args.get(i).then(tail);
        if (requires != null) tail.requires(requires);
        return tail.build();
    }
}
