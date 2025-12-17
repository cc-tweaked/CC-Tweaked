// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.client.turtle;

import com.mojang.serialization.MapCodec;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.jetbrains.annotations.ApiStatus;

/**
 * This event is fired to register additional {@link TurtleUpgradeModel}s.
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
     * {@inheritDoc}The codec used to read/decode an upgrade model.
     */
    @Override
    public void register(Identifier id, MapCodec<? extends TurtleUpgradeModel.Unbaked> model) {
        dispatch.register(id, model);
    }
}
