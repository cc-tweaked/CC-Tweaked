/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.data;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.Registry;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.tags.BlockTagsProvider;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.Tag;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.data.ExistingFileHelper;

import static dan200.computercraft.data.Tags.CCTags.*;

public class Tags extends ItemTagsProvider
{
    private static final Tag.Named<Item> PIGLIN_LOVED = ItemTags.PIGLIN_LOVED;

    public static class CCTags
    {
        public static final Tag.Named<Item> COMPUTER = item( "computer" );
        public static final Tag.Named<Item> TURTLE = item( "turtle" );
        public static final Tag.Named<Item> WIRED_MODEM = item( "wired_modem" );
        public static final Tag.Named<Item> MONITOR = item( "monitor" );
    }

    public Tags( DataGenerator generator, ExistingFileHelper helper )
    {
        super( generator, new BlockTagsProvider( generator, ComputerCraft.MOD_ID, helper ), ComputerCraft.MOD_ID, helper );
    }

    @Override
    protected void addTags()
    {
        tag( COMPUTER ).add(
            Registry.ModItems.COMPUTER_NORMAL.get(),
            Registry.ModItems.COMPUTER_ADVANCED.get(),
            Registry.ModItems.COMPUTER_COMMAND.get()
        );
        tag( TURTLE ).add( Registry.ModItems.TURTLE_NORMAL.get(), Registry.ModItems.TURTLE_ADVANCED.get() );
        tag( WIRED_MODEM ).add( Registry.ModItems.WIRED_MODEM.get(), Registry.ModItems.WIRED_MODEM_FULL.get() );
        tag( MONITOR ).add( Registry.ModItems.MONITOR_NORMAL.get(), Registry.ModItems.MONITOR_ADVANCED.get() );

        tag( PIGLIN_LOVED ).add(
            Registry.ModItems.COMPUTER_ADVANCED.get(), Registry.ModItems.TURTLE_ADVANCED.get(),
            Registry.ModItems.WIRELESS_MODEM_ADVANCED.get(), Registry.ModItems.POCKET_COMPUTER_ADVANCED.get(),
            Registry.ModItems.MONITOR_ADVANCED.get()
        );
    }

    private static Tag.Named<Item> item( String name )
    {
        return ItemTags.bind( new ResourceLocation( ComputerCraft.MOD_ID, name ).toString() );
    }
}
