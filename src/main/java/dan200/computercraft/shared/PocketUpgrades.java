/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.api.pocket.PocketUpgradeSerialiser;

import java.util.stream.Stream;

public final class PocketUpgrades
{
    private static final UpgradeManager<PocketUpgradeSerialiser<?>, IPocketUpgrade> registry = new UpgradeManager<>(
        "pocket computer upgrade", "computercraft/pocket_upgrades", PocketUpgradeSerialiser.REGISTRY_ID
    );

    private PocketUpgrades() {}

    public static UpgradeManager<PocketUpgradeSerialiser<?>, IPocketUpgrade> instance()
    {
        return registry;
    }

    public static Stream<IPocketUpgrade> getVanillaUpgrades()
    {
        return instance().getUpgradeWrappers().values().stream()
            .filter( x -> x.modId().equals( ComputerCraft.MOD_ID ) )
            .map( UpgradeManager.UpgradeWrapper::upgrade );
    }
}
