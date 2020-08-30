/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.data;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.Registry;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.server.BlockTagsProvider;
import net.minecraft.data.server.ItemTagsProvider;
import net.minecraft.item.Item;
import net.minecraft.tag.ItemTags;
import net.minecraft.tag.Tag;
import net.minecraft.util.Identifier;

import static dan200.computercraft.data.Tags.CCTags.*;

public class Tags extends ItemTagsProvider
{
    private static final Tag.Identified<Item> PIGLIN_LOVED = ItemTags.PIGLIN_LOVED;

    public static class CCTags
    {
        public static final Tag.Identified<Item> COMPUTER = item( "computer" );
        public static final Tag.Identified<Item> TURTLE = item( "turtle" );
        public static final Tag.Identified<Item> WIRED_MODEM = item( "wired_modem" );
        public static final Tag.Identified<Item> MONITOR = item( "monitor" );
    }

    public Tags( DataGenerator generator, BlockTagsProvider tags )
    {
        super( generator, tags );
    }

    @Override
    protected void configure()
    {
        getOrCreateTagBuilder( COMPUTER ).add(
            Registry.ModItems.COMPUTER_NORMAL.get(),
            Registry.ModItems.COMPUTER_ADVANCED.get(),
            Registry.ModItems.COMPUTER_COMMAND.get()
        );
        getOrCreateTagBuilder( TURTLE ).add( Registry.ModItems.TURTLE_NORMAL.get(), Registry.ModItems.TURTLE_ADVANCED.get() );
        getOrCreateTagBuilder( WIRED_MODEM ).add( Registry.ModItems.WIRED_MODEM.get(), Registry.ModItems.WIRED_MODEM_FULL.get() );
        getOrCreateTagBuilder( MONITOR ).add( Registry.ModItems.MONITOR_NORMAL.get(), Registry.ModItems.MONITOR_ADVANCED.get() );

        getOrCreateTagBuilder( PIGLIN_LOVED ).add(
            Registry.ModItems.COMPUTER_ADVANCED.get(), Registry.ModItems.TURTLE_ADVANCED.get(),
            Registry.ModItems.WIRELESS_MODEM_ADVANCED.get(), Registry.ModItems.POCKET_COMPUTER_ADVANCED.get(),
            Registry.ModItems.MONITOR_ADVANCED.get()
        );
    }

    private static Tag.Identified<Item> item( String name )
    {
        return ItemTags.register( new Identifier( ComputerCraft.MOD_ID, name ).toString() );
    }
}
