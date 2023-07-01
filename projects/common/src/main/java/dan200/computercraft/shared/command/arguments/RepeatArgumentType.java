// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.command.arguments;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * Reads one argument multiple times.
 * <p>
 * Note that this must be the last element in an argument chain: in order to improve the quality of error messages,
 * we will always try to consume another argument while there is input remaining.
 * <p>
 * One problem with how parsers function, is that they must consume some input: and thus we
 *
 * @param <T> The type of each value returned
 * @param <U> The type of the inner parser. This will normally be a {@link List} or {@code T}.
 */
public final class RepeatArgumentType<T, U> implements ArgumentType<List<T>> {
    private final ArgumentType<U> child;
    private final BiConsumer<List<T>, U> appender;
    private final boolean flatten;
    private final SimpleCommandExceptionType some;

    private RepeatArgumentType(ArgumentType<U> child, BiConsumer<List<T>, U> appender, boolean flatten, SimpleCommandExceptionType some) {
        this.child = child;
        this.appender = appender;
        this.flatten = flatten;
        this.some = some;
    }

    public static <T> RepeatArgumentType<T, T> some(ArgumentType<T> appender, SimpleCommandExceptionType missing) {
        return new RepeatArgumentType<>(appender, List::add, false, missing);
    }

    public static <T> RepeatArgumentType<T, List<T>> someFlat(ArgumentType<List<T>> appender, SimpleCommandExceptionType missing) {
        return new RepeatArgumentType<>(appender, List::addAll, true, missing);
    }

    @Override
    public List<T> parse(StringReader reader) throws CommandSyntaxException {
        var hadSome = false;
        List<T> out = new ArrayList<>();
        while (true) {
            reader.skipWhitespace();
            if (!reader.canRead()) break;

            var startParse = reader.getCursor();
            appender.accept(out, child.parse(reader));
            hadSome = true;

            if (reader.getCursor() == startParse) {
                throw new IllegalStateException(child + " did not consume any input on " + reader.getRemaining());
            }
        }

        // Note that each child may return an empty list, we just require that some actual input
        // was consumed.
        // We should probably review that this is sensible in the future.
        if (!hadSome) throw some.createWithContext(reader);

        return Collections.unmodifiableList(out);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        var reader = new StringReader(builder.getInput());
        reader.setCursor(builder.getStart());
        var previous = reader.getCursor();
        while (reader.canRead()) {
            try {
                child.parse(reader);
            } catch (CommandSyntaxException e) {
                break;
            }

            var cursor = reader.getCursor();
            reader.skipWhitespace();
            if (cursor == reader.getCursor()) break;
            previous = reader.getCursor();
        }

        reader.setCursor(previous);
        return child.listSuggestions(context, builder.createOffset(previous));
    }

    @Override
    public Collection<String> getExamples() {
        return child.getExamples();
    }

    public static class Info implements ArgumentTypeInfo<RepeatArgumentType<?, ?>, Template> {
        @Override
        public void serializeToNetwork(RepeatArgumentType.Template arg, FriendlyByteBuf buf) {
            buf.writeBoolean(arg.flatten);
            ArgumentUtils.serializeToNetwork(buf, arg.child);
            buf.writeComponent(ArgumentUtils.getMessage(arg.some));
        }

        @Override
        public RepeatArgumentType.Template deserializeFromNetwork(FriendlyByteBuf buf) {
            var isList = buf.readBoolean();
            var child = ArgumentUtils.deserialize(buf);
            var message = buf.readComponent();
            return new RepeatArgumentType.Template(this, child, isList, new SimpleCommandExceptionType(message));
        }

        @Override
        public RepeatArgumentType.Template unpack(RepeatArgumentType<?, ?> argumentType) {
            return new RepeatArgumentType.Template(this, ArgumentTypeInfos.unpack(argumentType.child), argumentType.flatten, argumentType.some);
        }

        @Override
        public void serializeToJson(RepeatArgumentType.Template arg, JsonObject json) {
            json.addProperty("flatten", arg.flatten);
            json.add("child", ArgumentUtils.serializeToJson(arg.child));
            json.addProperty("error", Component.Serializer.toJson(ArgumentUtils.getMessage(arg.some)));
        }
    }

    public record Template(
        Info info, ArgumentTypeInfo.Template<?> child, boolean flatten, SimpleCommandExceptionType some
    ) implements ArgumentTypeInfo.Template<RepeatArgumentType<?, ?>> {
        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public RepeatArgumentType<?, ?> instantiate(CommandBuildContext commandBuildContext) {
            var child = child().instantiate(commandBuildContext);
            return flatten ? RepeatArgumentType.someFlat((ArgumentType) child, some()) : RepeatArgumentType.some(child, some());
        }

        @Override
        public ArgumentTypeInfo<RepeatArgumentType<?, ?>, ?> type() {
            return info;
        }
    }
}
