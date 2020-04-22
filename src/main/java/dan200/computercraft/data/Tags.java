/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.data;

import dan200.computercraft.ComputerCraft;
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
    protected void registerTags()
    {
        getBuilder( COMPUTER )
            .add( ComputerCraft.Items.computerNormal )
            .add( ComputerCraft.Items.computerAdvanced )
            .add( ComputerCraft.Items.computerCommand );
        getBuilder( TURTLE ).add( ComputerCraft.Items.turtleNormal, ComputerCraft.Items.turtleAdvanced );
        getBuilder( WIRED_MODEM ).add( ComputerCraft.Items.wiredModem, ComputerCraft.Blocks.wiredModemFull.asItem() );
        getBuilder( MONITOR )
            .add( ComputerCraft.Blocks.monitorNormal.asItem() )
            .add( ComputerCraft.Blocks.monitorAdvanced.asItem() );
    }

    private static Tag<Item> item( String name )
    {
        return new ItemTags.Wrapper( new ResourceLocation( ComputerCraft.MOD_ID, name ) );
    }
}
