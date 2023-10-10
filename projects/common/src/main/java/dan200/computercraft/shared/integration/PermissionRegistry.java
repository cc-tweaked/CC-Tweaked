// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.integration;

import com.mojang.brigadier.builder.ArgumentBuilder;
import dan200.computercraft.shared.command.CommandComputerCraft;
import dan200.computercraft.shared.command.UserLevel;
import dan200.computercraft.shared.platform.RegistrationHelper;
import net.minecraft.commands.CommandSourceStack;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Predicate;

/**
 * A registry of nodes in a permission system.
 * <p>
 * This acts as an abstraction layer over permission systems such Forge's built-in permissions API, or Fabric's
 * unofficial <a href="https://github.com/lucko/fabric-permissions-api">fabric-permissions-api-v0</a>.
 * <p>
 * This behaves similarly to {@link RegistrationHelper} (aka Forge's deferred registry), in that you {@linkplain #create()
 * create a registry}, {@linkplain #registerCommand(String, UserLevel) add nodes to it} and then finally {@linkplain
 * #register()} all created nodes.
 *
 * @see dan200.computercraft.shared.ModRegistry.Permissions
 */
public abstract class PermissionRegistry {
    private boolean frozen = false;

    /**
     * Register a permission node for a command. The registered node should be of the form {@code "command." + command}.
     *
     * @param command  The name of the command. This should be one of the subcommands under the {@code /computercraft}
     *                 subcommand, and not something general.
     * @param fallback The default/fallback permission check.
     * @return The resulting predicate which should be passed to {@link ArgumentBuilder#requires(Predicate)}.
     * @see CommandComputerCraft
     */
    public abstract Predicate<CommandSourceStack> registerCommand(String command, UserLevel fallback);

    /**
     * Check that the registry has not been frozen (namely {@link #register()} has been called). This should be called
     * before registering each node.
     */
    protected void checkNotFrozen() {
        if (frozen) throw new IllegalStateException("Permission registry has been frozen.");
    }

    /**
     * Freeze the permissions registry and register the underlying nodes.
     */
    @OverridingMethodsMustInvokeSuper
    public void register() {
        frozen = true;
    }

    public interface Provider {
        Optional<PermissionRegistry> get();
    }

    public static PermissionRegistry create() {
        return ServiceLoader.load(Provider.class)
            .stream()
            .flatMap(x -> x.get().get().stream())
            .findFirst()
            .orElseGet(DefaultPermissionRegistry::new);
    }

    private static final class DefaultPermissionRegistry extends PermissionRegistry {
        @Override
        public Predicate<CommandSourceStack> registerCommand(String command, UserLevel fallback) {
            checkNotFrozen();
            return fallback;
        }
    }
}
