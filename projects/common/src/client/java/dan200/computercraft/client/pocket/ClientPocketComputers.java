// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.pocket;

import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.network.client.PocketComputerDataMessage;
import dan200.computercraft.shared.pocket.items.PocketComputerItem;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.world.item.ItemStack;

/**
 * Maps {@link ServerComputer#getInstanceID()} to locals {@link PocketComputerData}.
 * <p>
 * This is populated by {@link PocketComputerDataMessage} and accessed when rendering pocket computers
 */
public final class ClientPocketComputers {
    private static final Int2ObjectMap<PocketComputerData> instances = new Int2ObjectOpenHashMap<>();

    private ClientPocketComputers() {
    }

    public static void reset() {
        instances.clear();
    }

    public static void remove(int id) {
        instances.remove(id);
    }

    /**
     * Get or create a pocket computer.
     *
     * @param instanceId The instance ID of the pocket computer.
     * @param advanced   Whether this computer has an advanced terminal.
     * @return The pocket computer data.
     */
    public static PocketComputerData get(int instanceId, boolean advanced) {
        var computer = instances.get(instanceId);
        if (computer == null) instances.put(instanceId, computer = new PocketComputerData(advanced));
        return computer;
    }

    public static PocketComputerData get(ItemStack stack) {
        var family = stack.getItem() instanceof PocketComputerItem computer ? computer.getFamily() : ComputerFamily.NORMAL;
        return get(PocketComputerItem.getInstanceID(stack), family != ComputerFamily.NORMAL);
    }
}
