// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api;

import net.minecraft.references.BlockItemId;
import net.minecraft.resources.Identifier;

/**
 * The identifiers of blocks and items provided by ComputerCraft.
 *
 * @see net.minecraft.references.BlockItemIds
 * @see ComputerCraftBlockIds
 * @see ComputerCraftItemIds
 */
public final class ComputerCraftBlockItemIds {
    public static final BlockItemId COMPUTER_NORMAL = create("computer_normal");
    public static final BlockItemId COMPUTER_ADVANCED = create("computer_advanced");
    public static final BlockItemId COMPUTER_COMMAND = create("computer_command");

    public static final BlockItemId TURTLE_NORMAL = create("turtle_normal");
    public static final BlockItemId TURTLE_ADVANCED = create("turtle_advanced");

    public static final BlockItemId DISK_DRIVE = create("disk_drive");
    public static final BlockItemId MONITOR_ADVANCED = create("monitor_advanced");
    public static final BlockItemId MONITOR_NORMAL = create("monitor_normal");
    public static final BlockItemId PRINTER = create("printer");
    public static final BlockItemId REDSTONE_RELAY = create("redstone_relay");
    public static final BlockItemId SPEAKER = create("speaker");
    public static final BlockItemId WIRED_MODEM_FULL = create("wired_modem_full");
    public static final BlockItemId WIRELESS_MODEM_ADVANCED = create("wireless_modem_advanced");
    public static final BlockItemId WIRELESS_MODEM_NORMAL = create("wireless_modem_normal");

    private static BlockItemId create(String name) {
        var id = Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, name);
        return BlockItemId.create(id, id);
    }

    private ComputerCraftBlockItemIds() {
    }
}
