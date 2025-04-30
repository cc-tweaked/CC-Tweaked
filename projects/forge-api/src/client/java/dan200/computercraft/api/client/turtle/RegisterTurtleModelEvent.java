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
 * This event is fired to register {@link TurtleUpgradeModel}s for a mod's {@linkplain TurtleUpgradeType turtle
 * upgrades}.
 * <p>
 * This event is fired during the initial mod construction. Registries will be frozen, but mods may not be fully
 * initialised at this point (i.e. {@link FMLCommonSetupEvent} or {@link FMLClientSetupEvent} may not have been
 * dispatched).
 */
public class RegisterTurtleModelEvent extends Event implements IModBusEvent, RegisterTurtleUpgradeModel {
    private final RegisterTurtleUpgradeModel dispatch;

    @ApiStatus.Internal
    public RegisterTurtleModelEvent(RegisterTurtleUpgradeModel dispatch) {
        this.dispatch = dispatch;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends ITurtleUpgrade> void register(UpgradeType<T> type, TurtleUpgradeModel.Unbaked<? super T> modeller) {
        dispatch.register(type, modeller);
    }
}
