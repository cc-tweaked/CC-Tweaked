// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.command.builder;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static dan200.computercraft.core.util.Nullability.assertNonNull;
import static dan200.computercraft.shared.command.text.ChatHelpers.coloured;

/**
 * An alternative to {@link LiteralArgumentBuilder} which also provides a {@code /... help} command, and defaults
 * to that command when no arguments are given.
 */
public final class HelpingArgumentBuilder extends LiteralArgumentBuilder<CommandSourceStack> {
    private final Collection<HelpingArgumentBuilder> children = new ArrayList<>();
    private @Nullable Predicate<CommandSourceStack> requirement;

    private HelpingArgumentBuilder(String literal) {
        super(literal);
    }

    public static HelpingArgumentBuilder choice(String literal) {
        return new HelpingArgumentBuilder(literal);
    }

    @Override
    public HelpingArgumentBuilder requires(Predicate<CommandSourceStack> requirement) {
        this.requirement = requirement;
        return this;
    }

    @Override
    public Predicate<CommandSourceStack> getRequirement() {
        if (requirement != null) return requirement;

        var requirements = Stream.concat(
            children.stream().map(ArgumentBuilder::getRequirement),
            getArguments().stream().map(CommandNode::getRequirement)
        ).toList();
        return x -> requirements.stream().anyMatch(y -> y.test(x));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> executes(final Command<CommandSourceStack> command) {
        throw new IllegalStateException("Cannot use executes on a HelpingArgumentBuilder");
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> then(final ArgumentBuilder<CommandSourceStack, ?> argument) {
        if (getRedirect() != null) throw new IllegalStateException("Cannot add children to a redirected node");

        if (argument instanceof HelpingArgumentBuilder) {
            children.add((HelpingArgumentBuilder) argument);
        } else if (argument instanceof LiteralArgumentBuilder) {
            super.then(argument);
        } else {
            throw new IllegalStateException("HelpingArgumentBuilder can only accept literal children");
        }

        return this;
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> then(CommandNode<CommandSourceStack> argument) {
        if (!(argument instanceof LiteralCommandNode)) {
            throw new IllegalStateException("HelpingArgumentBuilder can only accept literal children");
        }
        return super.then(argument);
    }

    @Override
    public LiteralCommandNode<CommandSourceStack> build() {
        return buildImpl(getLiteral().replace('-', '_'), getLiteral());
    }

    private LiteralCommandNode<CommandSourceStack> build(String id, String command) {
        return buildImpl(id + "." + getLiteral().replace('-', '_'), command + " " + getLiteral());
    }

    private LiteralCommandNode<CommandSourceStack> buildImpl(String id, String command) {
        var helpCommand = new HelpCommand(id, command);
        var node = new LiteralCommandNode<>(getLiteral(), helpCommand, getRequirement(), getRedirect(), getRedirectModifier(), isFork());
        helpCommand.node = node;

        // Set up a /... help command
        var helpNode = LiteralArgumentBuilder.<CommandSourceStack>literal("help").executes(helpCommand);

        // Add all normal command children to this and the help node
        for (var child : getArguments()) {
            node.addChild(child);

            helpNode.then(LiteralArgumentBuilder.<CommandSourceStack>literal(child.getName())
                .requires(child.getRequirement())
                .executes(helpForChild(child, id, command))
                .build()
            );
        }

        // And add alternative versions of which forward instead
        for (var childBuilder : children) {
            var child = childBuilder.build(id, command);
            node.addChild(child);
            helpNode.then(LiteralArgumentBuilder.<CommandSourceStack>literal(child.getName())
                .requires(child.getRequirement())
                .executes(helpForChild(child, id, command))
                .redirect(child.getChild("help"))
                .build()
            );
        }

        node.addChild(helpNode.build());

        return node;
    }

    private static final ChatFormatting HEADER = ChatFormatting.LIGHT_PURPLE;
    private static final ChatFormatting SYNOPSIS = ChatFormatting.AQUA;
    private static final ChatFormatting NAME = ChatFormatting.GREEN;

    private static final class HelpCommand implements Command<CommandSourceStack> {
        private final String id;
        private final String command;
        @Nullable
        LiteralCommandNode<CommandSourceStack> node;

        private HelpCommand(String id, String command) {
            this.id = id;
            this.command = command;
        }

        @Override
        public int run(CommandContext<CommandSourceStack> context) {
            context.getSource().sendSuccess(() -> getHelp(context, assertNonNull(node), id, command), false);
            return 0;
        }
    }

    private static Command<CommandSourceStack> helpForChild(CommandNode<CommandSourceStack> node, String id, String command) {
        return context -> {
            context.getSource().sendSuccess(() -> getHelp(context, node, id + "." + node.getName().replace('-', '_'), command + " " + node.getName()), false);
            return 0;
        };
    }

    private static Component getHelp(CommandContext<CommandSourceStack> context, CommandNode<CommandSourceStack> node, String id, String command) {
        // An ugly hack to extract usage information from the dispatcher. We generate a temporary node, generate
        // the shorthand usage, and emit that.
        var dispatcher = context.getSource().getServer().getCommands().getDispatcher();
        CommandNode<CommandSourceStack> temp = new LiteralCommandNode<>("_", null, x -> true, null, null, false);
        temp.addChild(node);
        var usage = assertNonNull(dispatcher.getSmartUsage(temp, context.getSource()).get(node)).substring(node.getName().length());

        var output = Component.literal("")
            .append(coloured("/" + command + usage, HEADER))
            .append(" ")
            .append(Component.translatable("commands." + id + ".synopsis").withStyle(SYNOPSIS))
            .append("\n")
            .append(Component.translatable("commands." + id + ".desc"));

        for (var child : node.getChildren()) {
            if (!child.canUse(context.getSource()) || !(child instanceof LiteralCommandNode)) {
                continue;
            }

            output.append("\n");

            var component = coloured(child.getName(), NAME);
            component.getStyle().withClickEvent(new ClickEvent(
                ClickEvent.Action.SUGGEST_COMMAND,
                "/" + command + " " + child.getName()
            ));
            output.append(component);

            output.append(" - ").append(Component.translatable("commands." + id + "." + child.getName() + ".synopsis"));
        }

        return output;
    }
}
