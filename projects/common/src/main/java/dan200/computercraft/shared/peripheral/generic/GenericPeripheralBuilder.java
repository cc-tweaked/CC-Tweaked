// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.peripheral.generic;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.PeripheralType;
import dan200.computercraft.core.asm.NamedMethod;
import dan200.computercraft.core.asm.PeripheralMethod;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * A builder for a {@link GenericPeripheral}.
 * <p>
 * This handles building a list of {@linkplain SaturatedMethod methods} and computing the appropriate
 * {@link PeripheralType} from the {@linkplain NamedMethod#genericType() methods' peripheral types}.
 * <p>
 * See the platform-specific peripheral providers for the usage of this.
 */
final class GenericPeripheralBuilder {
    private @Nullable String name;
    private final Set<String> additionalTypes = new HashSet<>(0);
    private final ArrayList<SaturatedMethod> methods = new ArrayList<>(0);

    @Nullable
    IPeripheral toPeripheral(BlockEntity tile, Direction side) {
        if (methods.isEmpty()) return null;

        methods.trimToSize();
        return new GenericPeripheral(tile, side, name, additionalTypes, methods);
    }

    boolean addMethods(Object target) {
        var methods = PeripheralMethod.GENERATOR.getMethods(target.getClass());
        if (methods.isEmpty()) return false;

        var saturatedMethods = this.methods;
        saturatedMethods.ensureCapacity(saturatedMethods.size() + methods.size());
        for (var method : methods) {
            saturatedMethods.add(new SaturatedMethod(target, method.name(), method.method()));

            // If we have a peripheral type, use it. Always pick the smallest one, so it's consistent (assuming mods
            // don't change).
            var type = method.genericType();
            if (type != null && type.getPrimaryType() != null) {
                var name = type.getPrimaryType();
                if (this.name == null || this.name.compareTo(name) > 0) this.name = name;
            }
            if (type != null) additionalTypes.addAll(type.getAdditionalTypes());
        }

        return true;
    }
}
