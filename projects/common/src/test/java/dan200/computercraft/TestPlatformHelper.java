// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft;

import com.google.auto.service.AutoService;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.ArgumentType;
import dan200.computercraft.api.network.wired.WiredElement;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.impl.AbstractComputerCraftAPI;
import dan200.computercraft.impl.ComputerCraftAPIService;
import dan200.computercraft.shared.config.ConfigFile;
import dan200.computercraft.shared.network.MessageType;
import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.network.client.ClientNetworkContext;
import dan200.computercraft.shared.network.container.ContainerData;
import dan200.computercraft.shared.platform.*;
import io.netty.buffer.Unpooled;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

@AutoService({ PlatformHelper.class, dan200.computercraft.impl.PlatformHelper.class, ComputerCraftAPIService.class })
public class TestPlatformHelper extends AbstractComputerCraftAPI implements PlatformHelper {
    @Override
    public boolean isDevelopmentEnvironment() {
        return true;
    }

    @Override
    public ConfigFile.Builder createConfigBuilder() {
        throw new UnsupportedOperationException("Cannot create config file inside tests");
    }

    @Override
    public <T> RegistrationHelper<T> createRegistrationHelper(ResourceKey<Registry<T>> registry) {
        throw new UnsupportedOperationException("Cannot query registry inside tests");
    }

    @SuppressWarnings("unchecked")
    private static <T> Registry<T> getRegistry(ResourceKey<Registry<T>> id) {
        var registry = (Registry<T>) BuiltInRegistries.REGISTRY.get(id.location());
        if (registry == null) throw new IllegalArgumentException("Unknown registry " + id);
        return registry;
    }

    @Override
    public <T> ResourceLocation getRegistryKey(ResourceKey<Registry<T>> registry, T object) {
        var key = getRegistry(registry).getKey(object);
        if (key == null) throw new IllegalArgumentException(object + " was not registered in " + registry);
        return key;
    }

    @Override
    public <T> T getRegistryObject(ResourceKey<Registry<T>> registry, ResourceLocation id) {
        var value = getRegistry(registry).get(id);
        if (value == null) throw new IllegalArgumentException(id + " was not registered in " + registry);
        return value;
    }

    @Override
    public <T> RegistryWrappers.RegistryWrapper<T> wrap(ResourceKey<Registry<T>> registry) {
        return new RegistryWrapperImpl<>(registry.location(), getRegistry(registry));
    }

    @Nullable
    @Override
    public <T> T tryGetRegistryObject(ResourceKey<Registry<T>> registry, ResourceLocation id) {
        return getRegistry(registry).get(id);
    }

    @Override
    public boolean shouldLoadResource(JsonObject object) {
        throw new UnsupportedOperationException("Cannot use resource conditions");
    }

    @Override
    public void addRequiredModCondition(JsonObject object, String modId) {
        throw new UnsupportedOperationException("Cannot use resource conditions");
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

    record TypeImpl<T extends NetworkMessage<?>>(
        ResourceLocation id, Function<FriendlyByteBuf, T> reader
    ) implements MessageType<T> {
    }

    @Override
    public <T extends NetworkMessage<?>> MessageType<T> createMessageType(int id, ResourceLocation channel, Class<T> klass, FriendlyByteBuf.Reader<T> reader) {
        return new TypeImpl<>(channel, reader);
    }

    @Override
    public Packet<ClientGamePacketListener> createPacket(NetworkMessage<ClientNetworkContext> message) {
        var buf = new FriendlyByteBuf(Unpooled.buffer());
        message.write(buf);
        return new ClientboundCustomPayloadPacket(((TypeImpl<?>) message.type()).id(), buf);
    }

    @Override
    public ComponentAccess<IPeripheral> createPeripheralAccess(BlockEntity owner, Consumer<Direction> invalidate) {
        throw new UnsupportedOperationException("Cannot interact with the world inside tests");
    }

    @Override
    public ComponentAccess<WiredElement> createWiredElementAccess(BlockEntity owner, Consumer<Direction> invalidate) {
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
    public CreativeModeTab.Builder newCreativeModeTab() {
        throw new IllegalStateException("Cannot create creative tab inside tests");
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
    public InteractionResult useOn(ServerPlayer player, ItemStack stack, BlockHitResult hit, Predicate<BlockState> canUseBlock) {
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

    private record RegistryWrapperImpl<T>(
        ResourceLocation name, Registry<T> registry
    ) implements RegistryWrappers.RegistryWrapper<T> {
        @Override
        public int getId(T object) {
            return registry.getId(object);
        }

        @Override
        public ResourceLocation getKey(T object) {
            var key = registry.getKey(object);
            if (key == null) throw new IllegalArgumentException(object + " was not registered in " + name);
            return key;
        }

        @Override
        public T get(ResourceLocation location) {
            var object = registry.get(location);
            if (object == null) throw new IllegalArgumentException(location + " was not registered in " + name);
            return object;
        }

        @Nullable
        @Override
        public T tryGet(ResourceLocation location) {
            return registry.get(location);
        }

        @Override
        public @Nullable T byId(int id) {
            return registry.byId(id);
        }

        @Override
        public int size() {
            return registry.size();
        }

        @Override
        public Iterator<T> iterator() {
            return registry.iterator();
        }
    }
}
