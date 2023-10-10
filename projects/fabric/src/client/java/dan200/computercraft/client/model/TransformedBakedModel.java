// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.model;

import com.mojang.math.Transformation;
import dan200.computercraft.client.model.turtle.ModelTransformer;
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
 * A {@link BakedModel} which applies a transformation matrix to its underlying quads.
 *
 * @see ModelTransformer
 */
public class TransformedBakedModel extends ForwardingBakedModel {
    private final FabricModelTransformer transformation;

    public TransformedBakedModel(BakedModel model, Transformation transformation) {
        wrapped = model;
        this.transformation = new FabricModelTransformer(transformation);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState blockState, @Nullable Direction face, RandomSource rand) {
        return transformation.transform(wrapped.getQuads(blockState, face, rand));
    }

    @Override
    public final void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, RenderContext context) {
        super.emitBlockQuads(blockView, state, pos, randomSupplier, context);
        context.popTransform();
    }

    @Override
    public final void emitItemQuads(ItemStack stack, Supplier<RandomSource> randomSupplier, RenderContext context) {
        context.pushTransform(transformation);
        super.emitItemQuads(stack, randomSupplier, context);
        context.popTransform();
    }
}
