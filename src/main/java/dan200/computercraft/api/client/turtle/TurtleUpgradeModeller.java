/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.client.turtle;

import dan200.computercraft.api.client.ComputerCraftAPIClient;
import dan200.computercraft.api.client.TransformedModel;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.api.turtle.TurtleUpgradeSerialiser;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Provides models for a {@link ITurtleUpgrade}.
 *
 * @param <T> The type of turtle upgrade this modeller applies to.
 * @see ComputerCraftAPIClient#registerTurtleUpgradeModeller(TurtleUpgradeSerialiser, TurtleUpgradeModeller) To register a modeller.
 */
public interface TurtleUpgradeModeller<T extends ITurtleUpgrade>
{
    /**
     * Obtain the model to be used when rendering a turtle peripheral.
     * <p>
     * When the current turtle is {@literal null}, this function should be constant for a given upgrade and side.
     *
     * @param upgrade The upgrade that you're getting the model for.
     * @param turtle  Access to the turtle that the upgrade resides on. This will be null when getting item models!
     * @param side    Which side of the turtle (left or right) the upgrade resides on.
     * @return The model that you wish to be used to render your upgrade.
     */
    @Nonnull
    TransformedModel getModel( @Nonnull T upgrade, @Nullable ITurtleAccess turtle, @Nonnull TurtleSide side );

    /**
     * A basic {@link TurtleUpgradeModeller} which renders using the upgrade's {@linkplain ITurtleUpgrade#getCraftingItem()
     * crafting item}.
     * <p>
     * This uses appropriate transformations for "flat" items, namely those extending the {@literal minecraft:item/generated}
     * model type. It will not appear correct for 3D models with additional depth, such as blocks.
     *
     * @param <T> The type of the turtle upgrade.
     * @return The constructed modeller.
     */
    @SuppressWarnings( "unchecked" )
    static <T extends ITurtleUpgrade> TurtleUpgradeModeller<T> flatItem()
    {
        return (TurtleUpgradeModeller<T>) TurtleUpgradeModellers.FLAT_ITEM;
    }

    /**
     * Construct a {@link TurtleUpgradeModeller} which has a single model for the left and right side.
     *
     * @param left  The model to use on the left.
     * @param right The model to use on the right.
     * @param <T>   The type of the turtle upgrade.
     * @return The constructed modeller.
     */
    static <T extends ITurtleUpgrade> TurtleUpgradeModeller<T> sided( ModelResourceLocation left, ModelResourceLocation right )
    {
        return ( upgrade, turtle, side ) -> TransformedModel.of( side == TurtleSide.LEFT ? left : right );
    }

    /**
     * Construct a {@link TurtleUpgradeModeller} which has a single model for the left and right side.
     *
     * @param left  The model to use on the left.
     * @param right The model to use on the right.
     * @param <T>   The type of the turtle upgrade.
     * @return The constructed modeller.
     */
    static <T extends ITurtleUpgrade> TurtleUpgradeModeller<T> sided( ResourceLocation left, ResourceLocation right )
    {
        return ( upgrade, turtle, side ) -> TransformedModel.of( side == TurtleSide.LEFT ? left : right );
    }
}
