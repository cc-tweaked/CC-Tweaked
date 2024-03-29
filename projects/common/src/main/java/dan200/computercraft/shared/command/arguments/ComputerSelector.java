// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.command.arguments;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.core.ServerContext;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dan200.computercraft.shared.command.CommandUtils.suggestOnServer;
import static dan200.computercraft.shared.command.Exceptions.*;
import static dan200.computercraft.shared.command.arguments.ArgumentParserUtils.consume;
import static dan200.computercraft.shared.command.text.ChatHelpers.makeComputerDumpCommand;

public record ComputerSelector(
    String selector,
    OptionalInt instanceId,
    @Nullable UUID instanceUuid,
    OptionalInt computerId,
    @Nullable String label,
    @Nullable ComputerFamily family,
    @Nullable AABB bounds,
    @Nullable MinMaxBounds.Doubles range
) {
    private static final ComputerSelector all = new ComputerSelector("@c[]", OptionalInt.empty(), null, OptionalInt.empty(), null, null, null, null);

    private static UuidArgument uuidArgument = UuidArgument.uuid();

    /**
     * A {@link ComputerSelector} which matches all computers.
     *
     * @return A {@link ComputerSelector} instance.
     */
    public static ComputerSelector all() {
        return all;
    }

    /**
     * Find all computers matching this selector.
     *
     * @param source The source requesting these computers.
     * @return The stream of matching computers.
     */
    public Stream<ServerComputer> find(CommandSourceStack source) {
        var context = ServerContext.get(source.getServer());
        if (instanceId().isPresent()) {
            var computer = context.registry().get(instanceId().getAsInt());
            return computer != null && matches(source, computer) ? Stream.of(computer) : Stream.of();
        }

        if (instanceUuid() != null) {
            var computer = context.registry().get(instanceUuid());
            return computer != null && matches(source, computer) ? Stream.of(computer) : Stream.of();
        }

        return context.registry().getComputers().stream().filter(c -> matches(source, c));
    }

    /**
     * Find exactly one computer which matches this selector.
     *
     * @param source The source requesting this computer.
     * @return The computer.
     * @throws CommandSyntaxException If no or multiple computers could be found.
     */
    public ServerComputer findOne(CommandSourceStack source) throws CommandSyntaxException {
        var computers = find(source).toList();
        if (computers.isEmpty()) throw COMPUTER_ARG_NONE.create(selector);
        if (computers.size() == 1) return computers.iterator().next();

        var builder = MutableComponent.create(ComponentContents.EMPTY);
        var first = true;
        for (var computer : computers) {
            if (first) {
                first = false;
            } else {
                builder.append(", ");
            }

            builder.append(makeComputerDumpCommand(computer));
        }


        // We have an incorrect number of computers: throw an error
        throw COMPUTER_ARG_MANY.create(selector, builder);
    }

    /**
     * Determine if this selector matches a given computer.
     *
     * @param source   The command source, used for distance comparisons.
     * @param computer The computer to check.
     * @return If this computer is matched by the selector.
     */
    public boolean matches(CommandSourceStack source, ServerComputer computer) {
        return (instanceId().isEmpty() || computer.getInstanceID() == instanceId().getAsInt())
            && (instanceUuid() == null || computer.getInstanceUUID().equals(instanceUuid()))
            && (computerId().isEmpty() || computer.getID() == computerId().getAsInt())
            && (label == null || Objects.equals(computer.getLabel(), label))
            && (family == null || computer.getFamily() == family)
            && (bounds == null || (source.getLevel() == computer.getLevel() && bounds.contains(Vec3.atCenterOf(computer.getPosition()))))
            && (range == null || (source.getLevel() == computer.getLevel() && range.matchesSqr(source.getPosition().distanceToSqr(Vec3.atCenterOf(computer.getPosition())))));
    }

    /**
     * Parse an input string.
     *
     * @param reader The reader to parse from.
     * @return The parsed selector.
     * @throws CommandSyntaxException If the selector was incomplete or malformed.
     */
    public static ComputerSelector parse(StringReader reader) throws CommandSyntaxException {
        var start = reader.getCursor();

        var builder = new Builder();

        if (consume(reader, "@c[")) {
            parseSelector(builder, reader);
        } else {
            // TODO(1.20.5): Only parse computer ids here.
            var kind = reader.peek();
            if (kind == '@') {
                reader.skip();
                builder.label = reader.readString();
            } else if (kind == '~') {
                reader.skip();
                builder.family = parseFamily(reader);
            } else if (kind == '#') {
                reader.skip();
                builder.computerId = OptionalInt.of(reader.readInt());
            } else {
                builder.instanceId = OptionalInt.of(reader.readInt());
            }
        }

        var selector = reader.getString().substring(start, reader.getCursor());
        return new ComputerSelector(selector, builder.instanceId, builder.instanceUuid, builder.computerId, builder.label, builder.family, builder.bounds, builder.range);
    }

    private static void parseSelector(Builder builder, StringReader reader) throws CommandSyntaxException {
        Set<Option> seenOptions = new HashSet<>();
        while (true) {
            reader.skipWhitespace();

            if (!reader.canRead()) throw ERROR_EXPECTED_END_OF_OPTIONS.createWithContext(reader);
            if (consume(reader, ']')) break;

            // Read the option and validate it.
            var option = parseOption(reader, seenOptions);
            reader.skipWhitespace();
            if (!consume(reader, '=')) throw ERROR_EXPECTED_OPTION_VALUE.createWithContext(reader, option.name());
            reader.skipWhitespace();
            option.parser.parse(reader, builder);
            reader.skipWhitespace();

            if (consume(reader, ']')) break;
            if (!consume(reader, ',')) throw ERROR_EXPECTED_END_OF_OPTIONS.createWithContext(reader);
        }
    }

    private static Option parseOption(StringReader reader, Set<Option> seen) throws CommandSyntaxException {
        var start = reader.getCursor();
        var name = reader.readUnquotedString();
        var option = options.get(name);
        if (option == null) {
            reader.setCursor(start);
            throw ERROR_UNKNOWN_OPTION.createWithContext(reader, name);
        } else if (!seen.add(option)) {
            throw ERROR_INAPPLICABLE_OPTION.createWithContext(reader, name);
        }

        return option;
    }

    private static ComputerFamily parseFamily(StringReader reader) throws CommandSyntaxException {
        var start = reader.getCursor();
        var name = reader.readUnquotedString();
        var family = Arrays.stream(ComputerFamily.values()).filter(x -> x.name().equalsIgnoreCase(name)).findFirst().orElse(null);
        if (family == null) {
            reader.setCursor(start);
            throw UNKNOWN_FAMILY.createWithContext(reader, name);
        }

        return family;
    }

    /**
     * Suggest completions for a selector argument.
     *
     * @param context The current command context.
     * @param builder The builder containing the current input.
     * @return The possible suggestions.
     */
    public static CompletableFuture<Suggestions> suggest(CommandContext<?> context, SuggestionsBuilder builder) {
        var remaining = builder.getRemaining();

        if (remaining.startsWith("@")) {
            var reader = new StringReader(builder.getInput());
            reader.setCursor(builder.getStart());
            return suggestSelector(context, reader);
        } else if (remaining.startsWith("#")) {
            return suggestComputers(c -> "#" + c.getID()).suggest(context, builder);
        } else {
            return suggestComputers(c -> Integer.toString(c.getInstanceID())).suggest(context, builder);
        }
    }

    private static CompletableFuture<Suggestions> suggestSelector(CommandContext<?> context, StringReader reader) {
        Set<Option> seenOptions = new HashSet<>();
        var builder = new Builder();

        if (!consume(reader, "@c[")) return suggestions(reader).suggest("@c[").buildFuture();

        while (true) {
            reader.skipWhitespace();

            if (!reader.canRead()) return suggestOptions(reader);
            if (consume(reader, ']')) break;

            // Read the option and validate it.
            Option option;
            try {
                option = parseOption(reader, seenOptions);
            } catch (CommandSyntaxException e) {
                return suggestOptions(reader);
            }
            reader.skipWhitespace();
            if (!consume(reader, '=')) return suggestions(reader).suggest("=").buildFuture();
            reader.skipWhitespace();
            try {
                option.parser.parse(reader, builder);
            } catch (CommandSyntaxException e) {
                return option.suggest.suggest(context, suggestions(reader));
            }
            reader.skipWhitespace();

            if (consume(reader, ']')) break;
            if (!consume(reader, ',')) return suggestions(reader).suggest(",").buildFuture();
        }

        return Suggestions.empty();
    }

    private static CompletableFuture<Suggestions> suggestOptions(StringReader reader) {
        return SharedSuggestionProvider.suggest(options().values(), suggestions(reader), Option::name, Option::tooltip);
    }

    private static SuggestionsBuilder suggestions(StringReader reader) {
        return new SuggestionsBuilder(reader.getString(), reader.getCursor());
    }

    private static final class Builder {
        private OptionalInt instanceId = OptionalInt.empty();
        private @Nullable UUID instanceUuid = null;
        private OptionalInt computerId = OptionalInt.empty();
        private @Nullable String label;
        private @Nullable ComputerFamily family;
        private @Nullable AABB bounds;
        private @Nullable MinMaxBounds.Doubles range;
    }

    private static final Map<String, Option> options;

    /**
     * Get a map of individual selector options.
     *
     * @return The available options.
     */
    public static Map<String, Option> options() {
        return options;
    }

    static {
        var optionList = new Option[]{
            new Option(
                "instance",
                (reader, builder) -> builder.instanceUuid = uuidArgument.parse(reader),
                suggestComputers(c -> c.getInstanceUUID().toString())
            ),
            new Option(
                "id",
                (reader, builder) -> builder.computerId = OptionalInt.of(reader.readInt()),
                suggestComputers(c -> Integer.toString(c.getID()))
            ),
            new Option(
                "label",
                (reader, builder) -> builder.label = reader.readQuotedString(),
                suggestComputers(ServerComputer::getLabel)
            ),
            new Option(
                "family",
                (reader, builder) -> builder.family = parseFamily(reader),
                (source, builder) -> SharedSuggestionProvider.suggest(Arrays.stream(ComputerFamily.values()).map(x -> x.name().toLowerCase(Locale.ROOT)), builder)
            ),
            new Option(
                "distance",
                (reader, builder) -> builder.range = MinMaxBounds.Doubles.fromReader(reader),
                (source, builder) -> Suggestions.empty()
            ),
        };

        options = Arrays.stream(optionList).collect(Collectors.toUnmodifiableMap(Option::name, x -> x));
    }

    /**
     * A single option to filter a computer by.
     */
    public static final class Option {
        private final String name;
        private final Parser parser;
        private final SuggestionProvider suggest;
        private final String translationKey;
        private final Message tooltip;


        Option(String name, Parser parser, SuggestionProvider suggest) {
            this.name = name;
            this.parser = parser;
            this.suggest = suggest;
            tooltip = Component.translatable(translationKey = "argument.computercraft.computer." + name);
        }

        /**
         * The name of this selector.
         *
         * @return The selector's name.
         */
        public String name() {
            return name;
        }

        public Message tooltip() {
            return tooltip;
        }

        /**
         * The translation key for this selector.
         *
         * @return The selector's translation key.
         */
        public String translationKey() {
            return translationKey;
        }
    }

    private interface Parser {
        void parse(StringReader reader, Builder builder) throws CommandSyntaxException;
    }

    private interface SuggestionProvider {
        CompletableFuture<Suggestions> suggest(CommandContext<?> source, SuggestionsBuilder builder);
    }

    private static SuggestionProvider suggestComputers(Function<ServerComputer, String> renderer) {
        return (anyContext, builder) -> suggestOnServer(anyContext, context -> {
            var remaining = builder.getRemaining();
            for (var computer : ServerContext.get(context.getSource().getServer()).registry().getComputers()) {
                var converted = renderer.apply(computer);
                if (converted != null && converted.startsWith(remaining)) {
                    builder.suggest(converted);
                }
            }
            return builder.buildFuture();
        });
    }
}
