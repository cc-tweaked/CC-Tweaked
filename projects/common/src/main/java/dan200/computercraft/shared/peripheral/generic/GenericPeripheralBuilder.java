// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.peripheral.generic;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.PeripheralType;
import dan200.computercraft.core.methods.NamedMethod;
import dan200.computercraft.core.methods.PeripheralMethod;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger LOG = LoggerFactory.getLogger(GenericPeripheralBuilder.class);

    private @Nullable String name;
    private final Set<String> additionalTypes = new HashSet<>(0);
    private final ArrayList<SaturatedMethod> methods = new ArrayList<>();

    @Nullable
    IPeripheral toPeripheral(BlockEntity blockEntity, Direction side) {
        if (methods.isEmpty()) return null;

        String type;
        if (name == null) {
            var typeId = BlockEntityType.getKey(blockEntity.getType());
            if (typeId == null) {
                LOG.error(
                    "Block entity {} for {} was not registered. Skipping creating a generic peripheral for it.",
                    blockEntity, blockEntity.getBlockState().getBlock()
                );
                return null;
            }

            type = typeId.toString();
        } else {
            type = name;
        }

        methods.trimToSize();
        return new GenericPeripheral(blockEntity, side, type, additionalTypes, methods);
    }

    void addMethod(Object target, String name, PeripheralMethod method, @Nullable NamedMethod<PeripheralMethod> info) {
        methods.add(new SaturatedMethod(target, name, method));

        // If we have a peripheral type, use it. Always pick the smallest one, so it's consistent (assuming mods
        // don't change).
        var type = info == null ? null : info.genericType();
        if (type != null && type.getPrimaryType() != null) {
            var primaryType = type.getPrimaryType();
            if (this.name == null || this.name.compareTo(primaryType) > 0) this.name = primaryType;
        }
        if (type != null) additionalTypes.addAll(type.getAdditionalTypes());
    }
}
