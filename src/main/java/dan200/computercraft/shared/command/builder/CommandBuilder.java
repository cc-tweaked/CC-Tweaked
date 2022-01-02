/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.command.builder;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.CommandNode;
import dan200.computercraft.shared.command.arguments.RepeatArgumentType;
import net.minecraft.commands.CommandSourceStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static dan200.computercraft.shared.command.Exceptions.ARGUMENT_EXPECTED;
import static dan200.computercraft.shared.command.builder.HelpingArgumentBuilder.literal;

/**
 * An alternative way of building command nodes, so one does not have to nest.
 * {@link ArgumentBuilder#then(CommandNode)}s.
 *
 * @param <S> The command source we consume.
 */
public class CommandBuilder<S> implements CommandNodeBuilder<S, Command<S>>
{
    private final List<ArgumentBuilder<S, ?>> args = new ArrayList<>();
    private Predicate<S> requires;

    public static CommandBuilder<CommandSourceStack> args()
    {
        return new CommandBuilder<>();
    }

    public static CommandBuilder<CommandSourceStack> command( String literal )
    {
        CommandBuilder<CommandSourceStack> builder = new CommandBuilder<>();
        builder.args.add( literal( literal ) );
        return builder;
    }

    public CommandBuilder<S> requires( Predicate<S> predicate )
    {
        requires = requires == null ? predicate : requires.and( predicate );
        return this;
    }

    public CommandBuilder<S> arg( String name, ArgumentType<?> type )
    {
        args.add( RequiredArgumentBuilder.argument( name, type ) );
        return this;
    }

    public <T> CommandNodeBuilder<S, ArgCommand<S, List<T>>> argManyValue( String name, ArgumentType<T> type, List<T> empty )
    {
        return argMany( name, type, () -> empty );
    }

    public <T> CommandNodeBuilder<S, ArgCommand<S, List<T>>> argManyValue( String name, ArgumentType<T> type, T defaultValue )
    {
        return argManyValue( name, type, Collections.singletonList( defaultValue ) );
    }

    public <T> CommandNodeBuilder<S, ArgCommand<S, List<T>>> argMany( String name, ArgumentType<T> type, Supplier<List<T>> empty )
    {
        return argMany( name, RepeatArgumentType.some( type, ARGUMENT_EXPECTED ), empty );
    }

    public <T> CommandNodeBuilder<S, ArgCommand<S, List<T>>> argManyFlatten( String name, ArgumentType<List<T>> type, Supplier<List<T>> empty )
    {
        return argMany( name, RepeatArgumentType.someFlat( type, ARGUMENT_EXPECTED ), empty );
    }

    private <T, U> CommandNodeBuilder<S, ArgCommand<S, List<T>>> argMany( String name, RepeatArgumentType<T, ?> type, Supplier<List<T>> empty )
    {
        if( args.isEmpty() ) throw new IllegalStateException( "Cannot have empty arg chain builder" );

        return command -> {
            // The node for no arguments
            ArgumentBuilder<S, ?> tail = tail( ctx -> command.run( ctx, empty.get() ) );

            // The node for one or more arguments
            ArgumentBuilder<S, ?> moreArg = RequiredArgumentBuilder
                .<S, List<T>>argument( name, type )
                .executes( ctx -> command.run( ctx, getList( ctx, name ) ) );

            // Chain all of them together!
            tail.then( moreArg );
            return link( tail );
        };
    }

    @SuppressWarnings( "unchecked" )
    private static <T> List<T> getList( CommandContext<?> context, String name )
    {
        return (List<T>) context.getArgument( name, List.class );
    }

    @Override
    public CommandNode<S> executes( Command<S> command )
    {
        if( args.isEmpty() ) throw new IllegalStateException( "Cannot have empty arg chain builder" );

        return link( tail( command ) );
    }

    private ArgumentBuilder<S, ?> tail( Command<S> command )
    {
        ArgumentBuilder<S, ?> defaultTail = args.get( args.size() - 1 );
        defaultTail.executes( command );
        if( requires != null ) defaultTail.requires( requires );
        return defaultTail;
    }

    private CommandNode<S> link( ArgumentBuilder<S, ?> tail )
    {
        for( int i = args.size() - 2; i >= 0; i-- ) tail = args.get( i ).then( tail );
        return tail.build();
    }
}
