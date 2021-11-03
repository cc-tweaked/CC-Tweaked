/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared;

import dan200.computercraft.ComputerCraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ComputerCraftTags
{
    public static class Items
    {
        public static final Tag.Named<Item> COMPUTER = make( "computer" );
        public static final Tag.Named<Item> TURTLE = make( "turtle" );
        public static final Tag.Named<Item> WIRED_MODEM = make( "wired_modem" );
        public static final Tag.Named<Item> MONITOR = make( "monitor" );

        private static Tag.Named<Item> make( String name )
        {
            return ItemTags.bind( new ResourceLocation( ComputerCraft.MOD_ID, name ).toString() );
        }
    }

    public static class Blocks
    {
        public static final Tag.Named<Block> COMPUTER = make( "computer" );
        public static final Tag.Named<Block> TURTLE = make( "turtle" );
        public static final Tag.Named<Block> WIRED_MODEM = make( "wired_modem" );
        public static final Tag.Named<Block> MONITOR = make( "monitor" );

        public static final Tag.Named<Block> TURTLE_ALWAYS_BREAKABLE = make( "turtle_always_breakable" );
        public static final Tag.Named<Block> TURTLE_SHOVEL_BREAKABLE = make( "turtle_shovel_harvestable" );
        public static final Tag.Named<Block> TURTLE_SWORD_BREAKABLE = make( "turtle_sword_harvestable" );
        public static final Tag.Named<Block> TURTLE_HOE_BREAKABLE = make( "turtle_hoe_harvestable" );

        private static Tag.Named<Block> make( String name )
        {
            return BlockTags.bind( new ResourceLocation( ComputerCraft.MOD_ID, name ).toString() );
        }
    }
}
