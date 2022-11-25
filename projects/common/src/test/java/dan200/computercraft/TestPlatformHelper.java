/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft;

import com.google.auto.service.AutoService;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.ArgumentType;
import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.api.network.wired.WiredElement;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.impl.AbstractComputerCraftAPI;
import dan200.computercraft.impl.ComputerCraftAPIService;
import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.network.client.ClientNetworkContext;
import dan200.computercraft.shared.network.container.ContainerData;
import dan200.computercraft.shared.platform.*;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
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
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

@AutoService({ PlatformHelper.class, dan200.computercraft.impl.PlatformHelper.class, ComputerCraftAPIService.class })
public class TestPlatformHelper extends AbstractComputerCraftAPI implements PlatformHelper {
    @Override
    public <T> Registries.RegistryWrapper<T> wrap(ResourceKey<Registry<T>> registry) {
        throw new UnsupportedOperationException("Cannot query registry inside tests");
    }

    @Override
    public <T> RegistrationHelper<T> createRegistrationHelper(ResourceKey<Registry<T>> registry) {
        throw new UnsupportedOperationException("Cannot query registry inside tests");
    }

    @Override
    public <K> ResourceLocation getRegistryKey(ResourceKey<Registry<K>> registry, K object) {
        throw new UnsupportedOperationException("Cannot query registry inside tests");
    }

    @Override
    public <K> K getRegistryObject(ResourceKey<Registry<K>> registry, ResourceLocation id) {
        throw new UnsupportedOperationException("Cannot query registry inside tests");
    }

    @Override
    public <T extends BlockEntity> BlockEntityType<T> createBlockEntityType(BiFunction<BlockPos, BlockState, T> factory, Block block) {
        throw new UnsupportedOperationException("Cannot create BlockEntityType inside tests");
    }

    @Override
    public <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>, I extends ArgumentTypeInfo<A, T>> I registerArgumentTypeInfo(Class<A> klass, I info) {
        throw new UnsupportedOperationException("Cannot register ArgumentTypeInfo inside tests");
    }

    @Override
    public void sendToPlayer(NetworkMessage<ClientNetworkContext> message, ServerPlayer player) {
        throw new UnsupportedOperationException("Cannot send NetworkMessages inside tests");
    }

    @Override
    public void sendToPlayers(NetworkMessage<ClientNetworkContext> message, Collection<ServerPlayer> players) {
        throw new UnsupportedOperationException("Cannot send NetworkMessages inside tests");
    }

    @Override
    public void sendToAllPlayers(NetworkMessage<ClientNetworkContext> message, MinecraftServer server) {
        throw new UnsupportedOperationException("Cannot send NetworkMessages inside tests");
    }

    @Override
    public void sendToAllAround(NetworkMessage<ClientNetworkContext> message, ServerLevel level, Vec3 pos, float distance) {
        throw new UnsupportedOperationException("Cannot send NetworkMessages inside tests");
    }

    @Override
    public void sendToAllTracking(NetworkMessage<ClientNetworkContext> message, LevelChunk chunk) {
        throw new UnsupportedOperationException("Cannot send NetworkMessages inside tests");
    }

    @Override
    public CreativeModeTab getCreativeTab() {
        throw new UnsupportedOperationException("Cannot get creative tab inside tests");
    }

    @Override
    public List<TagKey<Item>> getDyeTags() {
        throw new UnsupportedOperationException("Cannot query tags inside tests");
    }

    @Override
    public <C extends AbstractContainerMenu, T extends ContainerData> MenuType<C> createMenuType(Function<FriendlyByteBuf, T> reader, ContainerData.Factory<C, T> factory) {
        throw new UnsupportedOperationException("Cannot create MenuType inside tests");
    }

    @Override
    public void openMenu(Player player, MenuProvider owner, ContainerData menu) {
        throw new UnsupportedOperationException("Cannot open menu inside tests");
    }

    @Override
    public ComponentAccess<IPeripheral> createPeripheralAccess(Consumer<Direction> invalidate) {
        throw new UnsupportedOperationException("Cannot interact with the world inside tests");
    }

    @Override
    public ComponentAccess<WiredElement> createWiredElementAccess(Consumer<Direction> invalidate) {
        throw new UnsupportedOperationException("Cannot interact with the world inside tests");
    }

    @Override
    public boolean hasWiredElementIn(Level level, BlockPos pos, Direction direction) {
        throw new UnsupportedOperationException("Cannot interact with the world inside tests");
    }

    @Override
    public boolean onNotifyNeighbour(Level level, BlockPos pos, BlockState block, Direction direction) {
        throw new UnsupportedOperationException("Cannot interact with the world inside tests");
    }

    @Override
    public Collection<CreativeModeTab> getCreativeTabs(ItemStack stack) {
        throw new UnsupportedOperationException("Cannot get creative tabs inside tests");
    }

    @Override
    public RecipeIngredients getRecipeIngredients() {
        throw new UnsupportedOperationException("Cannot query recipes inside tests");
    }

    @Override
    public int getBurnTime(ItemStack stack) {
        throw new UnsupportedOperationException("Cannot get burn time inside tests");
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return new ItemStack(stack.getItem().getCraftingRemainingItem());
    }

    @Override
    public ServerPlayer createFakePlayer(ServerLevel world, GameProfile name) {
        throw new UnsupportedOperationException("Cannot interact with the world inside tests");
    }

    @Override
    public boolean hasToolUsage(ItemStack stack) {
        throw new UnsupportedOperationException("Cannot query item properties inside tests");
    }

    @Override
    public InteractionResult canAttackEntity(ServerPlayer player, Entity entity) {
        throw new UnsupportedOperationException("Cannot get burn time inside tests");
    }

    @Override
    public boolean interactWithEntity(ServerPlayer player, Entity entity, Vec3 hitPos) {
        throw new UnsupportedOperationException("Cannot interact with the world inside tests");
    }

    @Override
    public InteractionResult useOn(ServerPlayer player, ItemStack stack, BlockHitResult hit) {
        throw new UnsupportedOperationException("Cannot interact with the world inside tests");
    }

    @Override
    public ContainerTransfer.Slotted wrapContainer(Container container) {
        throw new UnsupportedOperationException("Cannot wrap container");
    }

    @Nullable
    @Override
    public ContainerTransfer getContainer(ServerLevel level, BlockPos pos, Direction side) {
        throw new UnsupportedOperationException("Cannot interact with the world inside tests");
    }

    @Override
    public List<ItemStack> getRecipeRemainingItems(ServerPlayer player, Recipe<CraftingContainer> recipe, CraftingContainer container) {
        throw new UnsupportedOperationException("Cannot query recipes inside tests");
    }

    @Override
    public void onItemCrafted(ServerPlayer player, CraftingContainer container, ItemStack stack) {
        throw new UnsupportedOperationException("Cannot interact with the world inside tests");
    }

    @Override
    public String getInstalledVersion() {
        return "1.0";
    }

    @Nullable
    @Override
    public Mount createResourceMount(MinecraftServer server, String domain, String subPath) {
        throw new UnsupportedOperationException("Cannot create resource mount");
    }

    @Nullable
    @Override
    public <T> T tryGetRegistryObject(ResourceKey<Registry<T>> registry, ResourceLocation id) {
        throw new UnsupportedOperationException("Cannot query registries");
    }
}
