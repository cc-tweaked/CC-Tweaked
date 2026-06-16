// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.data;

import dan200.computercraft.api.ComputerCraftBlockIds;
import dan200.computercraft.api.ComputerCraftBlockItemIds;
import dan200.computercraft.api.ComputerCraftItemIds;
import dan200.computercraft.api.ComputerCraftTags;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.integration.ExternalModTags;
import dan200.computercraft.shared.platform.RegistryEntry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.tags.BlockItemTagAppender;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.references.BlockIds;
import net.minecraft.references.BlockItemIds;
import net.minecraft.references.ItemIds;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockItemTagId;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Generators for block and item tags.
 * <p>
 * We cannot trivially extend {@link TagsProvider}, as Forge requires an {@code ExistingFileHelper} as a constructor
 * argument. Instead, we write our tags to the wrapper interface {@link TagConsumer}.
 */
class TagProvider {
    public static void blockTags(TagConsumer<Block> tags) {
        itemAndBlockTags(i -> tags.tag(i.block()));
        tags.tag(ComputerCraftTags.Blocks.WIRED_MODEM).add(ComputerCraftBlockIds.CABLE, ComputerCraftBlockIds.WIRED_MODEM_FULL);

        tags.tag(ComputerCraftTags.Blocks.PERIPHERAL_HUB_IGNORE).addTag(ComputerCraftTags.Blocks.WIRED_MODEM);

        tags.tag(ComputerCraftTags.Blocks.TURTLE_ALWAYS_BREAKABLE).addTag(BlockTags.LEAVES).add(
            BlockItemIds.BAMBOO.block(), BlockIds.BAMBOO_SAPLING // Bamboo isn't instabreak for some odd reason.
        );

        tags.tag(ComputerCraftTags.Blocks.TURTLE_SHOVEL_BREAKABLE).addTag(BlockTags.MINEABLE_WITH_SHOVEL).add(
            BlockItemIds.MELON,
            BlockItemIds.PUMPKIN,
            BlockItemIds.CARVED_PUMPKIN,
            BlockItemIds.JACK_O_LANTERN
        );

        tags.tag(ComputerCraftTags.Blocks.TURTLE_HOE_BREAKABLE).addTag(BlockTags.CROPS).addTag(BlockTags.MINEABLE_WITH_HOE).add(
            BlockItemIds.CACTUS,
            BlockItemIds.MELON,
            BlockItemIds.PUMPKIN,
            BlockItemIds.CARVED_PUMPKIN,
            BlockItemIds.JACK_O_LANTERN
        );

        tags.tag(ComputerCraftTags.Blocks.TURTLE_SWORD_BREAKABLE)
            .addTag(BlockTags.WOOL)
            .addTag(BlockTags.SWORD_INSTANTLY_MINES)
            .add(BlockItemIds.COBWEB);

        tags.tag(ComputerCraftTags.Blocks.TURTLE_CAN_USE);

        // Make all blocks aside from command computer mineable.
        tags.tag(BlockTags.MINEABLE_WITH_PICKAXE).add(
            ComputerCraftBlockIds.COMPUTER_NORMAL,
            ComputerCraftBlockIds.COMPUTER_ADVANCED,
            ComputerCraftBlockIds.TURTLE_NORMAL,
            ComputerCraftBlockIds.TURTLE_ADVANCED,
            ComputerCraftBlockIds.SPEAKER,
            ComputerCraftBlockIds.DISK_DRIVE,
            ComputerCraftBlockIds.PRINTER,
            ComputerCraftBlockIds.MONITOR_NORMAL,
            ComputerCraftBlockIds.MONITOR_ADVANCED,
            ComputerCraftBlockIds.WIRELESS_MODEM_NORMAL,
            ComputerCraftBlockIds.WIRELESS_MODEM_ADVANCED,
            ComputerCraftBlockIds.WIRED_MODEM_FULL,
            ComputerCraftBlockIds.CABLE,
            ComputerCraftBlockIds.REDSTONE_RELAY
        );

        tags.tag(BlockTags.MINEABLE_WITH_AXE).add(ComputerCraftBlockIds.LECTERN);

        tags.tag(BlockTags.WITHER_IMMUNE).add(ComputerCraftBlockIds.COMPUTER_COMMAND);

        tags.tag(ExternalModTags.Blocks.CREATE_BRITTLE).add(
            ComputerCraftBlockIds.CABLE,
            ComputerCraftBlockIds.WIRELESS_MODEM_NORMAL,
            ComputerCraftBlockIds.WIRELESS_MODEM_ADVANCED
        );
    }

