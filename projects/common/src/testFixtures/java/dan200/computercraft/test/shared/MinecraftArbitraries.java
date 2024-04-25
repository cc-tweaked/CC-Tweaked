// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.test.shared;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * {@link Arbitrary} implementations for Minecraft types.
 */
public final class MinecraftArbitraries {
    public static <T> Arbitrary<T> ofRegistry(Registry<T> registry) {
        return Arbitraries.of(registry.stream().toList());
    }

    public static <T> Arbitrary<TagKey<T>> tagKey(ResourceKey<? extends Registry<T>> registry) {
        return resourceLocation().map(x -> TagKey.create(registry, x));
    }

    public static Arbitrary<Item> item() {
        return ofRegistry(BuiltInRegistries.ITEM);
    }

    public static Arbitrary<ItemStack> nonEmptyItemStack() {
        return Combinators.combine(item().filter(x -> x != Items.AIR), Arbitraries.integers().between(1, 64)).as(ItemStack::new);
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
}
