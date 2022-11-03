/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import com.mojang.math.Transformation;
import dan200.computercraft.api.client.TransformedModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.QuadTransformers;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.List;

public class TransformedBakedModel extends BakedModelWrapper<BakedModel> {
    private final Transformation transformation;
    private final boolean isIdentity;

    public TransformedBakedModel(BakedModel model, Transformation transformation) {
        super(model);
        this.transformation = transformation;
        isIdentity = transformation.isIdentity();
    }

    public TransformedBakedModel(TransformedModel model) {
        this(model.getModel(), model.getMatrix());
    }

    @Nonnull
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @Nonnull RandomSource rand) {
        return getQuads(state, side, rand, ModelData.EMPTY, null);
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData extraData, @Nullable RenderType renderType) {
        var quads = originalModel.getQuads(state, side, rand, extraData, renderType);
        return isIdentity ? quads : QuadTransformers.applying(transformation).process(quads);
    }

    public TransformedBakedModel composeWith(Transformation other) {
        return new TransformedBakedModel(originalModel, other.compose(transformation));
    }
}
