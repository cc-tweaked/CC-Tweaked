// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.computer.core;

import dan200.computercraft.api.lua.IComputerSystem;
import dan200.computercraft.api.lua.ILuaAPIFactory;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.apis.ComputerAccess;
import dan200.computercraft.core.apis.IAPIEnvironment;
import dan200.computercraft.core.computer.ApiLifecycle;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Implementation of {@link IComputerAccess}/{@link IComputerSystem} for usage by externally registered APIs.
 *
 * @see ILuaAPIFactory
 */
class ComputerSystem extends ComputerAccess implements IComputerSystem, ApiLifecycle {
    private final IAPIEnvironment environment;

    ComputerSystem(IAPIEnvironment environment) {
        super(environment);
        this.environment = environment;
    }

    @Override
    public void shutdown() {
        unmountAll();
    }

    @Override
    public String getAttachmentName() {
        return "computer";
    }

    @Nullable
    @Override
    public String getLabel() {
        return environment.getLabel();
    }

    @Override
    public Map<String, IPeripheral> getAvailablePeripherals() {
        // TODO: Should this return peripherals on the current computer?
        return Map.of();
    }

    @Nullable
    @Override
    public IPeripheral getAvailablePeripheral(String name) {
        return null;
    }
}
