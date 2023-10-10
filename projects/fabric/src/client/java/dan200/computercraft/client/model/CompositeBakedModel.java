// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.model;

import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A {@link BakedModel} formed from two or more other models stitched together.
 */
public class CompositeBakedModel extends ForwardingBakedModel {
    private final boolean isVanillaAdapter;
    private final List<BakedModel> models;

    public CompositeBakedModel(List<BakedModel> models) {
        wrapped = models.get(0);
        isVanillaAdapter = models.stream().allMatch(FabricBakedModel::isVanillaAdapter);
        this.models = models;
    }

    public static BakedModel of(List<BakedModel> models) {
        return models.size() == 1 ? models.get(0) : new CompositeBakedModel(models);
    }

    @Override
    public boolean isVanillaAdapter() {
        return isVanillaAdapter;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState blockState, @Nullable Direction face, RandomSource rand) {
        @SuppressWarnings({ "unchecked", "rawtypes" })
        List<BakedQuad>[] quads = new List[models.size()];

        var i = 0;
        for (var model : models) quads[i++] = model.getQuads(blockState, face, rand);
        return new ConcatListView(quads);
    }

    @Override
    public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, RenderContext context) {
        for (var model : models) model.emitBlockQuads(blockView, state, pos, randomSupplier, context);
    }

    @Override
    public void emitItemQuads(ItemStack stack, Supplier<RandomSource> randomSupplier, RenderContext context) {
        for (var model : models) model.emitItemQuads(stack, randomSupplier, context);
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
