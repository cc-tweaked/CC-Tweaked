// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dan200.computercraft.client.model.turtle.ModelTransformer;
import dan200.computercraft.client.platform.ClientPlatformHelper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector4f;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Utilities for rendering {@link BakedModel}s and {@link BakedQuad}s.
 */
public final class ModelRenderer {
    private ModelRenderer() {
    }

    /**
     * Render a list of {@linkplain BakedQuad quads} to a buffer.
     * <p>
     * This is not intended to be used directly, but instead by {@link ClientPlatformHelper#renderBakedModel(PoseStack, MultiBufferSource, BakedModel, int, int, int[])}. The
     * implementation here is pretty similar to {@link ItemRenderer#renderQuadList(PoseStack, VertexConsumer, List, ItemStack, int, int)},
     * but supports inverted quads (i.e. those with a negative scale).
     *
     * @param transform     The current matrix transformation to apply.
     * @param buffer        The buffer to draw to.
     * @param quads         The quads to draw.
     * @param lightmapCoord The current packed lightmap coordinate.
     * @param overlayLight  The current overlay light.
     * @param tints         Block colour tints to apply to the model.
     */
    public static void renderQuads(PoseStack transform, VertexConsumer buffer, List<BakedQuad> quads, int lightmapCoord, int overlayLight, @Nullable int[] tints) {
        var matrix = transform.last();
        var inverted = matrix.pose().determinant() < 0;

        for (var bakedquad : quads) {
            var tint = -1;
            if (tints != null && bakedquad.isTinted()) {
                var idx = bakedquad.getTintIndex();
                if (idx >= 0 && idx < tints.length) tint = tints[bakedquad.getTintIndex()];
            }

            var r = (float) (tint >> 16 & 255) / 255.0F;
            var g = (float) (tint >> 8 & 255) / 255.0F;
            var b = (float) (tint & 255) / 255.0F;
            putBulkQuad(buffer, matrix, bakedquad, r, g, b, lightmapCoord, overlayLight, inverted);
        }
    }

    /**
     * A version of {@link VertexConsumer#putBulkData(PoseStack.Pose, BakedQuad, float, float, float, int, int)} which
     * will reverse vertex order when the matrix is inverted.
     *
     * @param buffer        The buffer to draw to.
     * @param pose          The current matrix stack.
     * @param quad          The quad to draw.
     * @param red           The red tint of this quad.
     * @param green         The  green tint of this quad.
     * @param blue          The blue tint of this quad.
     * @param lightmapCoord The lightmap coordinate
     * @param overlayLight  The overlay light.
     * @param invert        Whether to reverse the order of this quad.
     */
    private static void putBulkQuad(VertexConsumer buffer, PoseStack.Pose pose, BakedQuad quad, float red, float green, float blue, int lightmapCoord, int overlayLight, boolean invert) {
        var matrix = pose.pose();
        // It's a little dubious to transform using this matrix rather than the normal matrix. This mirrors the logic in
        // Direction.rotate (so not out of nowhere!), but is a little suspicious.
        var dirNormal = quad.getDirection().getNormal();
        var vector = new Vector4f();

        matrix.transform(dirNormal.getX(), dirNormal.getY(), dirNormal.getZ(), 0.0f, vector).normalize();
        float normalX = vector.x(), normalY = vector.y(), normalZ = vector.z();

        var vertices = quad.getVertices();
        for (var vertex = 0; vertex < 4; vertex++) {
            var i = ModelTransformer.getVertexOffset(vertex, invert);

            var x = Float.intBitsToFloat(vertices[i]);
            var y = Float.intBitsToFloat(vertices[i + 1]);
            var z = Float.intBitsToFloat(vertices[i + 2]);

            matrix.transform(x, y, z, 1, vector);

            var u = Float.intBitsToFloat(vertices[i + 4]);
            var v = Float.intBitsToFloat(vertices[i + 5]);
            buffer.vertex(
                vector.x(), vector.y(), vector.z(),
                red, green, blue, 1.0F, u, v, overlayLight, lightmapCoord,
                normalX, normalY, normalZ
            );
        }
    }
}
