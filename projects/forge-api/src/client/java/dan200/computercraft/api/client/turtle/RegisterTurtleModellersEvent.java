// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.client.turtle;

import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleUpgradeSerialiser;
import dan200.computercraft.api.turtle.TurtleUpgradeType;
import dan200.computercraft.impl.client.ComputerCraftAPIClientService;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * This event is fired to register {@link TurtleUpgradeModeller}s for a mod's {@linkplain TurtleUpgradeType turtle
 * upgrades}.
 * <p>
 * This event is fired during the initial resource load. Registries will be frozen, but mods may not be fully
 * initialised at this point (i.e. {@link FMLCommonSetupEvent} or {@link FMLClientSetupEvent} may not have been
 * dispatched). Subscribers should be careful not to
 */
public class RegisterTurtleModellersEvent extends Event implements IModBusEvent, RegisterTurtleUpgradeModeller {
    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends ITurtleUpgrade> void register(TurtleUpgradeSerialiser<T> serialiser, TurtleUpgradeModeller<T> modeller) {
        ComputerCraftAPIClientService.get().registerTurtleUpgradeModeller(serialiser, modeller);
    }
}
