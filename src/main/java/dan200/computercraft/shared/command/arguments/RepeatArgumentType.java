/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.command.arguments;

import com.google.gson.JsonObject;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.synchronization.ArgumentSerializer;
import net.minecraft.commands.synchronization.ArgumentTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * Reads one argument multiple times.
 *
 * Note that this must be the last element in an argument chain: in order to improve the quality of error messages,
 * we will always try to consume another argument while there is input remaining.
 *
 * One problem with how parsers function, is that they must consume some input: and thus we
 *
 * @param <T> The type of each value returned
 * @param <U> The type of the inner parser. This will normally be a {@link List} or {@code T}.
 */
public final class RepeatArgumentType<T, U> implements ArgumentType<List<T>>
{
    private final ArgumentType<U> child;
    private final BiConsumer<List<T>, U> appender;
    private final boolean flatten;
    private final SimpleCommandExceptionType some;

    private RepeatArgumentType( ArgumentType<U> child, BiConsumer<List<T>, U> appender, boolean flatten, SimpleCommandExceptionType some )
    {
        this.child = child;
        this.appender = appender;
        this.flatten = flatten;
        this.some = some;
    }

    public static <T> RepeatArgumentType<T, T> some( ArgumentType<T> appender, SimpleCommandExceptionType missing )
    {
        return new RepeatArgumentType<>( appender, List::add, false, missing );
    }

    public static <T> RepeatArgumentType<T, List<T>> someFlat( ArgumentType<List<T>> appender, SimpleCommandExceptionType missing )
    {
        return new RepeatArgumentType<>( appender, List::addAll, true, missing );
    }

    @Override
    public List<T> parse( StringReader reader ) throws CommandSyntaxException
    {
        boolean hadSome = false;
        List<T> out = new ArrayList<>();
        while( true )
        {
            reader.skipWhitespace();
            if( !reader.canRead() ) break;

            int startParse = reader.getCursor();
            appender.accept( out, child.parse( reader ) );
            hadSome = true;

            if( reader.getCursor() == startParse )
            {
                throw new IllegalStateException( child + " did not consume any input on " + reader.getRemaining() );
            }
        }

        // Note that each child may return an empty list, we just require that some actual input
        // was consumed.
        // We should probably review that this is sensible in the future.
        if( !hadSome ) throw some.createWithContext( reader );

        return Collections.unmodifiableList( out );
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions( CommandContext<S> context, SuggestionsBuilder builder )
    {
        StringReader reader = new StringReader( builder.getInput() );
        reader.setCursor( builder.getStart() );
        int previous = reader.getCursor();
        while( reader.canRead() )
        {
            try
            {
                child.parse( reader );
            }
            catch( CommandSyntaxException e )
            {
                break;
            }

            int cursor = reader.getCursor();
            reader.skipWhitespace();
            if( cursor == reader.getCursor() ) break;
            previous = reader.getCursor();
        }

        reader.setCursor( previous );
        return child.listSuggestions( context, builder.createOffset( previous ) );
    }

    @Override
    public Collection<String> getExamples()
    {
        return child.getExamples();
    }

    public static class Serializer implements ArgumentSerializer<RepeatArgumentType<?, ?>>
    {
        @Override
        public void serializeToNetwork( @Nonnull RepeatArgumentType<?, ?> arg, @Nonnull FriendlyByteBuf buf )
        {
            buf.writeBoolean( arg.flatten );
            ArgumentTypes.serialize( buf, arg.child );
            buf.writeComponent( getMessage( arg ) );
        }

        @Nonnull
        @Override
        @SuppressWarnings( { "unchecked", "rawtypes" } )
        public RepeatArgumentType<?, ?> deserializeFromNetwork( @Nonnull FriendlyByteBuf buf )
        {
            boolean isList = buf.readBoolean();
            ArgumentType<?> child = ArgumentTypes.deserialize( buf );
            Component message = buf.readComponent();
            BiConsumer<List<Object>, ?> appender = isList ? ( list, x ) -> list.addAll( (Collection) x ) : List::add;
            return new RepeatArgumentType( child, appender, isList, new SimpleCommandExceptionType( message ) );
        }

        @Override
        public void serializeToJson( @Nonnull RepeatArgumentType<?, ?> arg, @Nonnull JsonObject json )
        {
            json.addProperty( "flatten", arg.flatten );
            json.addProperty( "child", "<<cannot serialize>>" ); // TODO: Potentially serialize this using reflection.
            json.addProperty( "error", Component.Serializer.toJson( getMessage( arg ) ) );
        }

        private static Component getMessage( RepeatArgumentType<?, ?> arg )
        {
            Message message = arg.some.create().getRawMessage();
            if( message instanceof Component ) return (Component) message;
            return new TextComponent( message.getString() );
        }
    }
}
