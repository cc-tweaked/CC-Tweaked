// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.command.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dan200.computercraft.shared.computer.core.ServerComputer;
import net.minecraft.commands.CommandSourceStack;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class ComputerArgumentType implements ArgumentType<ComputerSelector> {
    private static final ComputerArgumentType INSTANCE = new ComputerArgumentType();

    private static final List<String> EXAMPLES = List.of(
        "0", "123", "@c[instance_id=123]"
    );

    public static ComputerArgumentType get() {
        return INSTANCE;
    }

    private ComputerArgumentType() {
    }

    /**
     * Extract a list of computers from a {@link CommandContext} argument.
     *
     * @param context The current command context.
     * @param name    The name of the argument.
     * @return The found computer(s).
     */
    public static List<ServerComputer> getMany(CommandContext<CommandSourceStack> context, String name) {
        return context.getArgument(name, ComputerSelector.class).find(context.getSource()).toList();
    }

    /**
     * Extract a single computer from a {@link CommandContext} argument.
     *
     * @param context The current command context.
     * @param name    The name of the argument.
     * @return The found computer.
     * @throws CommandSyntaxException If exactly one computer could not be found.
     */
    public static ServerComputer getOne(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        return context.getArgument(name, ComputerSelector.class).findOne(context.getSource());
    }

    @Override
    public ComputerSelector parse(StringReader reader) throws CommandSyntaxException {
        return ComputerSelector.parse(reader);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return ComputerSelector.suggest(context, builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
