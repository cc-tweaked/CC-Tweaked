// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.data;

import dan200.computercraft.api.ComputerCraftTags;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.integration.ExternalModTags;
import net.minecraft.data.tags.TagAppender;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Generators for block and item tags.
 * <p>
 * We cannot trivially extend {@link TagsProvider}, as Forge requires an {@code ExistingFileHelper} as a constructor
 * argument. Instead, we write our tags to the wrapper interface {@link TagConsumer}.
 */
class TagProvider {
    public static void blockTags(TagConsumer<Block> tags) {
        itemAndBlockTags((b, i) -> tags.tag(b));
        tags.tag(ComputerCraftTags.Blocks.WIRED_MODEM).add(ModRegistry.Blocks.CABLE.get(), ModRegistry.Blocks.WIRED_MODEM_FULL.get());

        tags.tag(ComputerCraftTags.Blocks.PERIPHERAL_HUB_IGNORE).addTag(ComputerCraftTags.Blocks.WIRED_MODEM);

        tags.tag(ComputerCraftTags.Blocks.TURTLE_ALWAYS_BREAKABLE).addTag(BlockTags.LEAVES).add(
            Blocks.BAMBOO, Blocks.BAMBOO_SAPLING // Bamboo isn't instabreak for some odd reason.
        );

        tags.tag(ComputerCraftTags.Blocks.TURTLE_SHOVEL_BREAKABLE).addTag(BlockTags.MINEABLE_WITH_SHOVEL).add(
            Blocks.MELON,
            Blocks.PUMPKIN,
            Blocks.CARVED_PUMPKIN,
            Blocks.JACK_O_LANTERN
        );

        tags.tag(ComputerCraftTags.Blocks.TURTLE_HOE_BREAKABLE).addTag(BlockTags.CROPS).addTag(BlockTags.MINEABLE_WITH_HOE).add(
            Blocks.CACTUS,
            Blocks.MELON,
            Blocks.PUMPKIN,
            Blocks.CARVED_PUMPKIN,
            Blocks.JACK_O_LANTERN
        );

        tags.tag(ComputerCraftTags.Blocks.TURTLE_SWORD_BREAKABLE).addTag(BlockTags.WOOL).add(Blocks.COBWEB);

        tags.tag(ComputerCraftTags.Blocks.TURTLE_CAN_USE);

        // Make all blocks aside from command computer mineable.
        tags.tag(BlockTags.MINEABLE_WITH_PICKAXE).add(
            ModRegistry.Blocks.COMPUTER_NORMAL.get(),
            ModRegistry.Blocks.COMPUTER_ADVANCED.get(),
            ModRegistry.Blocks.TURTLE_NORMAL.get(),
            ModRegistry.Blocks.TURTLE_ADVANCED.get(),
            ModRegistry.Blocks.SPEAKER.get(),
            ModRegistry.Blocks.DISK_DRIVE.get(),
            ModRegistry.Blocks.PRINTER.get(),
            ModRegistry.Blocks.MONITOR_NORMAL.get(),
            ModRegistry.Blocks.MONITOR_ADVANCED.get(),
            ModRegistry.Blocks.WIRELESS_MODEM_NORMAL.get(),
            ModRegistry.Blocks.WIRELESS_MODEM_ADVANCED.get(),
            ModRegistry.Blocks.WIRED_MODEM_FULL.get(),
            ModRegistry.Blocks.CABLE.get(),
            ModRegistry.Blocks.REDSTONE_RELAY.get()
        );

        tags.tag(BlockTags.MINEABLE_WITH_AXE).add(ModRegistry.Blocks.LECTERN.get());

        tags.tag(BlockTags.WITHER_IMMUNE).add(ModRegistry.Blocks.COMPUTER_COMMAND.get());

        tags.tag(ExternalModTags.Blocks.CREATE_BRITTLE).add(
            ModRegistry.Blocks.CABLE.get(),
            ModRegistry.Blocks.WIRELESS_MODEM_NORMAL.get(),
            ModRegistry.Blocks.WIRELESS_MODEM_ADVANCED.get()
        );
    }

    public static void itemTags(TagConsumer<Item> tags) {
        itemAndBlockTags((b, i) -> tags.tag(i).map(Block::asItem));
        tags.tag(ComputerCraftTags.Items.WIRED_MODEM).add(ModRegistry.Items.WIRED_MODEM.get(), ModRegistry.Items.WIRED_MODEM_FULL.get());
        tags.tag(ComputerCraftTags.Items.DISKS).add(ModRegistry.Items.DISK.get(), ModRegistry.Items.TREASURE_DISK.get());
        tags.tag(ComputerCraftTags.Items.POCKET_COMPUTERS).add(ModRegistry.Items.POCKET_COMPUTER_NORMAL.get(), ModRegistry.Items.POCKET_COMPUTER_ADVANCED.get());

        tags.tag(ComputerCraftTags.Items.DYEABLE)
            .addTag(ComputerCraftTags.Items.TURTLE)
            .add(ModRegistry.Items.DISK.get(), ModRegistry.Items.POCKET_COMPUTER_NORMAL.get(), ModRegistry.Items.POCKET_COMPUTER_ADVANCED.get());

        tags.tag(ItemTags.PIGLIN_LOVED).add(
            ModRegistry.Items.COMPUTER_ADVANCED.get(), ModRegistry.Items.TURTLE_ADVANCED.get(),
            ModRegistry.Items.WIRELESS_MODEM_ADVANCED.get(), ModRegistry.Items.POCKET_COMPUTER_ADVANCED.get(),
            ModRegistry.Items.MONITOR_ADVANCED.get()
        );

        // Allow printed books to be placed in bookshelves.
        tags.tag(ItemTags.BOOKSHELF_BOOKS).add(ModRegistry.Items.PRINTED_BOOK.get());

        tags.tag(ComputerCraftTags.Items.TURTLE_CAN_PLACE)
            .add(Items.GLASS_BOTTLE)
            .addTag(ItemTags.BOATS);
    }

    private static void itemAndBlockTags(BlockItemTagConsumer tags) {
        tags.tag(ComputerCraftTags.Blocks.COMPUTER, ComputerCraftTags.Items.COMPUTER).add(
            ModRegistry.Blocks.COMPUTER_NORMAL.get(),
            ModRegistry.Blocks.COMPUTER_ADVANCED.get(),
            ModRegistry.Blocks.COMPUTER_COMMAND.get()
        );
        tags.tag(ComputerCraftTags.Blocks.TURTLE, ComputerCraftTags.Items.TURTLE).add(
            ModRegistry.Blocks.TURTLE_NORMAL.get(),
            ModRegistry.Blocks.TURTLE_ADVANCED.get()
        );
        tags.tag(ComputerCraftTags.Blocks.MONITOR, ComputerCraftTags.Items.MONITOR).add(
            ModRegistry.Blocks.MONITOR_NORMAL.get(),
            ModRegistry.Blocks.MONITOR_ADVANCED.get()
        );
    }

    /**
     * A wrapper over {@link TagsProvider}.
     *
     * @param <T> The type of object we're providing tags for.
     */
    public interface TagConsumer<T> {
        TagAppender<T, T> tag(TagKey<T> tag);
    }

    private interface BlockItemTagConsumer {
        TagAppender<Block, ?> tag(TagKey<Block> blockTag, TagKey<Item> itemTag);
    }
}
