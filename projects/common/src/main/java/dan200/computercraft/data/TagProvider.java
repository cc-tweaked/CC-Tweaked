// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.data;

import dan200.computercraft.api.ComputerCraftTags;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.integration.ExternalModTags;
import dan200.computercraft.shared.platform.RegistryWrappers;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagBuilder;
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
        tags.tag(ComputerCraftTags.Blocks.COMPUTER).add(
            ModRegistry.Blocks.COMPUTER_NORMAL.get(),
            ModRegistry.Blocks.COMPUTER_ADVANCED.get(),
            ModRegistry.Blocks.COMPUTER_COMMAND.get()
        );
        tags.tag(ComputerCraftTags.Blocks.TURTLE).add(ModRegistry.Blocks.TURTLE_NORMAL.get(), ModRegistry.Blocks.TURTLE_ADVANCED.get());
        tags.tag(ComputerCraftTags.Blocks.WIRED_MODEM).add(ModRegistry.Blocks.CABLE.get(), ModRegistry.Blocks.WIRED_MODEM_FULL.get());
        tags.tag(ComputerCraftTags.Blocks.MONITOR).add(ModRegistry.Blocks.MONITOR_NORMAL.get(), ModRegistry.Blocks.MONITOR_ADVANCED.get());

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

        tags.tag(ComputerCraftTags.Blocks.TURTLE_CAN_USE)
            .addTag(BlockTags.BEEHIVES)
            .addTag(BlockTags.CAULDRONS)
            .add(Blocks.COMPOSTER);

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

    public static void itemTags(ItemTagConsumer tags) {
        tags.copy(ComputerCraftTags.Blocks.COMPUTER, ComputerCraftTags.Items.COMPUTER);
        tags.copy(ComputerCraftTags.Blocks.TURTLE, ComputerCraftTags.Items.TURTLE);
        tags.tag(ComputerCraftTags.Items.WIRED_MODEM).add(ModRegistry.Items.WIRED_MODEM.get(), ModRegistry.Items.WIRED_MODEM_FULL.get());
        tags.copy(ComputerCraftTags.Blocks.MONITOR, ComputerCraftTags.Items.MONITOR);

        tags.tag(ItemTags.PIGLIN_LOVED).add(
            ModRegistry.Items.COMPUTER_ADVANCED.get(), ModRegistry.Items.TURTLE_ADVANCED.get(),
            ModRegistry.Items.WIRELESS_MODEM_ADVANCED.get(), ModRegistry.Items.POCKET_COMPUTER_ADVANCED.get(),
            ModRegistry.Items.MONITOR_ADVANCED.get()
        );

        // Allow printed books to be placed in bookshelves.
        tags.tag(ItemTags.BOOKSHELF_BOOKS).add(ModRegistry.Items.PRINTED_BOOK.get());

        // Allow any printout to be placed on lecterns. See also PrintoutItem and CustomLecternBlock.
        tags.tag(ItemTags.LECTERN_BOOKS).add(
            ModRegistry.Items.PRINTED_PAGE.get(), ModRegistry.Items.PRINTED_PAGES.get(), ModRegistry.Items.PRINTED_BOOK.get()
        );

        tags.tag(ComputerCraftTags.Items.TURTLE_CAN_PLACE)
            .add(Items.GLASS_BOTTLE)
            .addTag(ItemTags.BOATS);
    }

    /**
     * A wrapper over {@link TagsProvider}.
     *
     * @param <T> The type of object we're providing tags for.
     */
    public interface TagConsumer<T> {
        TagAppender<T> tag(TagKey<T> tag);
    }

    public record TagAppender<T>(RegistryWrappers.RegistryWrapper<T> registry, TagBuilder builder) {
        public TagAppender<T> add(T object) {
            builder.addElement(registry.getKey(object));
            return this;
        }

        @SafeVarargs
        public final TagAppender<T> add(T... objects) {
            for (var object : objects) add(object);
            return this;
        }

        public TagAppender<T> addTag(TagKey<T> tag) {
            builder.addTag(tag.location());
            return this;
        }
    }

    /**
     * A wrapper over {@link ItemTagsProvider}.
     */
    interface ItemTagConsumer extends TagConsumer<Item> {
        void copy(TagKey<Block> block, TagKey<Item> item);
    }
}
