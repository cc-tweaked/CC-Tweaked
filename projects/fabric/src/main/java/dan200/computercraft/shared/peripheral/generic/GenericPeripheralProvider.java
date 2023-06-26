// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.peripheral.generic;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.peripheral.generic.methods.InventoryMethods;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

public class GenericPeripheralProvider {
    interface Lookup<T> {
        @Nullable
        T find(Level world, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, Direction context);
    }

    private static final List<Lookup<?>> lookups = List.of(
        InventoryMethods::extractContainer
    );

    @Nullable
    public static IPeripheral getPeripheral(Level level, BlockPos pos, Direction side, @Nullable BlockEntity blockEntity) {
        if (blockEntity == null) return null;

        var builder = new GenericPeripheralBuilder();
        builder.addMethods(blockEntity);

        for (var lookup : lookups) {
            var contents = lookup.find(level, pos, blockEntity.getBlockState(), blockEntity, side);
            if (contents != null) builder.addMethods(contents);
        }

        return builder.toPeripheral(blockEntity, side);
    }
}
