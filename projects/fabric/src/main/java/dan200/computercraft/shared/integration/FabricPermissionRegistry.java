// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.integration;

import com.google.auto.service.AutoService;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.shared.command.UserLevel;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * An implementation of {@link PermissionRegistry} using Fabric's unofficial {@linkplain Permissions permissions api}.
 */
public final class FabricPermissionRegistry extends PermissionRegistry {
    private FabricPermissionRegistry() {
    }

    @Override
    public Predicate<CommandSourceStack> registerCommand(String command, UserLevel fallback) {
        checkNotFrozen();
        var name = ComputerCraftAPI.MOD_ID + ".command." + command;
        return source -> Permissions.getPermissionValue(source, name).orElseGet(() -> fallback.test(source));
    }

    @AutoService(PermissionRegistry.Provider.class)
    public static final class Provider implements PermissionRegistry.Provider {
        @Override
        public Optional<PermissionRegistry> get() {
            return FabricLoader.getInstance().isModLoaded("fabric-permissions-api-v0")
                ? Optional.of(new FabricPermissionRegistry())
                : Optional.empty();
        }
    }
}
