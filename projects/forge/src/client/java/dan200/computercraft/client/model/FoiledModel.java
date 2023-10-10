// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.model;

import dan200.computercraft.shared.util.ConsList;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A model wrapper which applies a glint/foil to the original model.
 */
public final class FoiledModel extends BakedModelWrapper<BakedModel> {
    public FoiledModel(BakedModel model) {
        super(model);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData extraData, @Nullable RenderType renderType) {
        return renderType == RenderType.glint()
            ? super.getQuads(state, side, rand, extraData, null)
            : super.getQuads(state, side, rand, extraData, renderType);
    }

    @Override
    public List<RenderType> getRenderTypes(ItemStack itemStack, boolean fabulous) {
        return new ConsList<>(fabulous ? RenderType.glintDirect() : RenderType.glint(), super.getRenderTypes(itemStack, fabulous));
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof FoiledModel other && originalModel.equals(other.originalModel));
    }

    @Override
    public int hashCode() {
        return originalModel.hashCode() ^ 1;
    }

}
