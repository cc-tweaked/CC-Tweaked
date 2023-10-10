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
import java.util.concurrent.CompletableFuture;

import static dan200.computercraft.shared.command.Exceptions.COMPUTER_ARG_MANY;

public final class ComputerArgumentType implements ArgumentType<ComputerArgumentType.ComputerSupplier> {
    private static final ComputerArgumentType INSTANCE = new ComputerArgumentType();

    public static ComputerArgumentType oneComputer() {
        return INSTANCE;
    }

    public static ServerComputer getComputerArgument(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        return context.getArgument(name, ComputerSupplier.class).unwrap(context.getSource());
    }

    private ComputerArgumentType() {
    }

    @Override
    public ComputerSupplier parse(StringReader reader) throws CommandSyntaxException {
        var start = reader.getCursor();
        var supplier = ComputersArgumentType.someComputers().parse(reader);
        var selector = reader.getString().substring(start, reader.getCursor());

        return s -> {
            var computers = supplier.unwrap(s);

            if (computers.size() == 1) return computers.iterator().next();

            var builder = new StringBuilder();
            var first = true;
            for (var computer : computers) {
                if (first) {
                    first = false;
                } else {
                    builder.append(", ");
                }

                builder.append(computer.getInstanceID());
            }


            // We have an incorrect number of computers: reset and throw an error
            reader.setCursor(start);
            throw COMPUTER_ARG_MANY.createWithContext(reader, selector, builder.toString());
        };
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return ComputersArgumentType.someComputers().listSuggestions(context, builder);
    }

    @Override
    public Collection<String> getExamples() {
        return ComputersArgumentType.someComputers().getExamples();
    }

    @FunctionalInterface
    public interface ComputerSupplier {
        ServerComputer unwrap(CommandSourceStack source) throws CommandSyntaxException;
    }
}
