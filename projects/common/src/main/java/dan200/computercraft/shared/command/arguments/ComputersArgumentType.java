// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.command.arguments;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.core.ServerContext;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;

import static dan200.computercraft.shared.command.CommandUtils.suggest;
import static dan200.computercraft.shared.command.CommandUtils.suggestOnServer;
import static dan200.computercraft.shared.command.Exceptions.COMPUTER_ARG_NONE;

public final class ComputersArgumentType implements ArgumentType<ComputersArgumentType.ComputersSupplier> {
    private static final ComputersArgumentType MANY = new ComputersArgumentType(false);
    private static final ComputersArgumentType SOME = new ComputersArgumentType(true);

    private static final List<String> EXAMPLES = List.of(
        "0", "#0", "@Label", "~Advanced"
    );

    public static ComputersArgumentType manyComputers() {
        return MANY;
    }

    public static ComputersArgumentType someComputers() {
        return SOME;
    }

    public static Collection<ServerComputer> getComputersArgument(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        return context.getArgument(name, ComputersSupplier.class).unwrap(context.getSource());
    }

    private final boolean requireSome;

    private ComputersArgumentType(boolean requireSome) {
        this.requireSome = requireSome;
    }

    @Override
    public ComputersSupplier parse(StringReader reader) throws CommandSyntaxException {
        var start = reader.getCursor();
        var kind = reader.peek();
        ComputersSupplier computers;
        if (kind == '@') {
            reader.skip();
            var label = reader.readUnquotedString();
            computers = getComputers(x -> Objects.equals(label, x.getLabel()));
        } else if (kind == '~') {
            reader.skip();
            var family = reader.readUnquotedString();
            computers = getComputers(x -> x.getFamily().name().equalsIgnoreCase(family));
        } else if (kind == '#') {
            reader.skip();
            var id = reader.readInt();
            computers = getComputers(x -> x.getID() == id);
        } else {
            var instance = reader.readInt();
            computers = s -> {
                var computer = ServerContext.get(s.getServer()).registry().get(instance);
                return computer == null ? List.of() : List.of(computer);
            };
        }

        if (requireSome) {
            var selector = reader.getString().substring(start, reader.getCursor());
            return source -> {
                var matched = computers.unwrap(source);
                if (matched.isEmpty()) throw COMPUTER_ARG_NONE.create(selector);
                return matched;
            };
        } else {
            return computers;
        }
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        var remaining = builder.getRemaining();

        // We can run this one on the client, for obvious reasons.
        if (remaining.startsWith("~")) {
            return suggest(builder, ComputerFamily.values(), x -> "~" + x.name());
        }

        // Verify we've a command source and we're running on the server
        return suggestOnServer(context, s -> {
            if (remaining.startsWith("@")) {
                suggestComputers(s.getSource(), builder, remaining, x -> {
                    var label = x.getLabel();
                    return label == null ? null : "@" + label;
                });
            } else if (remaining.startsWith("#")) {
                suggestComputers(s.getSource(), builder, remaining, c -> "#" + c.getID());
            } else {
                suggestComputers(s.getSource(), builder, remaining, c -> Integer.toString(c.getInstanceID()));
            }

            return builder.buildFuture();
        });
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    private static void suggestComputers(CommandSourceStack source, SuggestionsBuilder builder, String remaining, Function<ServerComputer, String> renderer) {
        remaining = remaining.toLowerCase(Locale.ROOT);
        for (var computer : ServerContext.get(source.getServer()).registry().getComputers()) {
            var converted = renderer.apply(computer);
            if (converted != null && converted.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                builder.suggest(converted);
            }
        }
    }

    private static ComputersSupplier getComputers(Predicate<ServerComputer> predicate) {
        return s -> ServerContext.get(s.getServer()).registry()
            .getComputers()
            .stream()
            .filter(predicate)
            .toList();
    }

    public static class Info implements ArgumentTypeInfo<ComputersArgumentType, Template> {
        @Override
        public void serializeToNetwork(ComputersArgumentType.Template arg, FriendlyByteBuf buf) {
            buf.writeBoolean(arg.requireSome());
        }

        @Override
        public ComputersArgumentType.Template deserializeFromNetwork(FriendlyByteBuf buf) {
            var requiresSome = buf.readBoolean();
            return new ComputersArgumentType.Template(this, requiresSome);
        }

        @Override
        public void serializeToJson(ComputersArgumentType.Template arg, JsonObject json) {
            json.addProperty("requireSome", arg.requireSome);
        }

        @Override
        public ComputersArgumentType.Template unpack(ComputersArgumentType argumentType) {
            return new ComputersArgumentType.Template(this, argumentType.requireSome);
        }
    }

    public record Template(Info info, boolean requireSome) implements ArgumentTypeInfo.Template<ComputersArgumentType> {
        @Override
        public ComputersArgumentType instantiate(CommandBuildContext context) {
            return requireSome ? SOME : MANY;
        }

        @Override
        public Info type() {
            return info;
        }
    }

    @FunctionalInterface
    public interface ComputersSupplier {
        Collection<ServerComputer> unwrap(CommandSourceStack source) throws CommandSyntaxException;
    }

    public static Set<ServerComputer> unwrap(CommandSourceStack source, Collection<ComputersSupplier> suppliers) throws CommandSyntaxException {
        Set<ServerComputer> computers = new HashSet<>();
        for (var supplier : suppliers) computers.addAll(supplier.unwrap(source));
        return computers;
    }
}
