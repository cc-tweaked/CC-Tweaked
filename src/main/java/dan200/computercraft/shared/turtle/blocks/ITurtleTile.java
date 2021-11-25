/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.blocks;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.computer.blocks.IComputerTile;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;

public interface ITurtleTile extends IComputerTile
{
    int getColour();

    ResourceLocation getOverlay();

    ITurtleUpgrade getUpgrade( TurtleSide side );

    ITurtleAccess getAccess();

    Vector3d getRenderOffset( float f );

    float getRenderYaw( float f );

    float getToolRenderAngle( TurtleSide side, float f );
}
