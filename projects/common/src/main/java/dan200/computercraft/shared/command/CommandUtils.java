// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dan200.computercraft.shared.platform.PlatformHelper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;

import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public final class CommandUtils {
    private CommandUtils() {
    }

    public static boolean isPlayer(CommandSourceStack output) {
        var player = output.getPlayer();
        return player != null && !PlatformHelper.get().isFakePlayer(player);
    }

    @SuppressWarnings("unchecked")
    public static CompletableFuture<Suggestions> suggestOnServer(CommandContext<?> context, Function<CommandContext<CommandSourceStack>, CompletableFuture<Suggestions>> supplier) {
        var source = context.getSource();
        if (!(source instanceof SharedSuggestionProvider shared)) {
            return Suggestions.empty();
        } else if (source instanceof CommandSourceStack) {
            return supplier.apply((CommandContext<CommandSourceStack>) context);
        } else {
            return shared.customSuggestion(context);
        }
    }

    public static <T> CompletableFuture<Suggestions> suggest(SuggestionsBuilder builder, Iterable<T> candidates, Function<T, String> toString) {
        var remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
        for (var choice : candidates) {
            var name = toString.apply(choice);
            if (!name.toLowerCase(Locale.ROOT).startsWith(remaining)) continue;
            builder.suggest(name);
        }

        return builder.buildFuture();
    }

    public static <T> CompletableFuture<Suggestions> suggest(SuggestionsBuilder builder, T[] candidates, Function<T, String> toString) {
        return suggest(builder, Arrays.asList(candidates), toString);
    }
}
