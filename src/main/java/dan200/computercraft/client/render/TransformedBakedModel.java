/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import com.mojang.math.Transformation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.QuadTransformers;
import net.minecraftforge.client.model.data.ModelData;

import javax.annotation.Nullable;
import java.util.List;

public class TransformedBakedModel extends BakedModelWrapper<BakedModel> {
    private final Transformation transformation;
    private final boolean invert;
    private @Nullable TransformedQuads cache;

    public TransformedBakedModel(BakedModel model, Transformation transformation) {
        super(model);
        this.transformation = transformation;
        invert = transformation.getNormalMatrix().determinant() < 0;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return getQuads(state, side, rand, ModelData.EMPTY, null);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData extraData, @Nullable RenderType renderType) {
        var cache = this.cache;
        var quads = originalModel.getQuads(state, side, rand, extraData, renderType);
        if (quads.isEmpty()) return List.of();

        // We do some basic caching here to avoid recomputing every frame. Most turtle models don't have culled faces,
        // so it's not worth being smarter here.
        if (cache != null && quads.equals(cache.original())) return cache.transformed();

        var transformed = QuadTransformers.applying(transformation).process(quads);
        this.cache = new TransformedQuads(quads, transformed);
        return transformed;
    }

    private record TransformedQuads(List<BakedQuad> original, List<BakedQuad> transformed) {
    }
}
