/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.model;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Stream;

/**
 * A {@link BakedModel} formed from two or more other models stitched together.
 */
public class CompositeBakedModel extends CustomBakedModel {
    private final List<BakedModel> models;

    public CompositeBakedModel(List<BakedModel> models) {
        super(models.get(0));
        this.models = models;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState blockState, @Nullable Direction face, RandomSource rand) {
        @SuppressWarnings({ "unchecked", "rawtypes" })
        List<BakedQuad>[] quads = new List[models.size()];

        var i = 0;
        for (var model : models) quads[i++] = model.getQuads(blockState, face, rand);
        return new ConcatListView(quads);
    }

    private static final class ConcatListView extends AbstractList<BakedQuad> {
        private final List<BakedQuad>[] quads;

        private ConcatListView(List<BakedQuad>[] quads) {
            this.quads = quads;
        }

        @Override
        public Iterator<BakedQuad> iterator() {
            return stream().iterator();
        }

        @Override
        public Stream<BakedQuad> stream() {
            return Arrays.stream(quads).flatMap(Collection::stream);
        }

        @Override
        public BakedQuad get(int index) {
            var i = index;
            for (var modelQuads : quads) {
                if (i < modelQuads.size()) return modelQuads.get(i);
                i -= modelQuads.size();
            }

            throw new IndexOutOfBoundsException(i);
        }

        @Override
        public int size() {
            var size = 0;
            for (var modelQuads : quads) size += modelQuads.size();
            return size;
        }
    }
}
