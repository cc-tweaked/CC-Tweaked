/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.data;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.Registry;
import net.minecraft.data.BlockTagsProvider;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.ItemTagsProvider;
import net.minecraft.item.Item;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ResourceLocation;

import static dan200.computercraft.data.Tags.CCTags.*;

public class Tags extends ItemTagsProvider
{
    public static class CCTags
    {
        public static final ITag.INamedTag<Item> COMPUTER = item( "computer" );
        public static final ITag.INamedTag<Item> TURTLE = item( "turtle" );
        public static final ITag.INamedTag<Item> WIRED_MODEM = item( "wired_modem" );
        public static final ITag.INamedTag<Item> MONITOR = item( "monitor" );
    }

    public Tags( DataGenerator generator, BlockTagsProvider tags )
    {
        super( generator, tags );
    }

    @Override
    protected void registerTags()
    {
        func_240522_a_( COMPUTER ).func_240534_a_(
            Registry.ModItems.COMPUTER_NORMAL.get(),
            Registry.ModItems.COMPUTER_ADVANCED.get(),
            Registry.ModItems.COMPUTER_COMMAND.get()
        );
        func_240522_a_( TURTLE ).func_240534_a_( Registry.ModItems.TURTLE_NORMAL.get(), Registry.ModItems.TURTLE_ADVANCED.get() );
        func_240522_a_( WIRED_MODEM ).func_240534_a_( Registry.ModItems.WIRED_MODEM.get(), Registry.ModItems.WIRED_MODEM_FULL.get() );
        func_240522_a_( MONITOR ).func_240534_a_( Registry.ModItems.MONITOR_NORMAL.get(), Registry.ModItems.MONITOR_ADVANCED.get() );
    }

    private static ITag.INamedTag<Item> item( String name )
    {
        return ItemTags.makeWrapperTag( new ResourceLocation( ComputerCraft.MOD_ID, name ).toString() );
    }
}
