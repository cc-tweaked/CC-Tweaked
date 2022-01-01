/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.data;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.ComputerCraftTags.Blocks;
import dan200.computercraft.shared.Registry;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraftforge.common.data.ExistingFileHelper;

import static dan200.computercraft.api.ComputerCraftTags.Items.*;

class ItemTagsGenerator extends ItemTagsProvider
{
    ItemTagsGenerator( DataGenerator generator, BlockTagsGenerator blockTags, ExistingFileHelper helper )
    {
        super( generator, blockTags, ComputerCraft.MOD_ID, helper );
    }

    @Override
    protected void addTags()
    {
        copy( Blocks.COMPUTER, COMPUTER );
        copy( Blocks.TURTLE, TURTLE );
        tag( WIRED_MODEM ).add( Registry.ModItems.WIRED_MODEM.get(), Registry.ModItems.WIRED_MODEM_FULL.get() );
        copy( Blocks.MONITOR, MONITOR );

        tag( ItemTags.PIGLIN_LOVED ).add(
            Registry.ModItems.COMPUTER_ADVANCED.get(), Registry.ModItems.TURTLE_ADVANCED.get(),
            Registry.ModItems.WIRELESS_MODEM_ADVANCED.get(), Registry.ModItems.POCKET_COMPUTER_ADVANCED.get(),
            Registry.ModItems.MONITOR_ADVANCED.get()
        );
    }
}
