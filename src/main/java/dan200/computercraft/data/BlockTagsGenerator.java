/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.data;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.ModRegistry;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.tags.BlockTagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.common.data.ExistingFileHelper;

import static dan200.computercraft.api.ComputerCraftTags.Blocks.*;

class BlockTagsGenerator extends BlockTagsProvider {
    BlockTagsGenerator(DataGenerator generator, ExistingFileHelper helper) {
        super(generator, ComputerCraft.MOD_ID, helper);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void addTags() {
        // Items
        tag(COMPUTER).add(
            ModRegistry.Blocks.COMPUTER_NORMAL.get(),
            ModRegistry.Blocks.COMPUTER_ADVANCED.get(),
            ModRegistry.Blocks.COMPUTER_COMMAND.get()
        );
        tag(TURTLE).add(ModRegistry.Blocks.TURTLE_NORMAL.get(), ModRegistry.Blocks.TURTLE_ADVANCED.get());
        tag(WIRED_MODEM).add(ModRegistry.Blocks.CABLE.get(), ModRegistry.Blocks.WIRED_MODEM_FULL.get());
        tag(MONITOR).add(ModRegistry.Blocks.MONITOR_NORMAL.get(), ModRegistry.Blocks.MONITOR_ADVANCED.get());

        tag(TURTLE_ALWAYS_BREAKABLE).addTags(BlockTags.LEAVES).add(
            net.minecraft.world.level.block.Blocks.BAMBOO, net.minecraft.world.level.block.Blocks.BAMBOO_SAPLING // Bamboo isn't instabreak for some odd reason.
        );

        tag(TURTLE_SHOVEL_BREAKABLE).addTag(BlockTags.MINEABLE_WITH_SHOVEL).add(
            net.minecraft.world.level.block.Blocks.MELON,
            net.minecraft.world.level.block.Blocks.PUMPKIN,
            net.minecraft.world.level.block.Blocks.CARVED_PUMPKIN,
            net.minecraft.world.level.block.Blocks.JACK_O_LANTERN
        );

        tag(TURTLE_HOE_BREAKABLE).addTags(
            BlockTags.CROPS,
            BlockTags.MINEABLE_WITH_HOE
        ).add(
            net.minecraft.world.level.block.Blocks.CACTUS,
            net.minecraft.world.level.block.Blocks.MELON,
            net.minecraft.world.level.block.Blocks.PUMPKIN,
            net.minecraft.world.level.block.Blocks.CARVED_PUMPKIN,
            net.minecraft.world.level.block.Blocks.JACK_O_LANTERN
        );

        tag(TURTLE_SWORD_BREAKABLE).addTags(BlockTags.WOOL).add(net.minecraft.world.level.block.Blocks.COBWEB);

        // Make all blocks aside from command computer mineable.
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(
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
            ModRegistry.Blocks.CABLE.get()
        );
    }
}
