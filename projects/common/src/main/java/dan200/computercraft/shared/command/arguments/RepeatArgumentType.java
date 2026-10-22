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

/**
 * Reads one argument multiple times.
 * <p>
 * Note that this must be the last element in an argument chain: in order to improve the quality of error messages,
 * we will always try to consume another argument while there is input remaining.
 *
 * @param <T> The type of each value returned.
 */
public final class RepeatArgumentType<T> implements ArgumentType<List<T>> {
    private final ArgumentType<T> child;
    private final SimpleCommandExceptionType some;

    private RepeatArgumentType(ArgumentType<T> child, SimpleCommandExceptionType some) {
        this.child = child;
        this.some = some;
    }

    public static <T> RepeatArgumentType<T> some(ArgumentType<T> appender, SimpleCommandExceptionType missing) {
        return new RepeatArgumentType<>(appender, missing);
    }

    @Override
    public List<T> parse(StringReader reader) throws CommandSyntaxException {
        var hadSome = false;
        List<T> out = new ArrayList<>();
        while (true) {
            reader.skipWhitespace();
            if (!reader.canRead()) break;

            var startParse = reader.getCursor();
            out.add(child.parse(reader));
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

    public static <T> ArgumentTypeInfo<RepeatArgumentType<T>, ? extends ArgumentTypeInfo.Template<RepeatArgumentType<T>>> type() {
        return new Info<>();
    }

    private static final class Info<T> implements ArgumentTypeInfo<RepeatArgumentType<T>, Template<T>> {
        @Override
        public void serializeToNetwork(RepeatArgumentType.Template<T> arg, FriendlyByteBuf buf) {
            ArgumentUtils.serializeToNetwork(buf, arg.child);
            buf.writeComponent(ArgumentUtils.getMessage(arg.some));
        }

        @Override
        public RepeatArgumentType.Template<T> deserializeFromNetwork(FriendlyByteBuf buf) {
            @SuppressWarnings("unchecked")
            var child = (ArgumentTypeInfo.Template<ArgumentType<T>>) ArgumentUtils.deserialize(buf);

            var message = buf.readComponent();
            return new RepeatArgumentType.Template<>(this, child, new SimpleCommandExceptionType(message));
        }

        @Override
        public RepeatArgumentType.Template<T> unpack(RepeatArgumentType<T> argumentType) {
            return new RepeatArgumentType.Template<T>(this, ArgumentTypeInfos.unpack(argumentType.child), argumentType.some);
        }

        @Override
        public void serializeToJson(RepeatArgumentType.Template<T> arg, JsonObject json) {
            json.add("child", ArgumentUtils.serializeToJson(arg.child));
            json.addProperty("error", Component.Serializer.toJson(ArgumentUtils.getMessage(arg.some)));
        }
    }

    private record Template<T>(
        Info<T> info, ArgumentTypeInfo.Template<ArgumentType<T>> child, SimpleCommandExceptionType some
    ) implements ArgumentTypeInfo.Template<RepeatArgumentType<T>> {
        @Override
        public RepeatArgumentType<T> instantiate(CommandBuildContext commandBuildContext) {
            var child = child().instantiate(commandBuildContext);
            return RepeatArgumentType.some(child, some());
        }

        @Override
        public ArgumentTypeInfo<RepeatArgumentType<T>, ?> type() {
            return info;
        }
    }
}
