/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.render;

import java.util.List;

import javax.vecmath.Vector4f;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormatElement;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Quaternion;

/**
 * Transforms vertices of a model, remaining aware of winding order, and rearranging vertices if needed.
 */
@Environment(EnvType.CLIENT)
public final class ModelTransformer {
    private static final Matrix4f identity;

    static {
        identity = new Matrix4f();
        identity.loadIdentity();
    }

    private ModelTransformer() {
    }

    public static void transformQuadsTo(List<BakedQuad> output, List<BakedQuad> input, Matrix4f transform) {
        transformQuadsTo(VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL, output, input, transform);
    }

    public static void transformQuadsTo(VertexFormat format, List<BakedQuad> output, List<BakedQuad> input, Matrix4f transform) {
        if (transform == null || transform.equals(identity)) {
            output.addAll(input);
        } else {
            for (BakedQuad quad : input) {
                output.add(doTransformQuad(format, quad, transform));
            }
        }
    }

    private static BakedQuad doTransformQuad(VertexFormat format, BakedQuad quad, Matrix4f transform) {
        int[] vertexData = quad.getVertexData()
                               .clone();
        int offset = 0;
        BakedQuad copy = new BakedQuad(vertexData, -1, quad.getFace(), quad.sprite, true);
        for (int i = 0; i < format.getElements().size(); ++i) // For each vertex element
        {
            VertexFormatElement element = format.getElements().get(i);
            if (element.getType() == VertexFormatElement.Type.POSITION && element.getFormat() == VertexFormatElement.Format.FLOAT && element.getSize() == 3) // When we find a position
                // element
            {
                for (int j = 0; j < 4; ++j) // For each corner of the quad
                {
                    int start = offset + j * format.getVertexSize();
                    if ((start % 4) == 0) {
                        start = start / 4;

                        // Extract the position
                        Quaternion pos = new Quaternion(Float.intBitsToFloat(vertexData[start]),
                                                    Float.intBitsToFloat(vertexData[start + 1]),
                                                    Float.intBitsToFloat(vertexData[start + 2]),
                                                    1);

                        // Transform the position
                        transform.multiply(pos);

                        // Insert the position
                        vertexData[start] = Float.floatToRawIntBits(pos.getX());
                        vertexData[start + 1] = Float.floatToRawIntBits(pos.getY());
                        vertexData[start + 2] = Float.floatToRawIntBits(pos.getZ());
                    }
                }
            }
            offset += element.getSize();
        }
        return copy;
    }

    public static BakedQuad transformQuad(VertexFormat format, BakedQuad input, Matrix4f transform) {
        if (transform == null || transform.equals(identity)) {
            return input;
        }
        return doTransformQuad(format, input, transform);
    }
}
