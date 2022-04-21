/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api;

import dan200.computercraft.ComputerCraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Tags provided by ComputerCraft.
 */
public class ComputerCraftTags
{
    public static class Items
    {
        public static final TagKey<Item> COMPUTER = make( "computer" );
        public static final TagKey<Item> TURTLE = make( "turtle" );
        public static final TagKey<Item> WIRED_MODEM = make( "wired_modem" );
        public static final TagKey<Item> MONITOR = make( "monitor" );

        private static TagKey<Item> make( String name )
        {
            return ItemTags.create( new ResourceLocation( ComputerCraft.MOD_ID, name ) );
        }
    }

    public static class Blocks
    {
        public static final TagKey<Block> COMPUTER = make( "computer" );
        public static final TagKey<Block> TURTLE = make( "turtle" );
        public static final TagKey<Block> WIRED_MODEM = make( "wired_modem" );
        public static final TagKey<Block> MONITOR = make( "monitor" );

        /**
         * Blocks which can be broken by any turtle tool.
         */
        public static final TagKey<Block> TURTLE_ALWAYS_BREAKABLE = make( "turtle_always_breakable" );

        /**
         * Blocks which can be broken by the default shovel tool.
         */
        public static final TagKey<Block> TURTLE_SHOVEL_BREAKABLE = make( "turtle_shovel_harvestable" );

        /**
         * Blocks which can be broken with the default sword tool.
         */
        public static final TagKey<Block> TURTLE_SWORD_BREAKABLE = make( "turtle_sword_harvestable" );

        /**
         * Blocks which can be broken with the default hoe tool.
         */
        public static final TagKey<Block> TURTLE_HOE_BREAKABLE = make( "turtle_hoe_harvestable" );

        private static TagKey<Block> make( String name )
        {
            return BlockTags.create( new ResourceLocation( ComputerCraft.MOD_ID, name ) );
        }
    }
}
