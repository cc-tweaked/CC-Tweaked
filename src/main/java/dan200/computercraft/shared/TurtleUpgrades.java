/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleUpgradeSerialiser;

import java.util.stream.Stream;

public final class TurtleUpgrades
{
    private static final UpgradeManager<TurtleUpgradeSerialiser<?>, ITurtleUpgrade> registry = new UpgradeManager<>(
        "turtle upgrade", "computercraft/turtle_upgrades", TurtleUpgradeSerialiser.REGISTRY_ID
    );

    private TurtleUpgrades() {}

    public static UpgradeManager<TurtleUpgradeSerialiser<?>, ITurtleUpgrade> instance()
    {
        return registry;
    }

    public static Stream<ITurtleUpgrade> getVanillaUpgrades()
    {
        return instance().getUpgradeWrappers().values().stream()
            .filter( x -> x.modId().equals( ComputerCraft.MOD_ID ) )
            .map( UpgradeManager.UpgradeWrapper::upgrade );
    }
}
