// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.model.turtle;

import dan200.computercraft.client.model.CompositeBakedModel;
import dan200.computercraft.client.model.TransformedBakedModel;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class TurtleModel extends ForwardingBakedModel {
    private final TurtleModelParts parts;

    private final Map<TurtleModelParts.Combination, BakedModel> cachedModels = new HashMap<>();
    private final ItemOverrides overrides = new ItemOverrides() {
        @Override
        public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
            return cachedModels.computeIfAbsent(parts.getCombination(stack), TurtleModel.this::buildModel);
        }
    };

    public TurtleModel(BakedModel familyModel, BakedModel colourModel) {
        wrapped = familyModel;
        parts = new TurtleModelParts(familyModel, colourModel, TransformedBakedModel::new);
    }

    @Override
    public ItemOverrides getOverrides() {
        return overrides;
    }

    private BakedModel buildModel(TurtleModelParts.Combination combo) {
        var models = parts.buildModel(combo);
        return models.size() == 1 ? models.get(0) : new CompositeBakedModel(models);
    }
}
