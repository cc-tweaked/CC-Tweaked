/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.client.turtle;

import com.mojang.math.Matrix4f;
import com.mojang.math.Transformation;
import dan200.computercraft.api.client.TransformedModel;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;

import java.nio.FloatBuffer;

class TurtleUpgradeModellers {
    private static final Transformation leftTransform = getMatrixFor(-0.40625f);
    private static final Transformation rightTransform = getMatrixFor(0.40625f);

    private static Transformation getMatrixFor(float offset) {
        var matrix = new Matrix4f();
        matrix.load(FloatBuffer.wrap(new float[]{
            0.0f, 0.0f, -1.0f, 1.0f + offset,
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, -1.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 0.0f, 1.0f,
        }));
        matrix.transpose();
        return new Transformation(matrix);
    }

    static final TurtleUpgradeModeller<ITurtleUpgrade> FLAT_ITEM = (upgrade, turtle, side) ->
        TransformedModel.of(upgrade.getCraftingItem(), side == TurtleSide.LEFT ? leftTransform : rightTransform);
}
