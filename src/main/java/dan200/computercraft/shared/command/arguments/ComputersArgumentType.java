/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.command.arguments;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.synchronization.ArgumentSerializer;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static dan200.computercraft.shared.command.CommandUtils.suggest;
import static dan200.computercraft.shared.command.CommandUtils.suggestOnServer;
import static dan200.computercraft.shared.command.Exceptions.COMPUTER_ARG_NONE;

public final class ComputersArgumentType implements ArgumentType<ComputersArgumentType.ComputersSupplier>
{
    private static final ComputersArgumentType MANY = new ComputersArgumentType( false );
    private static final ComputersArgumentType SOME = new ComputersArgumentType( true );

    private static final List<String> EXAMPLES = Arrays.asList(
        "0", "#0", "@Label", "~Advanced"
    );

    public static ComputersArgumentType manyComputers()
    {
        return MANY;
    }

    public static ComputersArgumentType someComputers()
    {
        return SOME;
    }

    public static Collection<ServerComputer> getComputersArgument( CommandContext<CommandSourceStack> context, String name ) throws CommandSyntaxException
    {
        return context.getArgument( name, ComputersSupplier.class ).unwrap( context.getSource() );
    }

    private final boolean requireSome;

    private ComputersArgumentType( boolean requireSome )
    {
        this.requireSome = requireSome;
    }

    @Override
    public ComputersSupplier parse( StringReader reader ) throws CommandSyntaxException
    {
        int start = reader.getCursor();
        char kind = reader.peek();
        ComputersSupplier computers;
        if( kind == '@' )
        {
            reader.skip();
            String label = reader.readUnquotedString();
            computers = getComputers( x -> Objects.equals( label, x.getLabel() ) );
        }
        else if( kind == '~' )
        {
            reader.skip();
            String family = reader.readUnquotedString();
            computers = getComputers( x -> x.getFamily().name().equalsIgnoreCase( family ) );
        }
        else if( kind == '#' )
        {
            reader.skip();
            int id = reader.readInt();
            computers = getComputers( x -> x.getID() == id );
        }
        else
        {
            int instance = reader.readInt();
            computers = s -> {
                ServerComputer computer = ComputerCraft.serverComputerRegistry.get( instance );
                return computer == null ? Collections.emptyList() : Collections.singletonList( computer );
            };
        }

        if( requireSome )
        {
            String selector = reader.getString().substring( start, reader.getCursor() );
            return source -> {
                Collection<ServerComputer> matched = computers.unwrap( source );
                if( matched.isEmpty() ) throw COMPUTER_ARG_NONE.create( selector );
                return matched;
            };
        }
        else
        {
            return computers;
        }
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions( CommandContext<S> context, SuggestionsBuilder builder )
    {
        String remaining = builder.getRemaining();

        // We can run this one on the client, for obvious reasons.
        if( remaining.startsWith( "~" ) )
        {
            return suggest( builder, ComputerFamily.values(), x -> "~" + x.name() );
        }

        // Verify we've a command source and we're running on the server
        return suggestOnServer( context, s -> {
            if( remaining.startsWith( "@" ) )
            {
                suggestComputers( builder, remaining, x -> {
                    String label = x.getLabel();
                    return label == null ? null : "@" + label;
                } );
            }
            else if( remaining.startsWith( "#" ) )
            {
                suggestComputers( builder, remaining, c -> "#" + c.getID() );
            }
            else
            {
                suggestComputers( builder, remaining, c -> Integer.toString( c.getInstanceID() ) );
            }

            return builder.buildFuture();
        } );
    }

    @Override
    public Collection<String> getExamples()
    {
        return EXAMPLES;
    }

    private static void suggestComputers( SuggestionsBuilder builder, String remaining, Function<ServerComputer, String> renderer )
    {
        remaining = remaining.toLowerCase( Locale.ROOT );
        for( ServerComputer computer : ComputerCraft.serverComputerRegistry.getComputers() )
        {
            String converted = renderer.apply( computer );
            if( converted != null && converted.toLowerCase( Locale.ROOT ).startsWith( remaining ) )
            {
                builder.suggest( converted );
            }
        }
    }

    private static ComputersSupplier getComputers( Predicate<ServerComputer> predicate )
    {
        return s -> Collections.unmodifiableList( ComputerCraft.serverComputerRegistry
            .getComputers()
            .stream()
            .filter( predicate )
            .collect( Collectors.toList() )
        );
    }

    public static class Serializer implements ArgumentSerializer<ComputersArgumentType>
    {

        @Override
        public void serializeToNetwork( @Nonnull ComputersArgumentType arg, @Nonnull FriendlyByteBuf buf )
        {
            buf.writeBoolean( arg.requireSome );
        }

        @Nonnull
        @Override
        public ComputersArgumentType deserializeFromNetwork( @Nonnull FriendlyByteBuf buf )
        {
            return buf.readBoolean() ? SOME : MANY;
        }

        @Override
        public void serializeToJson( @Nonnull ComputersArgumentType arg, @Nonnull JsonObject json )
        {
            json.addProperty( "requireSome", arg.requireSome );
        }
    }

    @FunctionalInterface
    public interface ComputersSupplier
    {
        Collection<ServerComputer> unwrap( CommandSourceStack source ) throws CommandSyntaxException;
    }

    public static Set<ServerComputer> unwrap( CommandSourceStack source, Collection<ComputersSupplier> suppliers ) throws CommandSyntaxException
    {
        Set<ServerComputer> computers = new HashSet<>();
        for( ComputersSupplier supplier : suppliers ) computers.addAll( supplier.unwrap( source ) );
        return computers;
    }
}
