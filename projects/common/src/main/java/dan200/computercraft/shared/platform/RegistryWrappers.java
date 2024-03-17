// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.platform;

import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.IdMap;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;

import javax.annotation.Nullable;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Mimics {@link Registry} but using {@link PlatformHelper}'s recipe abstractions.
 */
public final class RegistryWrappers {
    public static final RegistryWrapper<Item> ITEMS = PlatformHelper.get().wrap(Registries.ITEM);
    public static final RegistryWrapper<Block> BLOCKS = PlatformHelper.get().wrap(Registries.BLOCK);
    public static final RegistryWrapper<BlockEntityType<?>> BLOCK_ENTITY_TYPES = PlatformHelper.get().wrap(Registries.BLOCK_ENTITY_TYPE);
    public static final RegistryWrapper<Fluid> FLUIDS = PlatformHelper.get().wrap(Registries.FLUID);
    public static final RegistryWrapper<Enchantment> ENCHANTMENTS = PlatformHelper.get().wrap(Registries.ENCHANTMENT);
    public static final RegistryWrapper<ArgumentTypeInfo<?, ?>> COMMAND_ARGUMENT_TYPES = PlatformHelper.get().wrap(Registries.COMMAND_ARGUMENT_TYPE);
    public static final RegistryWrapper<RecipeSerializer<?>> RECIPE_SERIALIZERS = PlatformHelper.get().wrap(Registries.RECIPE_SERIALIZER);
    public static final RegistryWrapper<MenuType<?>> MENU = PlatformHelper.get().wrap(Registries.MENU);

    public interface RegistryWrapper<T> extends IdMap<T> {
        ResourceLocation getKey(T object);

        T get(ResourceLocation location);

        @Nullable
        T tryGet(ResourceLocation location);

        default Stream<T> stream() {
            return StreamSupport.stream(spliterator(), false);
        }
    }

    private RegistryWrappers() {
    }

    public static <K> void writeKey(FriendlyByteBuf buf, RegistryWrapper<K> registry, K object) {
        buf.writeResourceLocation(registry.getKey(object));
    }

    public static <K> K readKey(FriendlyByteBuf buf, RegistryWrapper<K> registry) {
        var id = buf.readResourceLocation();
        return registry.get(id);
    }
}
