// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.peripheral.generic;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

public class SidedGenericPeripheral extends GenericPeripheral {
    private final Direction direction;

    SidedGenericPeripheral(BlockEntity tile, Direction direction, @Nullable String name, Set<String> additionalTypes, List<SaturatedMethod> methods) {
        super(tile, name, additionalTypes, methods);
        this.direction = direction;
    }

    public Direction direction() {
        return direction;
    }
}
