/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.data;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.Registry;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.ItemTagsProvider;
import net.minecraft.item.Item;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.Tag;
import net.minecraft.util.ResourceLocation;

import static dan200.computercraft.data.Tags.CCTags.*;

public class Tags extends ItemTagsProvider
{
    public static class CCTags
    {
        public static final Tag<Item> COMPUTER = item( "computer" );
        public static final Tag<Item> TURTLE = item( "turtle" );
        public static final Tag<Item> WIRED_MODEM = item( "wired_modem" );
        public static final Tag<Item> MONITOR = item( "monitor" );
    }

    public Tags( DataGenerator generator )
    {
        super( generator );
    }

    @Override
    protected void addTags()
    {
        tag( COMPUTER )
            .add( Registry.ModItems.COMPUTER_NORMAL.get() )
            .add( Registry.ModItems.COMPUTER_ADVANCED.get() )
            .add( Registry.ModItems.COMPUTER_COMMAND.get() );
        tag( TURTLE ).add( Registry.ModItems.TURTLE_NORMAL.get(), Registry.ModItems.TURTLE_ADVANCED.get() );
        tag( WIRED_MODEM ).add( Registry.ModItems.WIRED_MODEM.get(), Registry.ModItems.WIRED_MODEM_FULL.get() );
        tag( MONITOR )
            .add( Registry.ModItems.MONITOR_NORMAL.get() )
            .add( Registry.ModItems.MONITOR_ADVANCED.get() );
    }

    private static Tag<Item> item( String name )
    {
        return new ItemTags.Wrapper( new ResourceLocation( ComputerCraft.MOD_ID, name ) );
    }
}
