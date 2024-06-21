// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.platform;

import com.google.auto.service.AutoService;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.ArgumentType;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.network.wired.WiredElement;
import dan200.computercraft.api.network.wired.WiredElementCapability;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import dan200.computercraft.impl.Peripherals;
import dan200.computercraft.shared.config.ConfigFile;
import dan200.computercraft.shared.container.ListContainer;
import dan200.computercraft.shared.network.container.ContainerData;
import dan200.computercraft.shared.util.InventoryUtil;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.ToolActions;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

@AutoService(PlatformHelper.class)
public class PlatformHelperImpl implements PlatformHelper {
    @Override
    public ConfigFile.Builder createConfigBuilder() {
        return new ForgeConfigFile.Builder();
    }

    @Override
    public <T> RegistrationHelper<T> createRegistrationHelper(ResourceKey<Registry<T>> registry) {
        return new RegistrationHelperImpl<>(DeferredRegister.create(registry, ComputerCraftAPI.MOD_ID));
    }

    @Override
    public <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>, I extends ArgumentTypeInfo<A, T>> I registerArgumentTypeInfo(Class<A> klass, I info) {
        return ArgumentTypeInfos.registerByClass(klass, info);
    }

    @Override
    public <C extends AbstractContainerMenu, T extends ContainerData> MenuType<C> createMenuType(StreamCodec<RegistryFriendlyByteBuf, T> codec, ContainerData.Factory<C, T> factory) {
        return IMenuTypeExtension.create((id, player, data) -> factory.create(id, player, codec.decode(data)));
    }

    @Override
    public void openMenu(Player player, MenuProvider owner, ContainerData menu) {
        player.openMenu(owner, menu::toBytes);
    }

    @Override
    public void invalidateComponent(BlockEntity owner) {
        owner.invalidateCapabilities();
    }

    @Override
    public ComponentAccess<IPeripheral> createPeripheralAccess(BlockEntity owner, Consumer<Direction> invalidate) {
        return new PeripheralAccess(owner, invalidate);
    }

    @Override
    public ComponentAccess<WiredElement> createWiredElementAccess(BlockEntity owner, Consumer<Direction> invalidate) {
        return new ComponentAccessImpl<>(owner, WiredElementCapability.get(), invalidate);
    }

    @Override
    public boolean hasWiredElementIn(Level level, BlockPos pos, Direction direction) {
        if (!level.isLoaded(pos)) return false;
        return level.getCapability(WiredElementCapability.get(), pos.relative(direction), direction.getOpposite()) != null;
    }

    @Override
    public ContainerTransfer.Slotted wrapContainer(Container container) {
        return new ForgeContainerTransfer(new InvWrapper(container));
    }

    @Nullable
    @Override
    public ContainerTransfer getContainer(ServerLevel level, BlockPos pos, Direction side) {
        var inventory = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
        if (inventory != null) return new ForgeContainerTransfer(inventory);

        var entity = InventoryUtil.getEntityContainer(level, pos, side);
        return entity == null ? null : new ForgeContainerTransfer(new InvWrapper(entity));
    }

