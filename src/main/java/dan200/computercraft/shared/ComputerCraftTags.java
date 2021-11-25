/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared;

import dan200.computercraft.ComputerCraft;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ResourceLocation;

public class ComputerCraftTags
{
    public static class Items
    {
        public static final ITag.INamedTag<Item> COMPUTER = make( "computer" );
        public static final ITag.INamedTag<Item> TURTLE = make( "turtle" );
        public static final ITag.INamedTag<Item> WIRED_MODEM = make( "wired_modem" );
        public static final ITag.INamedTag<Item> MONITOR = make( "monitor" );

        private static ITag.INamedTag<Item> make( String name )
        {
            return ItemTags.bind( new ResourceLocation( ComputerCraft.MOD_ID, name ).toString() );
        }
    }

    public static class Blocks
    {
        public static final ITag.INamedTag<Block> COMPUTER = make( "computer" );
        public static final ITag.INamedTag<Block> TURTLE = make( "turtle" );
        public static final ITag.INamedTag<Block> WIRED_MODEM = make( "wired_modem" );
        public static final ITag.INamedTag<Block> MONITOR = make( "monitor" );

        public static final ITag.INamedTag<Block> TURTLE_ALWAYS_BREAKABLE = make( "turtle_always_breakable" );
        public static final ITag.INamedTag<Block> TURTLE_SHOVEL_BREAKABLE = make( "turtle_shovel_harvestable" );
        public static final ITag.INamedTag<Block> TURTLE_SWORD_BREAKABLE = make( "turtle_sword_harvestable" );
        public static final ITag.INamedTag<Block> TURTLE_HOE_BREAKABLE = make( "turtle_hoe_harvestable" );

        private static ITag.INamedTag<Block> make( String name )
        {
            return BlockTags.bind( new ResourceLocation( ComputerCraft.MOD_ID, name ).toString() );
        }
    }
}
