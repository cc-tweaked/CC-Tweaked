// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;

/**
 * The identifiers of blocks provided by ComputerCraft.
 *
 * @see net.minecraft.references.BlockIds
 * @see ComputerCraftBlockItemIds
 */
public final class ComputerCraftBlockIds {
    public static final ResourceKey<Block> COMPUTER_NORMAL = ComputerCraftBlockItemIds.COMPUTER_NORMAL.block();
    public static final ResourceKey<Block> COMPUTER_ADVANCED = ComputerCraftBlockItemIds.COMPUTER_ADVANCED.block();
    public static final ResourceKey<Block> COMPUTER_COMMAND = ComputerCraftBlockItemIds.COMPUTER_COMMAND.block();

    public static final ResourceKey<Block> TURTLE_NORMAL = ComputerCraftBlockItemIds.TURTLE_NORMAL.block();
    public static final ResourceKey<Block> TURTLE_ADVANCED = ComputerCraftBlockItemIds.TURTLE_ADVANCED.block();

    public static final ResourceKey<Block> CABLE = create("cable");
    public static final ResourceKey<Block> DISK_DRIVE = ComputerCraftBlockItemIds.DISK_DRIVE.block();
    public static final ResourceKey<Block> MONITOR_ADVANCED = ComputerCraftBlockItemIds.MONITOR_ADVANCED.block();
    public static final ResourceKey<Block> MONITOR_NORMAL = ComputerCraftBlockItemIds.MONITOR_NORMAL.block();
    public static final ResourceKey<Block> PRINTER = ComputerCraftBlockItemIds.PRINTER.block();
    public static final ResourceKey<Block> REDSTONE_RELAY = ComputerCraftBlockItemIds.REDSTONE_RELAY.block();
    public static final ResourceKey<Block> SPEAKER = ComputerCraftBlockItemIds.SPEAKER.block();
    public static final ResourceKey<Block> WIRED_MODEM_FULL = ComputerCraftBlockItemIds.WIRED_MODEM_FULL.block();
    public static final ResourceKey<Block> WIRELESS_MODEM_ADVANCED = ComputerCraftBlockItemIds.WIRELESS_MODEM_ADVANCED.block();
    public static final ResourceKey<Block> WIRELESS_MODEM_NORMAL = ComputerCraftBlockItemIds.WIRELESS_MODEM_NORMAL.block();

    public static final ResourceKey<Block> LECTERN = create("lectern");

    private static ResourceKey<Block> create(String id) {
        return ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, id));
    }

    private ComputerCraftBlockIds() {
    }
}
