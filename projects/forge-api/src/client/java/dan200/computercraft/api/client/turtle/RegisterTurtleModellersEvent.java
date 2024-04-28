// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.client.turtle;

import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleUpgradeType;
import dan200.computercraft.api.upgrades.UpgradeType;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.jetbrains.annotations.ApiStatus;

/**
 * This event is fired to register {@link TurtleUpgradeModeller}s for a mod's {@linkplain TurtleUpgradeType turtle
 * upgrades}.
 * <p>
 * This event is fired during the initial resource load. Registries will be frozen, but mods may not be fully
 * initialised at this point (i.e. {@link FMLCommonSetupEvent} or {@link FMLClientSetupEvent} may not have been
 * dispatched). Subscribers should be careful not to
 */
public class RegisterTurtleModellersEvent extends Event implements IModBusEvent, RegisterTurtleUpgradeModeller {
    private final RegisterTurtleUpgradeModeller dispatch;

    @ApiStatus.Internal
    public RegisterTurtleModellersEvent(RegisterTurtleUpgradeModeller dispatch) {
        this.dispatch = dispatch;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends ITurtleUpgrade> void register(UpgradeType<T> type, TurtleUpgradeModeller<T> modeller) {
        dispatch.register(type, modeller);
    }
}
