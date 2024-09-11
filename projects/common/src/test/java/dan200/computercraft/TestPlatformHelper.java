// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft;

import com.google.auto.service.AutoService;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.ArgumentType;
import dan200.computercraft.api.network.wired.WiredElement;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.impl.AbstractComputerCraftAPI;
import dan200.computercraft.impl.ComputerCraftAPIService;
import dan200.computercraft.shared.config.ConfigFile;
import dan200.computercraft.shared.network.container.ContainerData;
import dan200.computercraft.shared.platform.*;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

@AutoService({ PlatformHelper.class, ComputerCraftAPIService.class })
public class TestPlatformHelper extends AbstractComputerCraftAPI implements PlatformHelper {
    @Override
    public boolean isModLoaded(String id) {
        return false;
    }

    @Override
    public ConfigFile.Builder createConfigBuilder() {
        throw new UnsupportedOperationException("Cannot create config file inside tests");
    }

    @Override
    public <T> RegistrationHelper<T> createRegistrationHelper(ResourceKey<Registry<T>> registry) {
        throw new UnsupportedOperationException("Cannot query registry inside tests");
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
    public <C extends AbstractContainerMenu, T extends ContainerData> MenuType<C> createMenuType(StreamCodec<RegistryFriendlyByteBuf, T> reader, ContainerData.Factory<C, T> factory) {
        throw new UnsupportedOperationException("Cannot create MenuType inside tests");
    }

    @Override
    public void openMenu(Player player, MenuProvider owner, ContainerData menu) {
        throw new UnsupportedOperationException("Cannot open menu inside tests");
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
    public List<ItemStack> getRecipeRemainingItems(ServerPlayer player, Recipe<CraftingInput> recipe, CraftingInput container) {
        throw new UnsupportedOperationException("Cannot query recipes inside tests");
    }

    @Override
    public void onItemCrafted(ServerPlayer player, CraftingInput container, ItemStack stack) {
        throw new UnsupportedOperationException("Cannot interact with the world inside tests");
    }

    @Override
    public String getInstalledVersion() {
        return "1.0";
    }
}
