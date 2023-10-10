// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.model;

import com.mojang.math.Transformation;
import dan200.computercraft.client.model.turtle.ModelTransformer;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.core.Direction;
import org.joml.Vector3f;

/**
 * Extends {@link ModelTransformer} to also work as a {@link RenderContext.QuadTransform}.
 */
public class FabricModelTransformer extends ModelTransformer implements RenderContext.QuadTransform {
    public FabricModelTransformer(Transformation transformation) {
        super(transformation);
    }

    @Override
    public boolean transform(MutableQuadView quad) {
        var vec3 = new Vector3f();
        for (var i = 0; i < 4; i++) {
            quad.copyPos(i, vec3);
            transformation.transformPosition(vec3);
            quad.pos(i, vec3);
        }

        if (invert) {
            swapQuads(quad, 0, 3);
            swapQuads(quad, 1, 2);
        }

        var face = quad.nominalFace();
        if (face != null) quad.nominalFace(Direction.rotate(transformation, face));

        return true;
    }

    private static void swapQuads(MutableQuadView quad, int a, int b) {
        float aX = quad.x(a), aY = quad.y(a), aZ = quad.z(a), aU = quad.u(a), aV = quad.v(a);
        float bX = quad.x(b), bY = quad.y(b), bZ = quad.z(b), bU = quad.u(b), bV = quad.v(b);

        quad.pos(b, aX, aY, aZ).uv(b, aU, aV);
        quad.pos(a, bX, bY, bZ).uv(a, bU, bV);
    }
}
