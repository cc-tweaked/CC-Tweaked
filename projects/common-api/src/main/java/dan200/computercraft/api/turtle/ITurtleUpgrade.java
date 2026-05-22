// Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.api.turtle;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.upgrades.UpgradeBase;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.function.BiFunction;

/**
 * The primary interface for defining an update for Turtles. A turtle update can either be a new tool, or a new
 * peripheral.
 * <p>
 * Turtle upgrades are defined in two stages. First, one creates a {@link ITurtleUpgrade} subclass and corresponding
 * {@link TurtleUpgradeSerialiser} instance, which are then registered in a Minecraft registry.
 * <p>
 * You then write a JSON file in your mod's {@literal data/} folder. This is then parsed when the world is loaded, and
 * the upgrade automatically registered.
 *
 * <h2>Example</h2>
 * <h3>Registering the upgrade serialiser</h3>
 * First, let's create a new class that implements {@link ITurtleUpgrade}. It is recommended to subclass
 * {@link AbstractTurtleUpgrade}, as that provides a default implementation of most methods.
 *
 * {@snippet class=com.example.examplemod.ExampleTurtleUpgrade region=body}
 * <p>
 * Now we must construct a new upgrade serialiser. In most cases, you can use one of the helper methods
 * (e.g. {@link TurtleUpgradeSerialiser#simpleWithCustomItem(BiFunction)}), rather than defining your own implementation.
 *
 * {@snippet class=com.example.examplemod.ExampleMod region=turtle_upgrades}
 *
 * We now must register this upgrade serialiser. This is done the same way as you'd register blocks, items, or other
 * Minecraft objects. The approach to do this will depend on mod-loader.
 *
 * <h4>Fabric</h4>
 * {@snippet class=com.example.examplemod.FabricExampleMod region=turtle_upgrades}
 *
 * <h4>Forge</h4>
 * {@snippet class=com.example.examplemod.ForgeExampleMod region=turtle_upgrades}
 *
 * <h3>Rendering the upgrade</h3>
 * Next, we need to register a model for our upgrade. This is done by registering a
 * {@link dan200.computercraft.api.client.turtle.TurtleUpgradeModeller} for your upgrade serialiser.
 *
 * <h4>Fabric</h4>
 * {@snippet class=com.example.examplemod.FabricExampleModClient region=turtle_modellers}
 *
 *
 * <h4>Forge</h4>
 * {@snippet class=com.example.examplemod.FabricExampleModClient region=turtle_modellers}
 *
 * <h3>Registering the upgrade itself</h3>
 * Upgrades themselves are loaded from datapacks when a level is loaded. In order to register our new upgrade, we must
 * create a new JSON file at {@code data/<my_mod>/computercraft/turtle_upgrades/<my_upgrade_id>.json}.
 *
 * {@snippet file = data/examplemod/computercraft/turtle_upgrades/example_turtle_upgrade.json}
 *
 * The {@code "type"} field points to the ID of the upgrade serialiser we've just registered, while the other fields
 * are read by the serialiser itself. As our upgrade was defined with {@link TurtleUpgradeSerialiser#simpleWithCustomItem(BiFunction)}, the
 * {@code "item"} field will construct our upgrade with {@link Items#COMPASS}.
 * <p>
 * Rather than manually creating the file, it is recommended to data-generators to generate this file. This can be done
 * with {@link TurtleUpgradeDataProvider}.
 *
 * {@snippet class=com.example.examplemod.data.TurtleDataProvider region=body}
 *
 * @see TurtleUpgradeSerialiser Registering a turtle upgrade.
 */
public interface ITurtleUpgrade extends UpgradeBase {
    /**
     * Return whether this turtle adds a tool or a peripheral to the turtle.
     *
     * @return The type of upgrade this is.
     * @see TurtleUpgradeType for the differences between them.
     */
    TurtleUpgradeType getType();

    /**
     * Will only be called for peripheral upgrades. Creates a peripheral for a turtle being placed using this upgrade.
     * <p>
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
    default IPeripheral createPeripheral(ITurtleAccess turtle, TurtleSide side) {
        return null;
    }

    /**
     * Will only be called for Tool turtle. Called when turtle.dig() or turtle.attack() is called
     * by the turtle, and the tool is required to do some work.
     * <p>
     * Conforming implementations should fire loader-specific events when using the tool, for instance Forge's
     * {@code AttackEntityEvent}.
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
    default TurtleCommandResult useTool(ITurtleAccess turtle, TurtleSide side, TurtleVerb verb, Direction direction) {
        return TurtleCommandResult.failure();
    }

    /**
     * Called once per tick for each turtle which has the upgrade equipped.
     *
     * @param turtle Access to the turtle that the upgrade resides on.
     * @param side   Which side of the turtle (left or right) the upgrade resides on.
     */
    default void update(ITurtleAccess turtle, TurtleSide side) {
    }

    /**
     * Get upgrade data that should be persisted when the turtle was broken.
     * <p>
     * This method should be overridden when you don't need to store all upgrade data by default. For instance, if you
     * store peripheral state in the upgrade data, which should be lost when the turtle is broken.
     *
     * @param upgradeData Data that currently stored for this upgrade
     * @return Filtered version of this data.
     */
    default CompoundTag getPersistedData(CompoundTag upgradeData) {
        return upgradeData;
    }
}
