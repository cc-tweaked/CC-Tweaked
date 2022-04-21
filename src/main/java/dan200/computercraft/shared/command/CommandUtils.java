/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.util.FakePlayer;

import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public final class CommandUtils
{
    private CommandUtils() {}

    public static boolean isPlayer( CommandSourceStack output )
    {
        Entity sender = output.getEntity();
        return sender instanceof ServerPlayer
            && !(sender instanceof FakePlayer)
            && ((ServerPlayer) sender).connection != null;
    }

    @SuppressWarnings( "unchecked" )
    public static CompletableFuture<Suggestions> suggestOnServer( CommandContext<?> context, Function<CommandContext<CommandSourceStack>, CompletableFuture<Suggestions>> supplier )
    {
        Object source = context.getSource();
        if( !(source instanceof SharedSuggestionProvider) )
        {
            return Suggestions.empty();
        }
        else if( source instanceof CommandSourceStack )
        {
            return supplier.apply( (CommandContext<CommandSourceStack>) context );
        }
        else
        {
            return ((SharedSuggestionProvider) source).customSuggestion( context );
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
