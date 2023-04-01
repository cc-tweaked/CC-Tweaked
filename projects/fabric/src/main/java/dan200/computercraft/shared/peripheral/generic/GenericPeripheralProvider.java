// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.peripheral.generic;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.asm.NamedMethod;
import dan200.computercraft.core.asm.PeripheralMethod;
import dan200.computercraft.shared.peripheral.generic.methods.InventoryMethods;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GenericPeripheralProvider {
    interface Lookup<T> {
        @Nullable
        T find(Level world, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, Direction context);
    }

    private static final List<Lookup<?>> lookups = List.of(
        InventoryMethods::extractContainer
    );

    @Nullable
    public static IPeripheral getPeripheral(Level world, BlockPos pos, Direction side, @Nullable BlockEntity blockEntity) {
        if (blockEntity == null) return null;

        var saturated = new GenericPeripheralBuilder();

        var tileMethods = PeripheralMethod.GENERATOR.getMethods(blockEntity.getClass());
        if (!tileMethods.isEmpty()) saturated.addMethods(blockEntity, tileMethods);

        for (var lookup : lookups) {
            var contents = lookup.find(world, pos, blockEntity.getBlockState(), blockEntity, side);
            if (contents == null) continue;

            var methods = PeripheralMethod.GENERATOR.getMethods(contents.getClass());
            if (!methods.isEmpty()) saturated.addMethods(contents, methods);
        }

        return saturated.toPeripheral(blockEntity);
    }

    private static class GenericPeripheralBuilder {
        private @Nullable String name;
        private final Set<String> additionalTypes = new HashSet<>(0);
        private final ArrayList<SaturatedMethod> methods = new ArrayList<>(0);

        @Nullable
        IPeripheral toPeripheral(BlockEntity tile) {
            if (methods.isEmpty()) return null;

            methods.trimToSize();
            return new GenericPeripheral(tile, name, additionalTypes, methods);
        }

        void addMethods(Object target, List<NamedMethod<PeripheralMethod>> methods) {
            var saturatedMethods = this.methods;
            saturatedMethods.ensureCapacity(saturatedMethods.size() + methods.size());
            for (var method : methods) {
                saturatedMethods.add(new SaturatedMethod(target, method));

                // If we have a peripheral type, use it. Always pick the smallest one, so it's consistent (assuming mods
                // don't change).
                var type = method.getGenericType();
                if (type != null && type.getPrimaryType() != null) {
                    var name = type.getPrimaryType();
                    if (this.name == null || this.name.compareTo(name) > 0) this.name = name;
                }
                if (type != null) additionalTypes.addAll(type.getAdditionalTypes());
            }
        }
    }
}
