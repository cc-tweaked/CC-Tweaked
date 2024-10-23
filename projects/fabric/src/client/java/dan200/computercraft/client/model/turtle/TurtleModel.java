// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.model.turtle;

import dan200.computercraft.client.model.CompositeBakedModel;
import dan200.computercraft.client.model.TransformedBakedModel;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The custom model for turtle items, which renders tools and overlays as part of the model.
 *
 * @see TurtleModelParts
 */
public class TurtleModel extends ForwardingBakedModel {
    private final TurtleModelParts<BakedModel> parts;

    private final BakedOverrides overrides;

    public TurtleModel(ModelBaker baker, BakedModel familyModel, BakedModel colourModel) {
        wrapped = familyModel;
        parts = new TurtleModelParts<>(familyModel, colourModel, TransformedBakedModel::new, CompositeBakedModel::of);
        overrides = new BakedOverrides(baker, List.of()) {
            @Override
            public @Nullable BakedModel findOverride(ItemStack stack, @Nullable ClientLevel clientLevel, @Nullable LivingEntity entity, int seed) {
                return parts.getModel(stack);
            }
        };
    }

    @Override
    public BakedOverrides overrides() {
        return overrides;
    }
}
