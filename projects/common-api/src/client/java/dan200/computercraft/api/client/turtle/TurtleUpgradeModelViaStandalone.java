// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.client.turtle;

import com.mojang.blaze3d.vertex.PoseStack;
import dan200.computercraft.api.client.StandaloneModel;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.component.DataComponentPatch;

public interface TurtleUpgradeModelViaStandalone<T extends ITurtleUpgrade> extends TurtleUpgradeModel<T> {
    StandaloneModel getModel(T upgrade, TurtleSide side, DataComponentPatch data);

    @Override
    default void renderForItem(T upgrade, TurtleSide side, DataComponentPatch data, ItemStackRenderState renderer, ItemModelResolver resolver, ItemTransform transform, int seed) {
        var layer = renderer.newLayer();
        layer.setTransform(transform);
        getModel(upgrade, side, data).setupItemLayer(layer);
    }

    @Override
    default void renderForLevel(T upgrade, ITurtleAccess turtle, TurtleSide side, DataComponentPatch data, PoseStack transform, MultiBufferSource buffers, int light, int overlay) {
        getModel(upgrade, side, data).render(transform, buffers, light, overlay);
    }
}
