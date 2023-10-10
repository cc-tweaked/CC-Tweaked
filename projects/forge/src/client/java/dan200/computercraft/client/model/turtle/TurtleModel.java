// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.model.turtle;

import com.mojang.blaze3d.vertex.PoseStack;
import dan200.computercraft.client.model.TransformedBakedModel;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.model.BakedModelWrapper;

import java.util.List;
import java.util.function.Function;

/**
 * The custom model for turtle items, which renders tools and overlays as part of the model.
 *
 * @see TurtleModelParts
 */
public class TurtleModel extends BakedModelWrapper<BakedModel> {
    private final TurtleModelParts<List<BakedModel>> parts;

    public TurtleModel(BakedModel familyModel, BakedModel colourModel) {
        super(familyModel);
        parts = new TurtleModelParts<>(familyModel, colourModel, TransformedBakedModel::new, Function.identity());
    }

    @Override
    public BakedModel applyTransform(ItemDisplayContext transform, PoseStack poseStack, boolean applyLeftHandTransform) {
        originalModel.applyTransform(transform, poseStack, applyLeftHandTransform);
        return this;
    }

    @Override
    public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
        return parts.getModel(stack);
    }
}