    @Override
    public RecipeIngredients getRecipeIngredients() {
        return new RecipeIngredients(
            Ingredient.of(Tags.Items.DUSTS_REDSTONE),
            Ingredient.of(Tags.Items.STRINGS),
            Ingredient.of(Tags.Items.LEATHERS),
            Ingredient.of(Tags.Items.GLASS_PANES),
            Ingredient.of(Tags.Items.INGOTS_GOLD),
            Ingredient.of(Tags.Items.STORAGE_BLOCKS_GOLD),
            Ingredient.of(Tags.Items.INGOTS_IRON),
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
        return stack.getBurnTime(null);
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
    public List<ItemStack> getRecipeRemainingItems(ServerPlayer player, Recipe<CraftingInput> recipe, CraftingInput container) {
        CommonHooks.setCraftingPlayer(player);
        var result = recipe.getRemainingItems(container);
        CommonHooks.setCraftingPlayer(null);
        return result;
    }

    @Override
    public void onItemCrafted(ServerPlayer player, CraftingInput container, ItemStack stack) {
        EventHooks.firePlayerCraftingEvent(player, stack, new ListContainer(container.items()));
    }

    @Override
    public boolean onNotifyNeighbour(Level level, BlockPos pos, BlockState block, Direction direction) {
        return !EventHooks.onNeighborNotify(level, pos, block, EnumSet.of(direction), false).isCanceled();
    }

    @Override
    public ServerPlayer createFakePlayer(ServerLevel world, GameProfile profile) {
        return new FakePlayerExt(world, profile);
    }

    @Override
    public boolean hasToolUsage(ItemStack stack) {
        return stack.canPerformAction(ToolActions.SHOVEL_FLATTEN) || stack.canPerformAction(ToolActions.HOE_TILL);
    }

    @Override
    public InteractionResult canAttackEntity(ServerPlayer player, Entity entity) {
        return CommonHooks.onPlayerAttackTarget(player, entity) ? InteractionResult.PASS : InteractionResult.SUCCESS;
    }

    @Override
    public boolean interactWithEntity(ServerPlayer player, Entity entity, Vec3 hitPos) {
        // Our behaviour is slightly different here - we call onInteractEntityAt before the interact methods, while
        // Forge does the call afterwards (on the server, not on the client).
        var interactAt = CommonHooks.onInteractEntityAt(player, entity, hitPos, InteractionHand.MAIN_HAND);
        if (interactAt == null) {
            interactAt = entity.interactAt(player, hitPos.subtract(entity.position()), InteractionHand.MAIN_HAND);
        }

        return interactAt.consumesAction() || player.interactOn(entity, InteractionHand.MAIN_HAND).consumesAction();
    }

    @Override
    public InteractionResult useOn(ServerPlayer player, ItemStack stack, BlockHitResult hit, Predicate<BlockState> canUseBlock) {
        var level = player.level();
        var pos = hit.getBlockPos();
        var event = CommonHooks.onRightClickBlock(player, InteractionHand.MAIN_HAND, pos, hit);
        if (event.isCanceled()) return event.getCancellationResult();

        var context = new UseOnContext(player, InteractionHand.MAIN_HAND, hit);
        if (!event.getUseItem().isFalse()) {
            var result = stack.onItemUseFirst(context);
            if (result != InteractionResult.PASS) return result;
        }

        var block = level.getBlockState(hit.getBlockPos());
        if (!event.getUseBlock().isFalse() && !block.isAir() && canUseBlock.test(block)) {
            var useResult = block.useItemOn(stack, level, player, InteractionHand.MAIN_HAND, hit);
            if (useResult.consumesAction()) return useResult.result();
        }

        return event.getUseItem().isFalse() ? InteractionResult.PASS : stack.useOn(context);
    }

    private record RegistrationHelperImpl<R>(DeferredRegister<R> registry) implements RegistrationHelper<R> {
        @Override
        public <T extends R> RegistryEntry<T> register(String name, Supplier<T> create) {
            return new RegistryEntryImpl<>(registry().register(name, create));
        }

        @Override
        public void register() {
            registry().register(ComputerCraft.getEventBus());
        }
    }

    private record RegistryEntryImpl<R, T extends R>(DeferredHolder<R, T> object) implements RegistryEntry<T> {
        @Override
        public ResourceLocation id() {
            return object().getId();
        }

        @Override
        public T get() {
            return object().get();
        }
    }

    private static class ComponentAccessImpl<T> implements ComponentAccess<T> {
        private final BlockEntity owner;
        private final BlockCapability<T, Direction> capability;
        private final Consumer<Direction> invalidate;
        @SuppressWarnings({ "unchecked", "rawtypes" })
        final BlockCapabilityCache<T, Direction>[] caches = new BlockCapabilityCache[6];

        ComponentAccessImpl(BlockEntity owner, BlockCapability<T, Direction> capability, Consumer<Direction> invalidate) {
            this.owner = owner;
            this.capability = capability;
            this.invalidate = invalidate;
        }

        @Nullable
        @Override
        public T get(Direction direction) {
            var level = getLevel();
            var cache = caches[direction.ordinal()];
            if (cache == null) {
                cache = caches[direction.ordinal()] = BlockCapabilityCache.create(
                    capability, level, owner.getBlockPos().relative(direction),
                    direction.getOpposite(), () -> !owner.isRemoved(), () -> this.invalidate.accept(direction)
                );
            }

            return cache.getCapability();
        }

        final ServerLevel getLevel() {
            return Objects.requireNonNull((ServerLevel) owner.getLevel(), "Block entity is not in a level");
        }
    }

    private static class PeripheralAccess extends ComponentAccessImpl<IPeripheral> {
        PeripheralAccess(BlockEntity owner, Consumer<Direction> invalidate) {
            super(owner, PeripheralCapability.get(), invalidate);
        }

        @Nullable
        @Override
        public IPeripheral get(Direction direction) {
            var result = super.get(direction);
            if (result != null) return result;

            var cache = caches[direction.ordinal()];
            return Peripherals.getGenericPeripheral(cache.level(), cache.pos(), cache.context(), cache.level().getBlockEntity(cache.pos()));
        }
    }
}
