/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.function.BiFunction;

/**
 * This extends {@linkplain dan200.computercraft.impl.PlatformHelper the API's loader abstraction layer}, adding
 * additional methods used by the actual mod.
 */
public interface PlatformHelper extends dan200.computercraft.impl.PlatformHelper {
    /**
     * Get the current {@link PlatformHelper} instance.
     *
     * @return The current instance.
     */
    static PlatformHelper get() {
        return (PlatformHelper) dan200.computercraft.impl.PlatformHelper.get();
    }

    /**
     * Wrap a Minecraft registry in our own abstraction layer.
     *
     * @param registry The registry to wrap.
     * @param <T>      The type of object stored in this registry.
     * @return The wrapped registry.
     */
    <T> Registries.RegistryWrapper<T> wrap(ResourceKey<Registry<T>> registry);

    /**
     * Create a registration helper for a specific registry.
     *
     * @param registry The registry we'll add entries to.
     * @param <T>      The type of object stored in the registry.
     * @return The registration helper.
     */
    <T> RegistrationHelper<T> createRegistrationHelper(ResourceKey<Registry<T>> registry);

    /**
     * A version of {@link #getRegistryObject(ResourceKey, ResourceLocation)} which allows missing entries.
     *
     * @param registry The registry to look up this object in.
     * @param id       The ID to look up.
     * @param <T>      The type of object the registry stores.
     * @return The registered object or {@code null}.
     */
    @Nullable
    <T> T tryGetRegistryObject(ResourceKey<Registry<T>> registry, ResourceLocation id);

    /**
     * Create a new block entity type which serves a particular block.
     *
     * @param factory The method which creates a new block entity with this type, typically the constructor.
     * @param block   The block this block entity exists on.
     * @param <T>     The type of block entity we're creating.
     * @return The new block entity type.
     */
    <T extends BlockEntity> BlockEntityType<T> createBlockEntityType(BiFunction<BlockPos, BlockState, T> factory, Block block);
}
