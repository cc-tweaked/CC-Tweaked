// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.client.turtle;

import dan200.computercraft.api.client.ComputerCraftAPIClient;
import dan200.computercraft.api.client.TransformedModel;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.api.turtle.TurtleUpgradeSerialiser;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

/**
 * Provides models for a {@link ITurtleUpgrade}.
 *
 * @param <T> The type of turtle upgrade this modeller applies to.
 * @see ComputerCraftAPIClient#registerTurtleUpgradeModeller(TurtleUpgradeSerialiser, TurtleUpgradeModeller) To register a modeller.
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
        return (upgrade, turtle, side) -> TransformedModel.of(side == TurtleSide.LEFT ? left : right);
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
        return (upgrade, turtle, side) -> TransformedModel.of(side == TurtleSide.LEFT ? left : right);
    }
}
