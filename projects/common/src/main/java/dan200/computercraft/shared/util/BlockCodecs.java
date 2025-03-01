// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.util;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dan200.computercraft.shared.platform.RegistryEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Additional codecs for block properties.
 */
public final class BlockCodecs {
    private BlockCodecs() {
    }

    public static <B extends Block> RecordCodecBuilder<B, BlockBehaviour.Properties> propertiesCodec() {
        return BlockBehaviour.Properties.CODEC.fieldOf("properties").forGetter(BlockBehaviour::properties);
    }

    @SuppressWarnings("unchecked")
    public static <B extends Block, E extends BlockEntityType<?>> RecordCodecBuilder<B, RegistryEntry<E>> blockEntityCodec(Function<B, RegistryEntry<E>> getter) {
        return RegistryEntry.codec(BuiltInRegistries.BLOCK_ENTITY_TYPE)
            .xmap(x -> (RegistryEntry<E>) x, x -> x)
            .fieldOf("block_entity").forGetter(getter);
    }

    public static <B extends Block, E extends BlockEntityType<?>> MapCodec<B> blockWithBlockEntityCodec(
        BiFunction<BlockBehaviour.Properties, RegistryEntry<E>, B> factory,
        Function<B, RegistryEntry<E>> getter
    ) {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(propertiesCodec(), blockEntityCodec(getter)).apply(instance, factory));
    }
}
