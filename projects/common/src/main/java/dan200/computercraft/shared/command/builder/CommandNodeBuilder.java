// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.command.builder;

import com.mojang.brigadier.tree.CommandNode;

/**
 * A builder which generates a {@link CommandNode} from the provided action.
 *
 * @param <S> The command source we consume.
 * @param <T> The type of action to execute when this command is run.
 */
@FunctionalInterface
public interface CommandNodeBuilder<S, T> {
    /**
     * Generate a command node which executes this command.
     *
     * @param command The command to run
     * @return The constructed node.
     */
    CommandNode<S> executes(T command);
}
