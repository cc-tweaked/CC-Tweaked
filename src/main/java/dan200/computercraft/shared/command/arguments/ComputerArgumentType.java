/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.command.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dan200.computercraft.shared.command.arguments.ComputersArgumentType.ComputersSupplier;
import dan200.computercraft.shared.computer.core.ServerComputer;
import net.minecraft.command.CommandSource;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import static dan200.computercraft.shared.command.Exceptions.COMPUTER_SELECTOR_MANY;

public class ComputerArgumentType implements ArgumentType<ComputerArgumentType.ComputerSupplier>
{
    private static final ComputerArgumentType INSTANCE = new ComputerArgumentType();

    public static ComputerArgumentType oneComputer()
    {
        return INSTANCE;
    }

    public static ServerComputer getComputerArgument( CommandContext<CommandSource> context, String name ) throws CommandSyntaxException
    {
        return context.getArgument( name, ComputerSupplier.class ).unwrap( context.getSource() );
    }

    private ComputerArgumentType()
    {
    }

    @Override
    public ComputerSupplier parse( StringReader reader ) throws CommandSyntaxException
    {
        int start = reader.getCursor();
        ComputersSupplier supplier = ComputersArgumentType.someComputers().parse( reader );
        String selector = reader.getString().substring( start, reader.getCursor() );

        return s -> {
            Collection<ServerComputer> computers = supplier.unwrap( s );

            if( computers.size() == 1 ) return computers.iterator().next();

            // We have an incorrect number of computers: reset and throw an error
            reader.setCursor( start );
            throw COMPUTER_SELECTOR_MANY.createWithContext( reader, selector );
        };
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions( CommandContext<S> context, SuggestionsBuilder builder )
    {
        return ComputersArgumentType.someComputers().listSuggestions( context, builder );
    }

    @Override
    public Collection<String> getExamples()
    {
        return ComputersArgumentType.someComputers().getExamples();
    }

    @FunctionalInterface
    public interface ComputerSupplier
    {
        ServerComputer unwrap( CommandSource source ) throws CommandSyntaxException;
    }
}
