// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.client.turtle;

import dan200.computercraft.api.client.TransformedModel;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

/**
 * Provides models for a {@link ITurtleUpgrade}.
 * <p>
 * Use {@code dan200.computercraft.api.client.FabricComputerCraftAPIClient#registerTurtleUpgradeModeller} to register a
 * modeller on Fabric and {@code dan200.computercraft.api.client.turtle.RegisterTurtleModellersEvent} to register one
 * on Forge
 *
 * @param <T> The type of turtle upgrade this modeller applies to.
 * @see RegisterTurtleUpgradeModeller For multi-loader registration support.
 */
public interface TurtleUpgradeModeller<T extends ITurtleUpgrade> {
    /**
     * Obtain the model to be used when rendering a turtle peripheral.
     * <p>
     * When the current turtle is {@literal null}, this function should be constant for a given upgrade and side.
     *
     * @param upgrade The upgrade that you're getting the model for.
     * @param turtle  Access to the turtle that the upgrade resides on. This will be null when getting item models, unless
     *                {@link #getModel(ITurtleUpgrade, CompoundTag, TurtleSide)} is overriden.
     * @param side    Which side of the turtle (left or right) the upgrade resides on.
     * @return The model that you wish to be used to render your upgrade.
     */
    TransformedModel getModel(T upgrade, @Nullable ITurtleAccess turtle, TurtleSide side);

    /**
     * Obtain the model to be used when rendering a turtle peripheral.
     * <p>
     * This is used when rendering the turtle's item model, and so no {@link ITurtleAccess} is available.
     *
     * @param upgrade The upgrade that you're getting the model for.
     * @param data    Upgrade data instance for current turtle side.
     * @param side    Which side of the turtle (left or right) the upgrade resides on.
     * @return The model that you wish to be used to render your upgrade.
     */
    default TransformedModel getModel(T upgrade, CompoundTag data, TurtleSide side) {
        return getModel(upgrade, (ITurtleAccess) null, side);
    }


    /**
     * Get a list of models that this turtle modeller depends on.
     * <p>
     * Models included in this list will be loaded and baked alongside item and block models, and so may be referenced
     * by {@link TransformedModel#of(ResourceLocation)}. You do not need to override this method if you will load models
     * by other means.
     *
     * @return A list of models that this modeller depends on.
     * @see UnbakedModel#getDependencies()
     */
    default Collection<ResourceLocation> getDependencies() {
        return List.of();
    }

    /**
     * A basic {@link TurtleUpgradeModeller} which renders using the upgrade's {@linkplain ITurtleUpgrade#getUpgradeItem(CompoundTag)}
     * upgrade item}.
     * <p>
     * This uses appropriate transformations for "flat" items, namely those extending the {@literal minecraft:item/generated}
     * model type. It will not appear correct for 3D models with additional depth, such as blocks.
     *
     * @param <T> The type of the turtle upgrade.
     * @return The constructed modeller.
     */
    @SuppressWarnings("unchecked")
    static <T extends ITurtleUpgrade> TurtleUpgradeModeller<T> flatItem() {
        return (TurtleUpgradeModeller<T>) TurtleUpgradeModellers.UPGRADE_ITEM;
    }

    /**
     * Construct a {@link TurtleUpgradeModeller} which has a single model for the left and right side.
     *
     * @param left  The model to use on the left.
     * @param right The model to use on the right.
     * @param <T>   The type of the turtle upgrade.
     * @return The constructed modeller.
     */
    static <T extends ITurtleUpgrade> TurtleUpgradeModeller<T> sided(ModelResourceLocation left, ModelResourceLocation right) {
        // TODO(1.21.0): Remove this.
        return sided((ResourceLocation) left, right);
    }

    /**
     * Construct a {@link TurtleUpgradeModeller} which has a single model for the left and right side.
     *
     * @param left  The model to use on the left.
     * @param right The model to use on the right.
     * @param <T>   The type of the turtle upgrade.
     * @return The constructed modeller.
     */
    static <T extends ITurtleUpgrade> TurtleUpgradeModeller<T> sided(ResourceLocation left, ResourceLocation right) {
        return new TurtleUpgradeModeller<>() {
            @Override
            public TransformedModel getModel(T upgrade, @Nullable ITurtleAccess turtle, TurtleSide side) {
                return TransformedModel.of(side == TurtleSide.LEFT ? left : right);
            }

            @Override
            public Collection<ResourceLocation> getDependencies() {
                return List.of(left, right);
            }
        };
    }
}
