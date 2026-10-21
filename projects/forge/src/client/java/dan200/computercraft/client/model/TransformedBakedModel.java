// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.model;

import dan200.computercraft.client.model.turtle.ModelTransformer;
import dan200.computercraft.client.model.turtle.TurtleModelParts;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A {@link BakedModel} which applies a transformation matrix to its underlying quads.
 *
 * @see ModelTransformer
 */
public class TransformedBakedModel extends BakedModelWrapper<BakedModel> {
    private final ModelTransformer transformation;

    public TransformedBakedModel(TurtleModelParts.TurtleModelPart part) {
        super(part.model());
        this.transformation = new ModelTransformer(part.matrix(), part.side());
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return getQuads(state, side, rand, ModelData.EMPTY, null);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData extraData, @Nullable RenderType renderType) {
        return transformation.transform(originalModel.getQuads(state, side, rand, extraData, renderType));
    }
}
