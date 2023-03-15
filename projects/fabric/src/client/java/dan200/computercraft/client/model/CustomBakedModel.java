// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.model;

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
import java.util.List;
import java.util.function.Supplier;

/**
 * A subclass of {@link ForwardingBakedModel} which doesn't forward rendering.
 */
public abstract class CustomBakedModel extends ForwardingBakedModel {
    public CustomBakedModel(BakedModel wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public abstract List<BakedQuad> getQuads(@Nullable BlockState blockState, @Nullable Direction face, RandomSource rand);

    @Override
    public final void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, RenderContext context) {
        context.bakedModelConsumer().accept(this);
    }

    @Override
    public final void emitItemQuads(ItemStack stack, Supplier<RandomSource> randomSupplier, RenderContext context) {
        context.bakedModelConsumer().accept(this);
    }
}
