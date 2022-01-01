/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.turtle;

import dan200.computercraft.api.client.TransformedModel;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.upgrades.IUpgradeBase;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.world.BlockEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The primary interface for defining an update for Turtles. A turtle update can either be a new tool, or a new
 * peripheral.
 *
 * Turtle upgrades are defined in two stages. First, one creates a {@link ITurtleUpgrade} subclass and corresponding
 * {@link TurtleUpgradeSerialiser} instance, which are then registered in a Forge registry.
 *
 * You then write a JSON file in your mod's {@literal data/} folder. This is then parsed when the world is loaded, and
 * the upgrade registered internally. See the documentation in {@link TurtleUpgradeSerialiser} for details on this process
 * and where files should be located.
 *
 * @see TurtleUpgradeSerialiser For how to register a turtle upgrade.
 */
public interface ITurtleUpgrade extends IUpgradeBase
{
    /**
     * Return whether this turtle adds a tool or a peripheral to the turtle.
     *
     * @return The type of upgrade this is.
     * @see TurtleUpgradeType for the differences between them.
     */
    @Nonnull
    TurtleUpgradeType getType();

    /**
     * Will only be called for peripheral upgrades. Creates a peripheral for a turtle being placed using this upgrade.
     *
     * The peripheral created will be stored for the lifetime of the upgrade and will be passed as an argument to
     * {@link #update(ITurtleAccess, TurtleSide)}. It will be attached, detached and have methods called in the same
     * manner as a Computer peripheral.
     *
     * @param turtle Access to the turtle that the peripheral is being created for.
     * @param side   Which side of the turtle (left or right) that the upgrade resides on.
     * @return The newly created peripheral. You may return {@code null} if this upgrade is a Tool
     * and this method is not expected to be called.
     */
    @Nullable
    default IPeripheral createPeripheral( @Nonnull ITurtleAccess turtle, @Nonnull TurtleSide side )
    {
        return null;
    }

    /**
     * Will only be called for Tool turtle. Called when turtle.dig() or turtle.attack() is called
     * by the turtle, and the tool is required to do some work.
     *
     * Conforming implementations should fire {@link BlockEvent.BreakEvent} for digging {@link AttackEntityEvent}
     * for attacking.
     *
     * @param turtle    Access to the turtle that the tool resides on.
     * @param side      Which side of the turtle (left or right) the tool resides on.
     * @param verb      Which action (dig or attack) the turtle is being called on to perform.
     * @param direction Which world direction the action should be performed in, relative to the turtles
     *                  position. This will either be up, down, or the direction the turtle is facing, depending on
     *                  whether dig, digUp or digDown was called.
     * @return Whether the turtle was able to perform the action, and hence whether the {@code turtle.dig()}
     * or {@code turtle.attack()} lua method should return true. If true is returned, the tool will perform
     * a swinging animation. You may return {@code null} if this turtle is a Peripheral  and this method is not expected
     * to be called.
     */
    @Nonnull
    default TurtleCommandResult useTool( @Nonnull ITurtleAccess turtle, @Nonnull TurtleSide side, @Nonnull TurtleVerb verb, @Nonnull Direction direction )
    {
        return TurtleCommandResult.failure();
    }

    /**
     * Called to obtain the model to be used when rendering a turtle peripheral.
     *
     * This can be obtained from {@link net.minecraft.client.renderer.ItemModelShaper#getItemModel(ItemStack)},
     * {@link net.minecraft.client.resources.model.ModelManager#getModel(ModelResourceLocation)} or any other
     * source.
     *
     * @param turtle Access to the turtle that the upgrade resides on. This will be null when getting item models!
     * @param side   Which side of the turtle (left or right) the upgrade resides on.
     * @return The model that you wish to be used to render your upgrade.
     */
    @Nonnull
    @OnlyIn( Dist.CLIENT )
    TransformedModel getModel( @Nullable ITurtleAccess turtle, @Nonnull TurtleSide side );

    /**
     * Called once per tick for each turtle which has the upgrade equipped.
     *
     * @param turtle Access to the turtle that the upgrade resides on.
     * @param side   Which side of the turtle (left or right) the upgrade resides on.
     */
    default void update( @Nonnull ITurtleAccess turtle, @Nonnull TurtleSide side )
    {
    }
}
