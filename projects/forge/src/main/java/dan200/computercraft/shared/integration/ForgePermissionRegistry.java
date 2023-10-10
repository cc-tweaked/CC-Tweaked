// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.integration;

import com.google.auto.service.AutoService;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.shared.command.UserLevel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.events.PermissionGatherEvent;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import net.minecraftforge.server.permission.nodes.PermissionType;
import net.minecraftforge.server.permission.nodes.PermissionTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * An implementation of {@link PermissionRegistry} using Forge's {@link PermissionAPI}.
 */
public final class ForgePermissionRegistry extends PermissionRegistry {
    private final List<PermissionNode<?>> nodes = new ArrayList<>();

    private ForgePermissionRegistry() {
    }

    private <T> PermissionNode<T> registerNode(String nodeName, PermissionType<T> type, PermissionNode.PermissionResolver<T> defaultResolver) {
        checkNotFrozen();
        var node = new PermissionNode<>(ComputerCraftAPI.MOD_ID, nodeName, type, defaultResolver);
        nodes.add(node);
        return node;
    }

    @Override
    public Predicate<CommandSourceStack> registerCommand(String command, UserLevel fallback) {
        var node = registerNode(
            "command." + command, PermissionTypes.BOOLEAN,
            (player, uuid, context) -> player != null && fallback.test(player)
        );

        return source -> {
            var player = source.getPlayer();
            return player == null ? fallback.test(source) : PermissionAPI.getPermission(player, node);
        };
    }

    @Override
    public void register() {
        super.register();
        MinecraftForge.EVENT_BUS.addListener((PermissionGatherEvent.Nodes event) -> event.addNodes(nodes));
    }

    @AutoService(PermissionRegistry.Provider.class)
    public static final class Provider implements PermissionRegistry.Provider {
        @Override
        public Optional<PermissionRegistry> get() {
            return Optional.of(new ForgePermissionRegistry());
        }
    }
}
