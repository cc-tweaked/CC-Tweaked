/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.command.builder;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.command.CommandSource;
import net.minecraft.util.text.TextComponentString;

import java.util.concurrent.CompletableFuture;

/**
 * An alternative to {@link LiteralArgumentBuilder} which provides a tooltip in the suggestions
 *
 * Note, this doesn't actually work currently (well, it doesn't display a tooltip), but it's a nice idea
 * and means the data is there.
 */
public class DescribedArgumentBuilder<S> extends LiteralArgumentBuilder<S>
{
    private final Message tooltip;

    private DescribedArgumentBuilder( String literal, Message tooltip )
    {
        super( literal );
        this.tooltip = tooltip;
    }

    public static DescribedArgumentBuilder<CommandSource> literal( String literal, Message tooltip )
    {
        return new DescribedArgumentBuilder<>( literal, tooltip );
    }

    public static DescribedArgumentBuilder<CommandSource> literal( String literal, String tooltip )
    {
        return new DescribedArgumentBuilder<>( literal, new TextComponentString( tooltip ) );
    }

    @Override
    public LiteralCommandNode<S> build()
    {
        LiteralCommandNode<S> result = new LiteralCommandNode<S>( getLiteral(), getCommand(), getRequirement(), getRedirect(), getRedirectModifier(), isFork() )
        {
            @Override
            public CompletableFuture<Suggestions> listSuggestions( CommandContext<S> context, SuggestionsBuilder builder )
            {
                return getLiteral().toLowerCase().startsWith( builder.getRemaining().toLowerCase() )
                    ? builder.suggest( getLiteral(), tooltip ).buildFuture()
                    : Suggestions.empty();
            }
        };
        for( CommandNode<S> node : getArguments() ) result.addChild( node );

        return result;
    }
}
