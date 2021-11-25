/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.data;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.Registry;
import net.minecraft.block.Blocks;
import net.minecraft.data.BlockTagsProvider;
import net.minecraft.data.DataGenerator;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.common.data.ExistingFileHelper;

import static dan200.computercraft.shared.ComputerCraftTags.Blocks.*;

public class BlockTagsGenerator extends BlockTagsProvider
{
    public BlockTagsGenerator( DataGenerator generator, ExistingFileHelper helper )
    {
        super( generator, ComputerCraft.MOD_ID, helper );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    protected void addTags()
    {
        // Items
        tag( COMPUTER ).add(
            Registry.ModBlocks.COMPUTER_NORMAL.get(),
            Registry.ModBlocks.COMPUTER_ADVANCED.get(),
            Registry.ModBlocks.COMPUTER_COMMAND.get()
        );
        tag( TURTLE ).add( Registry.ModBlocks.TURTLE_NORMAL.get(), Registry.ModBlocks.TURTLE_ADVANCED.get() );
        tag( WIRED_MODEM ).add( Registry.ModBlocks.CABLE.get(), Registry.ModBlocks.WIRED_MODEM_FULL.get() );
        tag( MONITOR ).add( Registry.ModBlocks.MONITOR_NORMAL.get(), Registry.ModBlocks.MONITOR_ADVANCED.get() );

        tag( TURTLE_ALWAYS_BREAKABLE ).addTags( BlockTags.LEAVES ).add(
            Blocks.BAMBOO, Blocks.BAMBOO_SAPLING // Bamboo isn't instabreak for some odd reason.
        );

        tag( TURTLE_SHOVEL_BREAKABLE ).add(
            Blocks.MELON,
            Blocks.PUMPKIN,
            Blocks.CARVED_PUMPKIN,
            Blocks.JACK_O_LANTERN
        );

        tag( TURTLE_HOE_BREAKABLE ).addTags(
            BlockTags.CROPS
        ).add(
            Blocks.CACTUS,
            Blocks.MELON,
            Blocks.PUMPKIN,
            Blocks.CARVED_PUMPKIN,
            Blocks.JACK_O_LANTERN
        );

        tag( TURTLE_SWORD_BREAKABLE ).addTags( BlockTags.WOOL ).add( Blocks.COBWEB );
    }
}
