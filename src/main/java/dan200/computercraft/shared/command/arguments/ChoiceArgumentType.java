/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
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

public abstract class ChoiceArgumentType<T> implements ArgumentType<T>
{
    private final Iterable<T> choices;
    private final Function<T, String> name;
    private final Function<T, Message> tooltip;
    private final DynamicCommandExceptionType exception;

    protected ChoiceArgumentType( Iterable<T> choices, Function<T, String> name, Function<T, Message> tooltip, DynamicCommandExceptionType exception )
    {
        this.choices = choices;
        this.name = name;
        this.tooltip = tooltip;
        this.exception = exception;
    }

    @Override
    public T parse( StringReader reader ) throws CommandSyntaxException
    {
        int start = reader.getCursor();
        String name = reader.readUnquotedString();

        for( T choice : choices )
        {
            String choiceName = this.name.apply( choice );
            if( name.equals( choiceName ) ) return choice;
        }

        reader.setCursor( start );
        throw exception.createWithContext( reader, name );
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions( CommandContext<S> context, SuggestionsBuilder builder )
    {
        String remaining = builder.getRemaining().toLowerCase( Locale.ROOT );
        for( T choice : choices )
        {
            String name = this.name.apply( choice );
            if( !name.toLowerCase( Locale.ROOT ).startsWith( remaining ) ) continue;
            builder.suggest( name, tooltip.apply( choice ) );
        }

        return builder.buildFuture();
    }

    @Override
    public Collection<String> getExamples()
    {
        List<String> items = choices instanceof Collection<?> ? new ArrayList<>( ((Collection<T>) choices).size() ) : new ArrayList<>();
        for( T choice : choices ) items.add( name.apply( choice ) );
        items.sort( Comparator.naturalOrder() );
        return items;
    }
}
