// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

/**
 * The identifiers of items provided by ComputerCraft.
 *
 * @see net.minecraft.references.ItemIds
 * @see ComputerCraftBlockItemIds
 */
public final class ComputerCraftItemIds {
    public static final ResourceKey<Item> COMPUTER_NORMAL = ComputerCraftBlockItemIds.COMPUTER_NORMAL.item();
    public static final ResourceKey<Item> COMPUTER_ADVANCED = ComputerCraftBlockItemIds.COMPUTER_ADVANCED.item();
    public static final ResourceKey<Item> COMPUTER_COMMAND = ComputerCraftBlockItemIds.COMPUTER_COMMAND.item();

    public static final ResourceKey<Item> TURTLE_NORMAL = ComputerCraftBlockItemIds.TURTLE_NORMAL.item();
    public static final ResourceKey<Item> TURTLE_ADVANCED = ComputerCraftBlockItemIds.TURTLE_ADVANCED.item();

    public static final ResourceKey<Item> POCKET_COMPUTER_NORMAL = create("pocket_computer_normal");
    public static final ResourceKey<Item> POCKET_COMPUTER_ADVANCED = create("pocket_computer_advanced");

    public static final ResourceKey<Item> DISK = create("disk");
    public static final ResourceKey<Item> TREASURE_DISK = create("treasure_disk");

    public static final ResourceKey<Item> PRINTED_PAGE = create("printed_page");
    public static final ResourceKey<Item> PRINTED_PAGES = create("printed_pages");
    public static final ResourceKey<Item> PRINTED_BOOK = create("printed_book");

    public static final ResourceKey<Item> CABLE = create("cable");
    public static final ResourceKey<Item> DISK_DRIVE = ComputerCraftBlockItemIds.DISK_DRIVE.item();
    public static final ResourceKey<Item> MONITOR_ADVANCED = ComputerCraftBlockItemIds.MONITOR_ADVANCED.item();
    public static final ResourceKey<Item> MONITOR_NORMAL = ComputerCraftBlockItemIds.MONITOR_NORMAL.item();
    public static final ResourceKey<Item> PRINTER = ComputerCraftBlockItemIds.PRINTER.item();
    public static final ResourceKey<Item> REDSTONE_RELAY = ComputerCraftBlockItemIds.REDSTONE_RELAY.item();
    public static final ResourceKey<Item> SPEAKER = ComputerCraftBlockItemIds.SPEAKER.item();
    public static final ResourceKey<Item> WIRED_MODEM = create("wired_modem");
    public static final ResourceKey<Item> WIRED_MODEM_FULL = ComputerCraftBlockItemIds.WIRED_MODEM_FULL.item();
    public static final ResourceKey<Item> WIRELESS_MODEM_ADVANCED = ComputerCraftBlockItemIds.WIRELESS_MODEM_ADVANCED.item();
    public static final ResourceKey<Item> WIRELESS_MODEM_NORMAL = ComputerCraftBlockItemIds.WIRELESS_MODEM_NORMAL.item();

    private static ResourceKey<Item> create(String id) {
        return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, id));
    }

    private ComputerCraftItemIds() {
    }
}
