/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.platform;

import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.network.client.ClientNetworkContext;
import dan200.computercraft.shared.network.container.ContainerData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * This extends {@linkplain dan200.computercraft.impl.PlatformHelper the API's loader abstraction layer}, adding
 * additional methods used by the actual mod.
 */
public interface PlatformHelper extends dan200.computercraft.impl.PlatformHelper {
    /**
     * Get the current {@link PlatformHelper} instance.
     *
     * @return The current instance.
     */
    static PlatformHelper get() {
        return (PlatformHelper) dan200.computercraft.impl.PlatformHelper.get();
    }

    /**
     * Wrap a Minecraft registry in our own abstraction layer.
     *
     * @param registry The registry to wrap.
     * @param <T>      The type of object stored in this registry.
     * @return The wrapped registry.
     */
    <T> Registries.RegistryWrapper<T> wrap(ResourceKey<Registry<T>> registry);

    /**
     * Create a registration helper for a specific registry.
     *
     * @param registry The registry we'll add entries to.
     * @param <T>      The type of object stored in the registry.
     * @return The registration helper.
     */
    <T> RegistrationHelper<T> createRegistrationHelper(ResourceKey<Registry<T>> registry);

    /**
     * A version of {@link #getRegistryObject(ResourceKey, ResourceLocation)} which allows missing entries.
     *
     * @param registry The registry to look up this object in.
     * @param id       The ID to look up.
     * @param <T>      The type of object the registry stores.
     * @return The registered object or {@code null}.
     */
    @Nullable
    <T> T tryGetRegistryObject(ResourceKey<Registry<T>> registry, ResourceLocation id);

    /**
     * Create a new block entity type which serves a particular block.
     *
     * @param factory The method which creates a new block entity with this type, typically the constructor.
     * @param block   The block this block entity exists on.
     * @param <T>     The type of block entity we're creating.
     * @return The new block entity type.
     */
    <T extends BlockEntity> BlockEntityType<T> createBlockEntityType(BiFunction<BlockPos, BlockState, T> factory, Block block);

    /**
     * Create a menu type which sends additional data when opened.
     *
     * @param reader  Parse the additional container data into a usable type.
     * @param factory The factory to create the new menu.
     * @param <C>     The menu/container than we open.
     * @param <T>     The data that we send to the client.
     * @return The menu type for this container.
     */
    <C extends AbstractContainerMenu, T extends ContainerData> MenuType<C> createMenuType(Function<FriendlyByteBuf, T> reader, ContainerData.Factory<C, T> factory);

    /**
     * Open a container using a specific {@link ContainerData}.
     *
     * @param player The player to open the menu for.
     * @param owner  The underlying menu provider.
     * @param menu   The menu data.
     */
    void openMenu(Player player, MenuProvider owner, ContainerData menu);

    /**
     * Send a message to a specific player.
     *
     * @param message The message to send.
     * @param player  The player to send it to.
     */
    void sendToPlayer(NetworkMessage<ClientNetworkContext> message, ServerPlayer player);

    /**
     * Send a message to a set of players.
     *
     * @param message The message to send.
     * @param players The players to send it to.
     */
    void sendToPlayers(NetworkMessage<ClientNetworkContext> message, Collection<ServerPlayer> players);

    /**
     * Send a message to all players.
     *
     * @param message The message to send.
     * @param server  The current server.
     */
    void sendToAllPlayers(NetworkMessage<ClientNetworkContext> message, MinecraftServer server);

    /**
     * Send a message to all players around a point.
     *
     * @param message  The message to send.
     * @param level    The level the point is in.
     * @param pos      The centre position.
     * @param distance The distance to the centre players must be within.
     */
    void sendToAllAround(NetworkMessage<ClientNetworkContext> message, ServerLevel level, Vec3 pos, float distance);

    /**
     * Send a message to all players tracking a chunk.
     *
     * @param message The message to send.
     * @param chunk   The chunk players must be tracking.
     */
    void sendToAllTracking(NetworkMessage<ClientNetworkContext> message, LevelChunk chunk);
}
