// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.computer.core;

import dan200.computercraft.api.component.ComputerComponent;
import dan200.computercraft.api.lua.IComputerSystem;
import dan200.computercraft.api.lua.ILuaAPIFactory;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.apis.ComputerAccess;
import dan200.computercraft.core.apis.IAPIEnvironment;
import dan200.computercraft.core.computer.ApiLifecycle;
import dan200.computercraft.shared.util.ComponentMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Implementation of {@link IComputerSystem} for usage by externally registered APIs.
 *
 * @see ILuaAPIFactory
 */
final class ComputerSystem extends ComputerAccess implements IComputerSystem, ApiLifecycle {
    private final ServerComputer computer;
    private final IAPIEnvironment environment;
    private final ComponentMap components;

    private boolean active;

    ComputerSystem(ServerComputer computer, IAPIEnvironment environment, ComponentMap components) {
        super(environment);
        this.computer = computer;
        this.environment = environment;
        this.components = components;
    }

    void activate() {
        active = true;
    }

    @Override
    public void shutdown() {
        unmountAll();
    }

    @Override
    public String getAttachmentName() {
        return "computer";
    }

    @Override
    public ServerLevel getLevel() {
        if (!active) {
            throw new IllegalStateException("""
                Cannot access level when constructing the API. Computers are not guaranteed to stay in one place and
                APIs should not rely on the level remaining constant. Instead, call this method when needed.
                """.replace('\n', ' ').strip()
            );
        }
        return computer.getLevel();
    }

    @Override
    public BlockPos getPosition() {
        if (!active) {
            throw new IllegalStateException("""
                Cannot access computer position when constructing the API. Computers are not guaranteed to stay in one
                place and APIs should not rely on the position remaining constant. Instead, call this method when
                needed.
                """.replace('\n', ' ').strip()
            );
        }
        return computer.getPosition();
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

    @Override
    public <T> @Nullable T getComponent(ComputerComponent<T> component) {
        return components.get(component);
    }
}
