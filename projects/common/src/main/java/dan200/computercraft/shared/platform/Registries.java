/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.platform;

import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
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
public final class Registries {
    public static final RegistryWrapper<Item> ITEMS = PlatformHelper.get().wrap(Registry.ITEM_REGISTRY);
    public static final RegistryWrapper<Block> BLOCKS = PlatformHelper.get().wrap(Registry.BLOCK_REGISTRY);
    public static final RegistryWrapper<BlockEntityType<?>> BLOCK_ENTITY_TYPES = PlatformHelper.get().wrap(Registry.BLOCK_ENTITY_TYPE_REGISTRY);
    public static final RegistryWrapper<Fluid> FLUIDS = PlatformHelper.get().wrap(Registry.FLUID_REGISTRY);
    public static final RegistryWrapper<Enchantment> ENCHANTMENTS = PlatformHelper.get().wrap(Registry.ENCHANTMENT_REGISTRY);
    public static final RegistryWrapper<ArgumentTypeInfo<?, ?>> COMMAND_ARGUMENT_TYPES = PlatformHelper.get().wrap(Registry.COMMAND_ARGUMENT_TYPE_REGISTRY);
    public static final RegistryWrapper<SoundEvent> SOUND_EVENTS = PlatformHelper.get().wrap(Registry.SOUND_EVENT_REGISTRY);
    public static final RegistryWrapper<RecipeSerializer<?>> RECIPE_SERIALIZERS = PlatformHelper.get().wrap(Registry.RECIPE_SERIALIZER_REGISTRY);
    public static final RegistryWrapper<MenuType<?>> MENU = PlatformHelper.get().wrap(Registry.MENU_REGISTRY);

    public interface RegistryWrapper<T> extends Iterable<T> {
        int getId(T object);

        ResourceLocation getKey(T object);

        T get(ResourceLocation location);

        @Nullable
        T tryGet(ResourceLocation location);

        T get(int id);

        default Stream<T> stream() {
            return StreamSupport.stream(spliterator(), false);
        }
    }

    private Registries() {
    }

    public static <K> void writeId(FriendlyByteBuf buf, RegistryWrapper<K> registry, K object) {
        buf.writeVarInt(registry.getId(object));
    }

    public static <K> K readId(FriendlyByteBuf buf, RegistryWrapper<K> registry) {
        var id = buf.readVarInt();
        return registry.get(id);
    }

    public static <K> void writeKey(FriendlyByteBuf buf, RegistryWrapper<K> registry, K object) {
        buf.writeResourceLocation(registry.getKey(object));
    }

    public static <K> K readKey(FriendlyByteBuf buf, RegistryWrapper<K> registry) {
        var id = buf.readResourceLocation();
        return registry.get(id);
    }
}
