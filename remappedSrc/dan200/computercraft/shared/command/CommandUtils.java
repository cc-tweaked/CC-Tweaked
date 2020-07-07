/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dan200.computercraft.api.turtle.event.FakePlayer;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public final class CommandUtils
{
    private CommandUtils() {}

    public static boolean isPlayer( ServerCommandSource output )
    {
        Entity sender = output.getEntity();
        return sender instanceof ServerPlayerEntity
            && !(sender instanceof FakePlayer)
            && ((ServerPlayerEntity) sender).networkHandler != null;
    }

    @SuppressWarnings( "unchecked" )
    public static CompletableFuture<Suggestions> suggestOnServer( CommandContext<?> context, SuggestionsBuilder builder, Function<CommandContext<ServerCommandSource>, CompletableFuture<Suggestions>> supplier )
    {
        Object source = context.getSource();
        if( !(source instanceof CommandSource) )
        {
            return Suggestions.empty();
        }
        else if( source instanceof ServerCommandSource )
        {
            return supplier.apply( (CommandContext<ServerCommandSource>) context );
        }
        else
        {
            return ((CommandSource) source).getCompletions( (CommandContext<CommandSource>) context, builder );
        }
    }

    public static <T> CompletableFuture<Suggestions> suggest( SuggestionsBuilder builder, Iterable<T> candidates, Function<T, String> toString )
    {
        String remaining = builder.getRemaining().toLowerCase( Locale.ROOT );
        for( T choice : candidates )
        {
            String name = toString.apply( choice );
            if( !name.toLowerCase( Locale.ROOT ).startsWith( remaining ) ) continue;
            builder.suggest( name );
        }

        return builder.buildFuture();
    }

    public static <T> CompletableFuture<Suggestions> suggest( SuggestionsBuilder builder, T[] candidates, Function<T, String> toString )
    {
        return suggest( builder, Arrays.asList( candidates ), toString );
    }
}
