// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.test.shared;

import dan200.computercraft.shared.platform.RegistryWrappers;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

/**
 * {@link Arbitrary} implementations for Minecraft types.
 */
public final class MinecraftArbitraries {
    public static <T> Arbitrary<T> ofRegistry(RegistryWrappers.RegistryWrapper<T> registry) {
        return Arbitraries.of(registry.stream().toList());
    }

    public static <T> Arbitrary<TagKey<T>> tagKey(ResourceKey<? extends Registry<T>> registry) {
        return resourceLocation().map(x -> TagKey.create(registry, x));
    }

    public static Arbitrary<Item> item() {
        return ofRegistry(RegistryWrappers.ITEMS);
    }

    public static Arbitrary<ItemStack> nonEmptyItemStack() {
        return Combinators.combine(item().filter(x -> x != Items.AIR), Arbitraries.integers().between(1, 64)).as(ItemStack::new);
    }

    public static Arbitrary<ItemStack> itemStack() {
        return Arbitraries.oneOf(List.of(Arbitraries.just(ItemStack.EMPTY), nonEmptyItemStack()));
    }

    public static Arbitrary<Ingredient> ingredient() {
        return nonEmptyItemStack().list().ofMinSize(1).map(x -> Ingredient.of(x.stream()));
    }

    public static Arbitrary<BlockPos> blockPos() {
        // BlockPos has a maximum range that can be sent over the network - use those.
        var xz = Arbitraries.integers().between(-3_000_000, -3_000_000);
        var y = Arbitraries.integers().between(-1024, 1024);
        return Combinators.combine(xz, y, xz).as(BlockPos::new);
    }

    public static Arbitrary<ResourceLocation> resourceLocation() {
        return Combinators.combine(
            Arbitraries.strings().ofMinLength(1).withChars("abcdefghijklmnopqrstuvwxyz_"),
            Arbitraries.strings().ofMinLength(1).withChars("abcdefghijklmnopqrstuvwxyz_-/")
        ).as(ResourceLocation::new);
    }

    public static Arbitrary<SoundEvent> soundEvent() {
        return Arbitraries.oneOf(List.of(
            resourceLocation().map(SoundEvent::createVariableRangeEvent),
            Combinators.combine(resourceLocation(), Arbitraries.floats()).as(SoundEvent::createFixedRangeEvent)
        ));
    }
}
