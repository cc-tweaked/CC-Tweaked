// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.model.turtle;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Applies a {@link Transformation} (or rather a {@link Matrix4f}) to a list of {@link BakedQuad}s.
 * <p>
 * This does a little bit of magic compared with other system (i.e. Forge's {@code QuadTransformers}), as it needs to
 * handle flipping models upside down.
 * <p>
 * This is typically used with a {@link BakedModel} subclass - see the loader-specific projects.
 */
public class ModelTransformer {
    private static final int[] INVERSE_ORDER = new int[]{ 3, 2, 1, 0 };

    private static final int STRIDE = DefaultVertexFormat.BLOCK.getIntegerSize();
    private static final int POS_OFFSET = findOffset(DefaultVertexFormat.BLOCK, DefaultVertexFormat.ELEMENT_POSITION);

    protected final Matrix4f transformation;
    protected final boolean invert;
    private @Nullable TransformedQuads cache;

    public ModelTransformer(Transformation transformation) {
        this.transformation = transformation.getMatrix();
        invert = transformation.getMatrix().determinant() < 0;
    }

    public List<BakedQuad> transform(List<BakedQuad> quads) {
        if (quads.isEmpty()) return List.of();

        // We do some basic caching here to avoid recomputing every frame. Most turtle models don't have culled faces,
        // so it's not worth being smarter here.
        var cache = this.cache;
        if (cache != null && quads.equals(cache.original())) return cache.transformed();

        List<BakedQuad> transformed = new ArrayList<>(quads.size());
        for (var quad : quads) transformed.add(transformQuad(quad));
        this.cache = new TransformedQuads(quads, transformed);
        return transformed;
    }

    private BakedQuad transformQuad(BakedQuad quad) {
        var inputData = quad.getVertices();
        var outputData = new int[inputData.length];
        for (var i = 0; i < 4; i++) {
            var inStart = STRIDE * i;
            // Reverse the order of the quads if we're inverting
            var outStart = getVertexOffset(i, invert);
            System.arraycopy(inputData, inStart, outputData, outStart, STRIDE);

            // Apply the matrix to our position
            var inPosStart = inStart + POS_OFFSET;
            var outPosStart = outStart + POS_OFFSET;

            var x = Float.intBitsToFloat(inputData[inPosStart]);
            var y = Float.intBitsToFloat(inputData[inPosStart + 1]);
            var z = Float.intBitsToFloat(inputData[inPosStart + 2]);

            // Transform the position
            var pos = new Vector4f(x, y, z, 1);
            transformation.transformProject(pos);

            outputData[outPosStart] = Float.floatToRawIntBits(pos.x());
            outputData[outPosStart + 1] = Float.floatToRawIntBits(pos.y());
            outputData[outPosStart + 2] = Float.floatToRawIntBits(pos.z());
        }

        var direction = Direction.rotate(transformation, quad.getDirection());
        return new BakedQuad(outputData, quad.getTintIndex(), direction, quad.getSprite(), quad.isShade());
    }

    public static int getVertexOffset(int vertex, boolean invert) {
        return (invert ? ModelTransformer.INVERSE_ORDER[vertex] : vertex) * ModelTransformer.STRIDE;
    }

    private record TransformedQuads(List<BakedQuad> original, List<BakedQuad> transformed) {
    }

    private static int findOffset(VertexFormat format, VertexFormatElement element) {
        var offset = 0;
        for (var other : format.getElements()) {
            if (other == element) return offset / Integer.BYTES;
            offset += element.getByteSize();
        }
        throw new IllegalArgumentException("Cannot find " + element + " in " + format);
    }
}
