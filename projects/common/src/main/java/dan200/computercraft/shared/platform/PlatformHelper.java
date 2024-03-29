// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.platform;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.ArgumentType;
import dan200.computercraft.api.network.wired.WiredElement;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.config.ConfigFile;
import dan200.computercraft.shared.network.MessageType;
import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.network.client.ClientNetworkContext;
import dan200.computercraft.shared.network.container.ContainerData;
import dan200.computercraft.shared.util.InventoryUtil;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.tags.TagKey;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

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
     * Check if we're running in a development environment.
     *
     * @return If we're running in a development environment.
     */
    boolean isDevelopmentEnvironment();

    /**
     * Create a new config builder.
     *
     * @return The newly created config builder.
     */
    ConfigFile.Builder createConfigBuilder();

    /**
     * Wrap a Minecraft registry in our own abstraction layer.
     *
     * @param registry The registry to wrap.
     * @param <T>      The type of object stored in this registry.
     * @return The wrapped registry.
     */
    <T> RegistryWrappers.RegistryWrapper<T> wrap(ResourceKey<Registry<T>> registry);

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
     * Determine if this resource should be loaded, based on platform-specific loot conditions.
     * <p>
     * This should only be called from the {@code apply} stage of a reload listener.
     *
     * @param object The root JSON object of this resource.
     * @return If this resource should be loaded.
     */
    boolean shouldLoadResource(JsonObject object);

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
     * Register a new argument type.
     *
     * @param klass The argument type we're registering.
     * @param info  The argument type info.
     * @param <A>   The argument type.
     * @param <T>   The argument type template.
     * @param <I>   Argument type info
     * @return The registered argument type.
     */
    <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>, I extends ArgumentTypeInfo<A, T>> I registerArgumentTypeInfo(Class<A> klass, I info);

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
     * Create a new {@link MessageType}.
     *
     * @param id      The descriminator for this message type.
     * @param channel The channel name for this message type.
     * @param klass   The type of this message.
     * @param reader  The function which reads the packet from a buffer. Should be the inverse to {@link NetworkMessage#write(FriendlyByteBuf)}.
     * @param <T>     The type of this message.
     * @return The new {@link MessageType} instance.
     */
    <T extends NetworkMessage<?>> MessageType<T> createMessageType(int id, ResourceLocation channel, Class<T> klass, FriendlyByteBuf.Reader<T> reader);

    /**
     * Convert a clientbound {@link NetworkMessage} to a Minecraft {@link Packet}.
     *
     * @param message The messsge to convert.
     * @return The converted message.
     */
    Packet<ClientGamePacketListener> createPacket(NetworkMessage<ClientNetworkContext> message);

    /**
     * Create a {@link ComponentAccess} for surrounding peripherals.
     *
     * @param owner      The block entity requesting surrounding peripherals.
     * @param invalidate The function to call when a neighbouring peripheral potentially changes. This <em>MAY NOT</em>
     *                   include all changes, and so block updates should still be listened to.
     * @return The peripheral component access.
     */
    ComponentAccess<IPeripheral> createPeripheralAccess(BlockEntity owner, Consumer<Direction> invalidate);

    /**
     * Create a {@link ComponentAccess} for surrounding wired nodes.
     *
     * @param owner      The block entity requesting surrounding wired elements.
     * @param invalidate The function to call when a neighbouring wired node potentially changes. This <em>MAY NOT</em>
     *                   include all changes, and so block updates should still be listened to.
     * @return The peripheral component access.
     */
    ComponentAccess<WiredElement> createWiredElementAccess(BlockEntity owner, Consumer<Direction> invalidate);

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

    /**
     * Get the amount of fuel an item provides.
     *
     * @param stack The item to burn.
     * @return The amount of fuel it provides.
     */
    int getBurnTime(ItemStack stack);

    /**
     * Create a builder for a new creative tab.
     *
     * @return The creative tab builder.
     */
    CreativeModeTab.Builder newCreativeModeTab();

    /**
     * Get the "container" item to be returned after crafting. For instance, crafting with a lava bucket should return
     * an empty bucket.
     *
     * @param stack The original item.
     * @return The "remainder" item. May be {@link ItemStack#EMPTY}.
     */
    ItemStack getCraftingRemainingItem(ItemStack stack);

    /**
     * A more general version of {@link #getCraftingRemainingItem(ItemStack)} which gets all remaining items for a
     * recipe.
     *
     * @param player    The player performing the crafting.
     * @param recipe    The recipe currently doing the crafting.
     * @param container The crafting container.
     * @return A list of items to return to the player after crafting.
     */
    List<ItemStack> getRecipeRemainingItems(ServerPlayer player, Recipe<CraftingContainer> recipe, CraftingContainer container);

    /**
     * Fire an event after crafting has occurred.
     *
     * @param player    The player performing the crafting.
     * @param container The current crafting container.
     * @param stack     The resulting stack from crafting.
     */
    void onItemCrafted(ServerPlayer player, CraftingContainer container, ItemStack stack);

    /**
     * Check whether we should notify neighbours in a particular direction.
     *
     * @param level     The current level.
     * @param pos       The position of the current block.
     * @param block     The block which is performing the notification, should be equal to {@code level.getBlockState(pos)}.
     * @param direction The direction we're notifying in.
     * @return {@code true} if neighbours should be notified or {@code false} otherwise.
     */
    boolean onNotifyNeighbour(Level level, BlockPos pos, BlockState block, Direction direction);

    /**
     * Create a new fake player.
     *
     * @param level   The level the player should be created in.
     * @param profile The user this player should mimic.
     * @return The newly constructed fake player.
     */
    ServerPlayer createFakePlayer(ServerLevel level, GameProfile profile);

    /**
     * Determine if a player is not a real player.
     *
     * @param player The player to check.
     * @return Whether this player is fake.
     */
    default boolean isFakePlayer(ServerPlayer player) {
        // Any subclass of ServerPlayer (i.e. Forge's FakePlayer) is assumed to be a fake.
        return player.connection == null || player.getClass() != ServerPlayer.class;
    }

    /**
     * Get the distance a player can reach.
     *
     * @param player The player who is reaching.
     * @return The distance (in blocks) that a player can reach.
     */
    default double getReachDistance(Player player) {
        return player.isCreative() ? 5 : 4.5;
    }

    /**
     * Check if this item is a tool and has some secondary usage.
     * <p>
     * In practice, this only checks if a tool is a hoe or shovel. We don't want to include things like axes,
     *
     * @param stack The stack to check.
     * @return Whether this tool has a secondary usage.
     */
    boolean hasToolUsage(ItemStack stack);

    /**
     * Check if an entity can be attacked according to platform-specific events.
     *
     * @param player The player who is attacking.
     * @param entity The entity we're attacking.
     * @return If this entity can be attacked.
     * @see Player#attack(Entity)
     */
    InteractionResult canAttackEntity(ServerPlayer player, Entity entity);

    /**
     * Interact with an entity, for instance feeding cows.
     * <p>
     * Implementations should follow Minecraft behaviour - we try {@link Entity#interactAt(Player, Vec3, InteractionHand)}
     * and then {@link Player#interactOn(Entity, InteractionHand)}. Loader-specific hooks should also be called.
     *
     * @param player The player which is interacting with the entity.
     * @param entity The entity we're interacting with.
     * @param hitPos The position our ray trace hit the entity. This is a position in-world, unlike
     *               {@link Entity#interactAt(Player, Vec3, InteractionHand)} which is relative to the entity.
     * @return Whether any interaction occurred.
     * @see Entity#interactAt(Player, Vec3, InteractionHand)
     * @see Player#interactOn(Entity, InteractionHand)
     * @see ServerGamePacketListenerImpl#handleInteract
     */
    boolean interactWithEntity(ServerPlayer player, Entity entity, Vec3 hitPos);

    /**
     * Place an item against a block.
     * <p>
     * Implementations should largely mirror {@link ServerPlayerGameMode#useItemOn(ServerPlayer, Level, ItemStack, InteractionHand, BlockHitResult)}
     * (including any loader-specific modifications), except the call to {@link BlockState#use(Level, Player, InteractionHand, BlockHitResult)}
     * should only be evaluated when {@code canUseBlock} evaluates to true.
     *
     * @param player      The player which is placing this item.
     * @param stack       The item to place.
     * @param hit         The collision with the block we're placing against.
     * @param canUseBlock Test whether the block should be interacted with first.
     * @return Whether any interaction occurred.
     * @see ServerPlayerGameMode#useItemOn(ServerPlayer, Level, ItemStack, InteractionHand, BlockHitResult)
     */
    InteractionResult useOn(ServerPlayer player, ItemStack stack, BlockHitResult hit, Predicate<BlockState> canUseBlock);

    /**
     * Whether {@link net.minecraft.network.chat.ClickEvent.Action#RUN_COMMAND} can be used to run client commands.
     *
     * @return Whether client commands can be triggered from chat components.
     */
    default boolean canClickRunClientCommand() {
        return true;
    }
}
