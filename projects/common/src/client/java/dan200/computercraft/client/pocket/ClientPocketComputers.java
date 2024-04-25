// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.pocket;

import dan200.computercraft.shared.computer.core.ComputerState;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.terminal.TerminalState;
import dan200.computercraft.shared.network.client.PocketComputerDataMessage;
import dan200.computercraft.shared.pocket.items.PocketComputerItem;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Maps {@link ServerComputer#getInstanceUUID()} to locals {@link PocketComputerData}.
 * <p>
 * This is populated by {@link PocketComputerDataMessage} and accessed when rendering pocket computers
 */
public final class ClientPocketComputers {
    private static final Map<UUID, PocketComputerData> instances = new HashMap<>();

    private ClientPocketComputers() {
    }

    public static void reset() {
        instances.clear();
    }

    public static void remove(UUID id) {
        instances.remove(id);
    }

    /**
     * Set the state of a pocket computer.
     *
     * @param instanceId   The instance ID of the pocket computer.
     * @param state        The computer state of the pocket computer.
     * @param lightColour  The current colour of the modem light.
     * @param terminalData The current terminal contents.
     */
    public static void setState(UUID instanceId, ComputerState state, int lightColour, @Nullable TerminalState terminalData) {
        var computer = instances.get(instanceId);
        if (computer == null) {
            instances.put(instanceId, new PocketComputerData(state, lightColour, terminalData));
        } else {
            computer.setState(state, lightColour, terminalData);
        }
    }

    public static @Nullable PocketComputerData get(ItemStack stack) {
        var id = PocketComputerItem.getInstanceID(stack);
        return id == null ? null : instances.get(id);
    }
}
