// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.client.turtle;

import com.mojang.math.Transformation;
import dan200.computercraft.api.client.TransformedModel;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.impl.client.ClientPlatformHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

import javax.annotation.Nullable;

final class TurtleUpgradeModellers {
    private static final Transformation leftTransform = getMatrixFor(-0.4065f);
    private static final Transformation rightTransform = getMatrixFor(0.4065f);

    private static Transformation getMatrixFor(float offset) {
        var matrix = new Matrix4f();
        matrix.set(new float[]{
            0.0f, 0.0f, -1.0f, 1.0f + offset,
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, -1.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 0.0f, 1.0f,
        });
        matrix.transpose();
        return new Transformation(matrix);
    }

    static final TurtleUpgradeModeller<ITurtleUpgrade> UPGRADE_ITEM = new UpgradeItemModeller();

    private static final class UpgradeItemModeller implements TurtleUpgradeModeller<ITurtleUpgrade> {
        @Override
        public TransformedModel getModel(ITurtleUpgrade upgrade, @Nullable ITurtleAccess turtle, TurtleSide side) {
            return getModel(turtle == null ? upgrade.getCraftingItem() : upgrade.getUpgradeItem(turtle.getUpgradeNBTData(side)), side);
        }

        @Override
        public TransformedModel getModel(ITurtleUpgrade upgrade, CompoundTag data, TurtleSide side) {
            return getModel(upgrade.getUpgradeItem(data), side);
        }

        private TransformedModel getModel(ItemStack stack, TurtleSide side) {
            var model = Minecraft.getInstance().getItemRenderer().getItemModelShaper().getItemModel(stack);
            if (stack.hasFoil()) model = ClientPlatformHelper.get().createdFoiledModel(model);
            return new TransformedModel(model, side == TurtleSide.LEFT ? leftTransform : rightTransform);
        }
    }
}
