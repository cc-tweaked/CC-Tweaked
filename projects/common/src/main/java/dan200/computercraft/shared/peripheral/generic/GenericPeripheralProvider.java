// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.peripheral.generic;

import dan200.computercraft.api.lua.GenericSource;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.methods.MethodSupplier;
import dan200.computercraft.core.methods.PeripheralMethod;
import dan200.computercraft.shared.computer.core.ServerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A peripheral provider which finds methods from various {@linkplain GenericSource generic sources}.
 * <p>
 * Methods are found using the original block entity itself and a registered list of {@link ComponentLookup}s.
 *
 * @param <C> A platform-specific type, used for the invalidation callback.
 */
public final class GenericPeripheralProvider<C extends Runnable> {
    private final List<ComponentLookup<? super C>> lookups = new ArrayList<>();

    /**
     * Register a component lookup function.
     *
     * @param lookup The component lookup function.
     */
    public synchronized void registerLookup(ComponentLookup<? super C> lookup) {
        Objects.requireNonNull(lookup);
        if (!lookups.contains(lookup)) lookups.add(lookup);
    }

    public void forEachMethod(MethodSupplier<PeripheralMethod> methods, ServerLevel level, BlockPos pos, Direction side, BlockEntity blockEntity, C invalidate, MethodSupplier.TargetedConsumer<PeripheralMethod> consumer) {
        methods.forEachMethod(blockEntity, consumer);

        for (var lookup : lookups) {
            var contents = lookup.find(level, pos, blockEntity.getBlockState(), blockEntity, side, invalidate);
            if (contents != null) methods.forEachMethod(contents, consumer);
        }
    }

    @Nullable
    public IPeripheral getPeripheral(ServerLevel level, BlockPos pos, Direction side, @Nullable BlockEntity blockEntity, C invalidate) {
        if (blockEntity == null) return null;

        var builder = new GenericPeripheralBuilder();
        forEachMethod(ServerContext.get(level.getServer()).peripheralMethods(), level, pos, side, blockEntity, invalidate, builder::addMethod);
        return builder.toPeripheral(blockEntity, side);
    }
}
