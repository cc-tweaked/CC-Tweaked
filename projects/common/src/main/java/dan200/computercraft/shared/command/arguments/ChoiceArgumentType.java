// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.command.arguments;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public abstract class ChoiceArgumentType<T> implements ArgumentType<T> {
    private final Iterable<T> choices;
    private final Function<T, String> name;
    private final Function<T, Message> tooltip;
    private final DynamicCommandExceptionType exception;

    protected ChoiceArgumentType(Iterable<T> choices, Function<T, String> name, Function<T, Message> tooltip, DynamicCommandExceptionType exception) {
        this.choices = choices;
        this.name = name;
        this.tooltip = tooltip;
        this.exception = exception;
    }

    @Override
    public T parse(StringReader reader) throws CommandSyntaxException {
        var start = reader.getCursor();
        var name = reader.readUnquotedString();

        for (var choice : choices) {
            var choiceName = this.name.apply(choice);
            if (name.equals(choiceName)) return choice;
        }

        reader.setCursor(start);
        throw exception.createWithContext(reader, name);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        var remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
        for (var choice : choices) {
            var name = this.name.apply(choice);
            if (!name.toLowerCase(Locale.ROOT).startsWith(remaining)) continue;
            builder.suggest(name, tooltip.apply(choice));
        }

        return builder.buildFuture();
    }

    @Override
    public Collection<String> getExamples() {
        List<String> items = choices instanceof Collection<?> ? new ArrayList<>(((Collection<T>) choices).size()) : new ArrayList<>();
        for (var choice : choices) items.add(name.apply(choice));
        items.sort(Comparator.naturalOrder());
        return items;
    }
}
