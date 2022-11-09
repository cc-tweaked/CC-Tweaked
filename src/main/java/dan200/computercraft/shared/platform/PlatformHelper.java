/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.platform;

import dan200.computercraft.api.network.wired.IWiredElement;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.network.client.ClientNetworkContext;
import dan200.computercraft.shared.network.container.ContainerData;
import dan200.computercraft.shared.util.InventoryUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
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

    /**
     * Create a {@link ComponentAccess} for surrounding peripherals.
     *
     * @param invalidate The function to call when a neighbouring peripheral potentially changes. This <em>MAY NOT</em>
     *                   include all changes, and so block updates should still be listened to.
     * @return The peripheral component access.
     */
    ComponentAccess<IPeripheral> createPeripheralAccess(Consumer<Direction> invalidate);

    /**
     * Create a {@link ComponentAccess} for surrounding wired nodes.
     *
     * @param invalidate The function to call when a neighbouring wired node potentially changes. This <em>MAY NOT</em>
     *                   include all changes, and so block updates should still be listened to.
     * @return The peripheral component access.
     */
    ComponentAccess<IWiredElement> createWiredElementAccess(Consumer<Direction> invalidate);

    /**
     * Determine if there is a wired element in the given direction. This is equivalent to
     * {@code createWiredElementAt(x -> {}).get(level, pos, dir) != null}, but is intended for when we don't need the
     * cache.
     *
     * @param level     The current level.
     * @param pos       The <em>current</em> block's position.
     * @param direction The direction to check in.
     * @return Whether there is a wired element in the given direction.
     */
    boolean hasWiredElementIn(Level level, BlockPos pos, Direction direction);

    /**
     * Wrap a vanilla Minecraft {@link Container} into a {@link ContainerTransfer}.
     *
     * @param container The container to wrap.
     * @return The container transfer.
     */
    ContainerTransfer.Slotted wrapContainer(Container container);

    /**
     * Get access to a {@link ContainerTransfer} for a given position. This should look up blocks, then fall back to
     * {@link InventoryUtil#getEntityContainer(ServerLevel, BlockPos, Direction)}
     *
     * @param level The current level.
     * @param pos   The current position.
     * @param side  The side of the block we're viewing the inventory from. Equivalent to the direction argument for
     *              {@link WorldlyContainer}.
     * @return The container, or {@code null} if none exists.
     */
    @Nullable
    ContainerTransfer getContainer(ServerLevel level, BlockPos pos, Direction side);

    /**
     * Wrap a vanilla Minecraft {@link Container} into Forge's {@link IItemHandlerModifiable}.
     *
     * @param container The container to wrap.
     * @return The item handler.
     * @deprecated This is only needed for backwards compatibility, and will be removed in 1.19.3.
     */
    @Deprecated(forRemoval = true)
    IItemHandlerModifiable wrapContainerToItemHandler(Container container);

    /**
     * Get the {@link RecipeIngredients} for this loader.
     *
     * @return The loader-specific recipe ingredients.
     */
    RecipeIngredients getRecipeIngredients();

    /**
     * Get a list of tags representing each Minecraft dye. This should follow the same order as {@linkplain DyeColor
     * Minecraft's dyes}, starting with white and ending with black.
     *
     * @return A list of tags.
     */
    List<TagKey<Item>> getDyeTags();
}
