// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.platform;

import com.google.auto.service.AutoService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.ArgumentType;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.network.wired.WiredElement;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.impl.Peripherals;
import dan200.computercraft.shared.Capabilities;
import dan200.computercraft.shared.config.ConfigFile;
import dan200.computercraft.shared.network.MessageType;
import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.network.client.ClientNetworkContext;
import dan200.computercraft.shared.network.container.ContainerData;
import dan200.computercraft.shared.util.CapabilityUtil;
import dan200.computercraft.shared.util.InventoryUtil;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.ModLoadedCondition;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.RegistryManager;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.*;

@AutoService(dan200.computercraft.impl.PlatformHelper.class)
public class PlatformHelperImpl implements PlatformHelper {
    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.isProduction();
    }

    @Override
    public ConfigFile.Builder createConfigBuilder() {
        return new ForgeConfigFile.Builder();
    }

    @Override
    public <T> ResourceLocation getRegistryKey(ResourceKey<Registry<T>> registry, T object) {
        var key = RegistryManager.ACTIVE.getRegistry(registry).getKey(object);
        if (key == null) throw new IllegalArgumentException(object + " was not registered in " + registry);
        return key;
    }

    @Override
    public <T> T getRegistryObject(ResourceKey<Registry<T>> registry, ResourceLocation id) {
        var value = RegistryManager.ACTIVE.getRegistry(registry).getValue(id);
        if (value == null) throw new IllegalArgumentException(id + " was not registered in " + registry);
        return value;
    }

    @Override
    public <T> RegistryWrappers.RegistryWrapper<T> wrap(ResourceKey<Registry<T>> key) {
        return new RegistryWrapperImpl<>(key.location(), RegistryManager.ACTIVE.getRegistry(key));
    }

    @Override
    public <T> RegistrationHelper<T> createRegistrationHelper(ResourceKey<Registry<T>> registry) {
        return new RegistrationHelperImpl<>(DeferredRegister.create(registry, ComputerCraftAPI.MOD_ID));
    }

    @Nullable
    @Override
    public <K> K tryGetRegistryObject(ResourceKey<Registry<K>> registry, ResourceLocation id) {
        return RegistryManager.ACTIVE.getRegistry(registry).getValue(id);
    }

    @Override
    public boolean shouldLoadResource(JsonObject object) {
        return ICondition.shouldRegisterEntry(object);
    }

    @Override
    public void addRequiredModCondition(JsonObject object, String modId) {
        var conditions = GsonHelper.getAsJsonArray(object, "forge:conditions", null);
        if (conditions == null) {
            conditions = new JsonArray();
            object.add("forge:conditions", conditions);
        }

        conditions.add(CraftingHelper.serialize(new ModLoadedCondition(modId)));
    }

    @Override
    public <T extends BlockEntity> BlockEntityType<T> createBlockEntityType(BiFunction<BlockPos, BlockState, T> factory, Block block) {
        return new BlockEntityType<>(factory::apply, Set.of(block), null);
    }

    @Override
    public <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>, I extends ArgumentTypeInfo<A, T>> I registerArgumentTypeInfo(Class<A> klass, I info) {
        return ArgumentTypeInfos.registerByClass(klass, info);
    }

    @Override
    public <C extends AbstractContainerMenu, T extends ContainerData> MenuType<C> createMenuType(Function<FriendlyByteBuf, T> reader, ContainerData.Factory<C, T> factory) {
        return IForgeMenuType.create((id, player, data) -> factory.create(id, player, reader.apply(data)));
    }

    @Override
    public void openMenu(Player player, MenuProvider owner, ContainerData menu) {
        NetworkHooks.openScreen((ServerPlayer) player, owner, menu::toBytes);
    }

    @Override
    public <T extends NetworkMessage<?>> MessageType<T> createMessageType(int id, ResourceLocation channel, Class<T> klass, FriendlyByteBuf.Reader<T> reader) {
        return new NetworkHandler.MessageTypeImpl<>(id, klass, reader);
    }

    @Override
    public Packet<ClientGamePacketListener> createPacket(NetworkMessage<ClientNetworkContext> message) {
        return NetworkHandler.createClientboundPacket(message);
    }

    @Override
    public ComponentAccess<IPeripheral> createPeripheralAccess(BlockEntity owner, Consumer<Direction> invalidate) {
        return new PeripheralAccess(owner, invalidate);
    }

    @Override
    public ComponentAccess<WiredElement> createWiredElementAccess(BlockEntity owner, Consumer<Direction> invalidate) {
        return new CapabilityAccess<>(owner, Capabilities.CAPABILITY_WIRED_ELEMENT, invalidate);
    }

    @Override
    public boolean hasWiredElementIn(Level level, BlockPos pos, Direction direction) {
        if (!level.isLoaded(pos)) return false;

        var blockEntity = level.getBlockEntity(pos.relative(direction));
        return blockEntity != null && blockEntity.getCapability(Capabilities.CAPABILITY_WIRED_ELEMENT, direction.getOpposite()).isPresent();
    }

    @Override
    public ContainerTransfer.Slotted wrapContainer(Container container) {
        return new ForgeContainerTransfer(new InvWrapper(container));
    }

    @Nullable
    @Override
    public ContainerTransfer getContainer(ServerLevel level, BlockPos pos, Direction side) {
        var block = level.getBlockState(pos);
        if (block.getBlock() instanceof WorldlyContainerHolder holder) {
            var container = holder.getContainer(block, level, pos);
            return new ForgeContainerTransfer(new SidedInvWrapper(container, side));
        }

        var blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null) {
            var inventory = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, side);
            if (inventory.isPresent()) {
                return new ForgeContainerTransfer(inventory.orElseThrow(NullPointerException::new));
            }
        }

        var entity = InventoryUtil.getEntityContainer(level, pos, side);
        return entity == null ? null : new ForgeContainerTransfer(new InvWrapper(entity));
    }

    @Nullable
    @Override
    public CompoundTag getShareTag(ItemStack item) {
        return item.getShareTag();
    }

    @Override
    public RecipeIngredients getRecipeIngredients() {
        return new RecipeIngredients(
            Ingredient.of(Tags.Items.DUSTS_REDSTONE),
            Ingredient.of(Tags.Items.STRING),
            Ingredient.of(Tags.Items.LEATHER),
            Ingredient.of(Tags.Items.STONE),
            Ingredient.of(Tags.Items.GLASS_PANES),
            Ingredient.of(Tags.Items.INGOTS_GOLD),
            Ingredient.of(Tags.Items.STORAGE_BLOCKS_GOLD),
            Ingredient.of(Tags.Items.INGOTS_IRON),
            Ingredient.of(Tags.Items.HEADS),
            Ingredient.of(Tags.Items.DYES),
            Ingredient.of(Tags.Items.ENDER_PEARLS),
            Ingredient.of(Tags.Items.CHESTS_WOODEN)
        );
    }

    @Override
    public List<TagKey<Item>> getDyeTags() {
        return List.of(
            Tags.Items.DYES_WHITE,
            Tags.Items.DYES_ORANGE,
            Tags.Items.DYES_MAGENTA,
            Tags.Items.DYES_LIGHT_BLUE,
            Tags.Items.DYES_YELLOW,
            Tags.Items.DYES_LIME,
            Tags.Items.DYES_PINK,
            Tags.Items.DYES_GRAY,
            Tags.Items.DYES_LIGHT_GRAY,
            Tags.Items.DYES_CYAN,
            Tags.Items.DYES_PURPLE,
            Tags.Items.DYES_BLUE,
            Tags.Items.DYES_BROWN,
            Tags.Items.DYES_GREEN,
            Tags.Items.DYES_RED,
            Tags.Items.DYES_BLACK
        );
    }

    @Override
    public int getBurnTime(ItemStack stack) {
        return ForgeHooks.getBurnTime(stack, null);
    }

    @Override
    public CreativeModeTab.Builder newCreativeModeTab() {
        return CreativeModeTab.builder();
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return stack.getCraftingRemainingItem();
    }

    @Override
    public List<ItemStack> getRecipeRemainingItems(ServerPlayer player, Recipe<CraftingContainer> recipe, CraftingContainer container) {
        ForgeHooks.setCraftingPlayer(player);
        var result = recipe.getRemainingItems(container);
        ForgeHooks.setCraftingPlayer(null);
        return result;
    }

    @Override
    public void onItemCrafted(ServerPlayer player, CraftingContainer container, ItemStack stack) {
        ForgeEventFactory.firePlayerCraftingEvent(player, stack, container);
    }

    @Override
    public boolean onNotifyNeighbour(Level level, BlockPos pos, BlockState block, Direction direction) {
        return !ForgeEventFactory.onNeighborNotify(level, pos, block, EnumSet.of(direction), false).isCanceled();
    }

    @Override
    public ServerPlayer createFakePlayer(ServerLevel world, GameProfile profile) {
        return new FakePlayerExt(world, profile);
    }

    @Override
    public double getReachDistance(Player player) {
        return player.getBlockReach();
    }

    @Override
    public boolean hasToolUsage(ItemStack stack) {
        return stack.canPerformAction(ToolActions.SHOVEL_FLATTEN) || stack.canPerformAction(ToolActions.HOE_TILL);
    }

    @Override
    public InteractionResult canAttackEntity(ServerPlayer player, Entity entity) {
        return ForgeHooks.onPlayerAttackTarget(player, entity) ? InteractionResult.PASS : InteractionResult.SUCCESS;
    }

    @Override
    public boolean interactWithEntity(ServerPlayer player, Entity entity, Vec3 hitPos) {
        // Our behaviour is slightly different here - we call onInteractEntityAt before the interact methods, while
        // Forge does the call afterwards (on the server, not on the client).
        var interactAt = ForgeHooks.onInteractEntityAt(player, entity, hitPos, InteractionHand.MAIN_HAND);
        if (interactAt == null) {
            interactAt = entity.interactAt(player, hitPos.subtract(entity.position()), InteractionHand.MAIN_HAND);
        }

        return interactAt.consumesAction() || player.interactOn(entity, InteractionHand.MAIN_HAND).consumesAction();
    }

    @Override
    public InteractionResult useOn(ServerPlayer player, ItemStack stack, BlockHitResult hit, Predicate<BlockState> canUseBlock) {
        var level = player.level();
        var pos = hit.getBlockPos();
        var event = ForgeHooks.onRightClickBlock(player, InteractionHand.MAIN_HAND, pos, hit);
        if (event.isCanceled()) return event.getCancellationResult();

        var context = new UseOnContext(player, InteractionHand.MAIN_HAND, hit);
        if (event.getUseItem() != Event.Result.DENY) {
            var result = stack.onItemUseFirst(context);
            if (result != InteractionResult.PASS) return result;
        }

        var block = level.getBlockState(hit.getBlockPos());
        if (event.getUseBlock() != Event.Result.DENY && !block.isAir() && canUseBlock.test(block)) {
            var useResult = block.use(level, player, InteractionHand.MAIN_HAND, hit);
            if (useResult.consumesAction()) return useResult;
        }

        return event.getUseItem() == Event.Result.DENY ? InteractionResult.PASS : stack.useOn(context);
    }

    @Override
    public boolean canClickRunClientCommand() {
        return false;
    }

    private record RegistryWrapperImpl<T>(
        ResourceLocation name, ForgeRegistry<T> registry
    ) implements RegistryWrappers.RegistryWrapper<T> {
        @Override
        public int getId(T object) {
            return registry.getID(object);
        }

        @Override
        public ResourceLocation getKey(T object) {
            var key = registry.getKey(object);
            if (key == null) throw new IllegalStateException(object + " was not registered in " + name);
            return key;
        }

        @Override
        public T get(ResourceLocation location) {
            var object = registry.getValue(location);
            if (object == null) throw new IllegalStateException(location + " was not registered in " + name);
            return object;
        }

        @Nullable
        @Override
        public T tryGet(ResourceLocation location) {
            return registry.getValue(location);
        }

        @Override
        public @Nullable T byId(int id) {
            return registry.getValue(id);
        }

        @Override
        public int size() {
            return registry.getKeys().size();
        }

        @Override
        public Iterator<T> iterator() {
            return registry.iterator();
        }
    }

    private record RegistrationHelperImpl<T>(DeferredRegister<T> registry) implements RegistrationHelper<T> {
        @Override
        public <U extends T> RegistryEntry<U> register(String name, Supplier<U> create) {
            return new RegistryEntryImpl<>(registry().register(name, create));
        }

        @Override
        public void register() {
            registry().register(FMLJavaModLoadingContext.get().getModEventBus());
        }
    }

    private record RegistryEntryImpl<T>(RegistryObject<T> object) implements RegistryEntry<T> {
        @Override
        public ResourceLocation id() {
            return object().getId();
        }

        @Override
        public T get() {
            return object().get();
        }
    }

    private abstract static class ComponentAccessImpl<T> implements ComponentAccess<T> {
        private final BlockEntity owner;
        private final InvalidateCallback[] invalidators;

        ComponentAccessImpl(BlockEntity owner, Consumer<Direction> invalidate) {
            this.owner = owner;

            // Generate a cache of invalidation functions so we can guarantee we only ever have one registered per
            // capability - there's no way to remove these callbacks!
            var invalidators = this.invalidators = new InvalidateCallback[6];
            for (var dir : Direction.values()) invalidators[dir.ordinal()] = () -> invalidate.accept(dir);
        }

        @Nullable
        protected abstract T get(ServerLevel world, BlockPos pos, Direction side, InvalidateCallback invalidate);

        @Nullable
        @Override
        public T get(Direction direction) {
            return get(getLevel(), owner.getBlockPos().relative(direction), direction.getOpposite(), invalidators[direction.ordinal()]);
        }

        final ServerLevel getLevel() {
            return Objects.requireNonNull((ServerLevel) owner.getLevel(), "Block entity is not in a level");
        }

    }

    private static class PeripheralAccess extends ComponentAccessImpl<IPeripheral> {
        PeripheralAccess(BlockEntity owner, Consumer<Direction> invalidate) {
            super(owner, invalidate);
        }

        @Nullable
        @Override
        protected IPeripheral get(ServerLevel world, BlockPos pos, Direction side, InvalidateCallback invalidate) {
            return Peripherals.getPeripheral(world, pos, side, invalidate);
        }
    }

    private static class CapabilityAccess<T> extends ComponentAccessImpl<T> {
        private final Capability<T> capability;

        CapabilityAccess(BlockEntity owner, Capability<T> capability, Consumer<Direction> invalidate) {
            super(owner, invalidate);
            this.capability = capability;
        }

        @Nullable
        @Override
        protected T get(ServerLevel world, BlockPos pos, Direction side, InvalidateCallback invalidate) {
            if (!world.isLoaded(pos)) return null;

            var blockEntity = world.getBlockEntity(pos);
            return blockEntity != null ? CapabilityUtil.unwrap(blockEntity.getCapability(capability, side), invalidate) : null;
        }
    }
}
