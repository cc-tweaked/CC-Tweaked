// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.client.turtle;

import com.mojang.math.Transformation;
import dan200.computercraft.api.client.TransformedModel;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import org.joml.Matrix4f;

class TurtleUpgradeModellers {
    private static final Transformation leftTransform = getMatrixFor(-0.40625f);
    private static final Transformation rightTransform = getMatrixFor(0.40625f);

    private static Transformation getMatrixFor(float offset) {
        var matrix = new Matrix4f();
        matrix.set(new float[]{
            0.0f, 0.0f, -1.0f, 1.0f + offset,
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, -1.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 0.0f, 1.0f,
        });
        matrix.transpose();
        return new Transformation(matrix);
    }

    static final TurtleUpgradeModeller<ITurtleUpgrade> FLAT_ITEM = (upgrade, turtle, side) ->
        TransformedModel.of(upgrade.getCraftingItem(), side == TurtleSide.LEFT ? leftTransform : rightTransform);
}
