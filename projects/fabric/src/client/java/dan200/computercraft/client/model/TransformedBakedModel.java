/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.model;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link BakedModel} which applies a transformation matrix to its underlying quads.
 */
public class TransformedBakedModel extends CustomBakedModel {
    private static final int STRIDE = DefaultVertexFormat.BLOCK.getIntegerSize();
    private static final int POS_OFFSET = findOffset(DefaultVertexFormat.BLOCK, DefaultVertexFormat.ELEMENT_POSITION);

    private final Matrix4f transformation;
    private @Nullable TransformedQuads cache;

    public TransformedBakedModel(BakedModel model, Transformation transformation) {
        super(model);
        this.transformation = transformation.getMatrix();
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState blockState, @Nullable Direction face, RandomSource rand) {
        var cache = this.cache;
        var quads = wrapped.getQuads(blockState, face, rand);
        if (quads.isEmpty()) return List.of();

        // We do some basic caching here to avoid recomputing every frame. Most turtle models don't have culled faces,
        // so it's not worth being smarter here.
        if (cache != null && quads.equals(cache.original())) return cache.transformed();

        List<BakedQuad> transformed = new ArrayList<>(quads.size());
        for (var quad : quads) transformed.add(transformQuad(quad));
        this.cache = new TransformedQuads(quads, transformed);
        return transformed;
    }

    private BakedQuad transformQuad(BakedQuad quad) {
        var vertexData = quad.getVertices().clone();
        for (var i = 0; i < 4; i++) {
            // Apply the matrix to our position
            var start = STRIDE * i + POS_OFFSET;

            var x = Float.intBitsToFloat(vertexData[start]);
            var y = Float.intBitsToFloat(vertexData[start + 1]);
            var z = Float.intBitsToFloat(vertexData[start + 2]);

            // Transform the position
            var pos = new Vector4f(x, y, z, 1);
            transformation.transformProject(pos);

            vertexData[start] = Float.floatToRawIntBits(pos.x());
            vertexData[start + 1] = Float.floatToRawIntBits(pos.y());
            vertexData[start + 2] = Float.floatToRawIntBits(pos.z());
        }

        return new BakedQuad(vertexData, quad.getTintIndex(), quad.getDirection(), quad.getSprite(), quad.isShade());
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
