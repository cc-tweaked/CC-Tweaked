/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api;

import dan200.computercraft.ComputerCraft;
import net.fabricmc.fabric.api.tag.TagFactory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Tags provided by ComputerCraft.
 */
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
            return TagFactory.ITEM.create( new ResourceLocation( ComputerCraft.MOD_ID, name ) );
        }
    }

    public static class Blocks
    {
        public static final Tag.Named<Block> COMPUTER = make( "computer" );
        public static final Tag.Named<Block> TURTLE = make( "turtle" );
        public static final Tag.Named<Block> WIRED_MODEM = make( "wired_modem" );
        public static final Tag.Named<Block> MONITOR = make( "monitor" );

        /**
         * Blocks which can be broken by any turtle tool.
         */
        public static final Tag.Named<Block> TURTLE_ALWAYS_BREAKABLE = make( "turtle_always_breakable" );

        /**
         * Blocks which can be broken by the default shovel tool.
         */
        public static final Tag.Named<Block> TURTLE_SHOVEL_BREAKABLE = make( "turtle_shovel_harvestable" );

        /**
         * Blocks which can be broken with the default sword tool.
         */
        public static final Tag.Named<Block> TURTLE_SWORD_BREAKABLE = make( "turtle_sword_harvestable" );

        /**
         * Blocks which can be broken with the default hoe tool.
         */
        public static final Tag.Named<Block> TURTLE_HOE_BREAKABLE = make( "turtle_hoe_harvestable" );

        private static Tag.Named<Block> make( String name )
        {
            return TagFactory.BLOCK.create( new ResourceLocation( ComputerCraft.MOD_ID, name ) );
        }
    }
}
