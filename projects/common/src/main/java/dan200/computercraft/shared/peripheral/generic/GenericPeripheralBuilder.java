// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.peripheral.generic;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.PeripheralType;
import dan200.computercraft.core.methods.MethodSupplier;
import dan200.computercraft.core.methods.NamedMethod;
import dan200.computercraft.core.methods.PeripheralMethod;
import dan200.computercraft.shared.computer.core.ServerContext;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
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
    private final MethodSupplier<PeripheralMethod> peripheralMethods;

    private @Nullable String name;
    private final Set<String> additionalTypes = new HashSet<>(0);
    private final ArrayList<SaturatedMethod> methods = new ArrayList<>();

    GenericPeripheralBuilder(MinecraftServer server) {
        peripheralMethods = ServerContext.get(server).peripheralMethods();
    }

    @Nullable
    IPeripheral toPeripheral(BlockEntity blockEntity, Direction side) {
        if (methods.isEmpty()) return null;

        methods.trimToSize();
        return new GenericPeripheral(blockEntity, side, name, additionalTypes, methods);
    }

    boolean addMethods(Object target) {
        return peripheralMethods.forEachSelfMethod(target, (name, method, info) -> {
            methods.add(new SaturatedMethod(target, name, method));

            // If we have a peripheral type, use it. Always pick the smallest one, so it's consistent (assuming mods
            // don't change).
            var type = info == null ? null : info.genericType();
            if (type != null && type.getPrimaryType() != null) {
                var primaryType = type.getPrimaryType();
                if (this.name == null || this.name.compareTo(primaryType) > 0) this.name = primaryType;
            }
            if (type != null) additionalTypes.addAll(type.getAdditionalTypes());
        });
    }
}