    public static void itemTags(TagConsumer<Item> tags) {
        itemAndBlockTags(i -> tags.tag(i.item()));
        tags.tag(ComputerCraftTags.Items.WIRED_MODEM).add(ComputerCraftItemIds.WIRED_MODEM, item(ModRegistry.Items.WIRED_MODEM_FULL));
        tags.tag(ComputerCraftTags.Items.DISKS).add(ComputerCraftItemIds.DISK, item(ModRegistry.Items.TREASURE_DISK));
        tags.tag(ComputerCraftTags.Items.POCKET_COMPUTERS).add(ComputerCraftItemIds.POCKET_COMPUTER_NORMAL, item(ModRegistry.Items.POCKET_COMPUTER_ADVANCED));

        tags.tag(ComputerCraftTags.Items.DYEABLE)
            .addTag(ComputerCraftTags.Items.TURTLE)
            .add(ComputerCraftItemIds.DISK, ComputerCraftItemIds.POCKET_COMPUTER_NORMAL, item(ModRegistry.Items.POCKET_COMPUTER_ADVANCED));

        tags.tag(ItemTags.PIGLIN_LOVED).add(
            ComputerCraftItemIds.COMPUTER_ADVANCED, ComputerCraftItemIds.TURTLE_ADVANCED,
            ComputerCraftItemIds.WIRELESS_MODEM_ADVANCED, ComputerCraftItemIds.POCKET_COMPUTER_ADVANCED,
            ComputerCraftItemIds.MONITOR_ADVANCED
        );

        tags.tag(ItemTags.CAULDRON_CAN_REMOVE_DYE).addTag(ComputerCraftTags.Items.TURTLE);

        // Allow printed books to be placed in bookshelves.
        tags.tag(ItemTags.BOOKSHELF_BOOKS).add(item(ModRegistry.Items.PRINTED_BOOK));

        tags.tag(ComputerCraftTags.Items.TURTLE_CAN_PLACE)
            .add(ItemIds.GLASS_BOTTLE)
            .addTag(ItemTags.BOATS);
    }

    private static void itemAndBlockTags(BlockItemTagConsumer tags) {
        tags.tag(ComputerCraftTags.BlockItems.COMPUTER).add(
            ComputerCraftBlockItemIds.COMPUTER_NORMAL,
            ComputerCraftBlockItemIds.COMPUTER_ADVANCED,
            ComputerCraftBlockItemIds.COMPUTER_COMMAND
        );
        tags.tag(ComputerCraftTags.BlockItems.TURTLE).add(
            ComputerCraftBlockItemIds.TURTLE_NORMAL,
            ComputerCraftBlockItemIds.TURTLE_ADVANCED
        );
        tags.tag(ComputerCraftTags.BlockItems.MONITOR).add(
            ComputerCraftBlockItemIds.MONITOR_NORMAL,
            ComputerCraftBlockItemIds.MONITOR_ADVANCED
        );
    }

    /**
     * A wrapper over {@link TagsProvider}.
     *
     * @param <T> The type of object we're providing tags for.
     */
    public interface TagConsumer<T> {
        BlockItemTagAppender<T> tag(TagKey<T> tag);
    }

    private interface BlockItemTagConsumer {
        BlockItemTagAppender<?> tag(BlockItemTagId tag);
    }

    private static ResourceKey<Item> item(RegistryEntry<? extends Item> entry) {
        return ResourceKey.create(Registries.ITEM, entry.id());
    }
}
