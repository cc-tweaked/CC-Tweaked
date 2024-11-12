// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0
package dan200.computercraft.shared.peripheral.redstone;

import dan200.computercraft.api.peripheral.AttachedComputerSet;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.apis.RedstoneAPI;
import dan200.computercraft.core.apis.RedstoneMethods;
import dan200.computercraft.core.redstone.RedstoneAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The redstone relay is a peripheral that allows reading and outputting redstone signals.
 * <p>
 * The peripheral provides largely identical methods to a computer's built-in {@link RedstoneAPI} API, allowing setting
 * signals on all six sides of the block ("top", "bottom", "left", "right", "front" and "back").
 *
 * <p>
 * ## Recipe
 * <div class="recipe-container">
 *   <mc-recipe recipe="computercraft:redstone_relay"></mc-recipe>
 * </div>
 *
 * @cc.usage Toggle the redstone signal above the computer every 0.5 seconds.
 *
 * <pre>{@code
 * local relay = peripheral.find("redstone_relay")
 * while true do
 *   relay.setOutput("top", not relay.getOutput("top"))
 *   sleep(0.5)
 * end
 * }</pre>
 * @cc.module redstone_relay
 * @cc.since 1.114.0
 */
public final class RedstoneRelayPeripheral extends RedstoneMethods implements IPeripheral {
    private final AttachedComputerSet computers = new AttachedComputerSet();

    RedstoneRelayPeripheral(RedstoneAccess access) {
        super(access);
    }

    @Nonnull
    @Override
    public String getType() {
        return "redstone_relay";
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return this == other;
    }

    @Override
    public void attach(IComputerAccess computer) {
        computers.add(computer);
    }

    @Override
    public void detach(IComputerAccess computer) {
        computers.remove(computer);
    }

    void queueRedstoneEvent() {
        computers.queueEvent("redstone");
    }
}
