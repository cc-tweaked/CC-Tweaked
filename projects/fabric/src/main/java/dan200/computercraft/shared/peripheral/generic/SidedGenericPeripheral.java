/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
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
