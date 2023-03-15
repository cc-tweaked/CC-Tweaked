// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.turtle.blocks;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.computer.blocks.IComputerBlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public interface ITurtleBlockEntity extends IComputerBlockEntity {
    int getColour();

    @Nullable
    ResourceLocation getOverlay();

    @Nullable
    ITurtleUpgrade getUpgrade(TurtleSide side);

    ITurtleAccess getAccess();

    Vec3 getRenderOffset(float f);

    float getRenderYaw(float f);

    float getToolRenderAngle(TurtleSide side, float f);
}
