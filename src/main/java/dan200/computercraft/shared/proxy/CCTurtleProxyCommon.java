/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.proxy;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.event.TurtleActionEvent;
import dan200.computercraft.shared.TurtleUpgrades;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.turtle.blocks.BlockTurtle;
import dan200.computercraft.shared.turtle.blocks.TileTurtle;
import dan200.computercraft.shared.turtle.blocks.TileTurtleAdvanced;
import dan200.computercraft.shared.turtle.blocks.TileTurtleExpanded;
import dan200.computercraft.shared.turtle.core.TurtlePlayer;
import dan200.computercraft.shared.turtle.items.ItemTurtleAdvanced;
import dan200.computercraft.shared.turtle.items.ItemTurtleLegacy;
import dan200.computercraft.shared.turtle.items.ItemTurtleNormal;
import dan200.computercraft.shared.turtle.items.TurtleItemFactory;
import dan200.computercraft.shared.turtle.upgrades.*;
import dan200.computercraft.shared.util.ImpostorRecipe;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.IForgeRegistry;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public abstract class CCTurtleProxyCommon implements ICCTurtleProxy
{
    private Function<ItemStack, ItemStack> dropConsumer;
    private List<ItemStack> remainingDrops;
    private WeakReference<World> dropWorld;
    private BlockPos dropPos;
    private AxisAlignedBB dropBounds;
    private WeakReference<Entity> dropEntity;

    @Override
    public void preInit()
    {
        MinecraftForge.EVENT_BUS.register( this );

        EntityRegistry.registerModEntity(
            new ResourceLocation( ComputerCraft.MOD_ID, "turtle_player" ), TurtlePlayer.class, "turtle_player",
            0, ComputerCraft.instance, Integer.MAX_VALUE, Integer.MAX_VALUE, false
        );
    }

    @Override
    public void init()
    {
        registerForgeHandlers();
        registerTileEntities();
    }

    @Override
    public void setDropConsumer( Entity entity, Function<ItemStack, ItemStack> consumer )
    {
        dropConsumer = consumer;
        remainingDrops = new ArrayList<>();
        dropEntity = new WeakReference<>( entity );
        dropWorld = new WeakReference<>( entity.world );
        dropPos = null;
        dropBounds = new AxisAlignedBB( entity.getPosition() ).grow( 2, 2, 2 );

        entity.captureDrops = true;
    }

    @Override
    public void setDropConsumer( World world, BlockPos pos, Function<ItemStack, ItemStack> consumer )
    {
        dropConsumer = consumer;
        remainingDrops = new ArrayList<>();
        dropEntity = null;
        dropWorld = new WeakReference<>( world );
        dropPos = pos;
        dropBounds = new AxisAlignedBB( pos ).grow( 2, 2, 2 );
    }

    @Override
    public List<ItemStack> clearDropConsumer()
    {
        if( dropEntity != null )
        {
            Entity entity = dropEntity.get();
            if( entity != null )
            {
                entity.captureDrops = false;
                if( entity.capturedDrops != null )
                {
                    for( EntityItem entityItem : entity.capturedDrops ) handleDrops( entityItem.getItem() );
                    entity.capturedDrops.clear();
                }
            }
        }

        List<ItemStack> remainingStacks = remainingDrops;

        dropConsumer = null;
        remainingDrops = null;
        dropEntity = null;
        dropWorld = null;
        dropPos = null;
        dropBounds = null;

        return remainingStacks;
    }

    @SubscribeEvent
    public void registerBlocks( RegistryEvent.Register<Block> event )
    {
        IForgeRegistry<Block> registry = event.getRegistry();

        // Turtle
        ComputerCraft.Blocks.turtle = BlockTurtle.createTurtleBlock();
        registry.register( ComputerCraft.Blocks.turtle.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "turtle" ) ) );

        ComputerCraft.Blocks.turtleExpanded = BlockTurtle.createTurtleBlock();
        registry.register( ComputerCraft.Blocks.turtleExpanded.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "turtle_expanded" ) ) );

        // Advanced Turtle
        ComputerCraft.Blocks.turtleAdvanced = BlockTurtle.createTurtleBlock();
        registry.register( ComputerCraft.Blocks.turtleAdvanced.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "turtle_advanced" ) ) );
    }

    @SubscribeEvent
    public void registerItems( RegistryEvent.Register<Item> event )
    {
        IForgeRegistry<Item> registry = event.getRegistry();

        registry.register( new ItemTurtleLegacy( ComputerCraft.Blocks.turtle ).setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "turtle" ) ) );
        registry.register( new ItemTurtleNormal( ComputerCraft.Blocks.turtleExpanded ).setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "turtle_expanded" ) ) );
        registry.register( new ItemTurtleAdvanced( ComputerCraft.Blocks.turtleAdvanced ).setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "turtle_advanced" ) ) );

        registerUpgrades();
    }

    @SubscribeEvent
    public void registerRecipes( RegistryEvent.Register<IRecipe> event )
    {
        IForgeRegistry<IRecipe> registry = event.getRegistry();

        // Add a bunch of impostor recipes
        // TODO: Figure out a way to do this in a "nice" way.
        for( ITurtleUpgrade upgrade : TurtleUpgrades.getVanillaUpgrades() )
        {
            // Add fake recipes to fool NEI
            ItemStack craftingItem = upgrade.getCraftingItem();

            // A turtle just containing this upgrade
            for( ComputerFamily family : ComputerFamily.values() )
            {
                if( !TurtleUpgrades.suitableForFamily( family, upgrade ) ) continue;

                ItemStack baseTurtle = TurtleItemFactory.create( -1, null, -1, family, null, null, 0, null );
                if( !baseTurtle.isEmpty() )
                {
                    ItemStack craftedTurtle = TurtleItemFactory.create( -1, null, -1, family, upgrade, null, 0, null );
                    ItemStack craftedTurtleFlipped = TurtleItemFactory.create( -1, null, -1, family, null, upgrade, 0, null );
                    registry.register(
                        new ImpostorRecipe( "computercraft:" + family.toString() + "_turtle_upgrade", 2, 1, new ItemStack[] { baseTurtle, craftingItem }, craftedTurtle )
                            .setRegistryName( new ResourceLocation( "computercraft:" + family + "_turtle_upgrade_" + upgrade.getUpgradeID().toString().replace( ':', '_' ) + "_1" ) )
                    );
                    registry.register(
                        new ImpostorRecipe( "computercraft:" + family.toString() + "_turtle_upgrade", 2, 1, new ItemStack[] { craftingItem, baseTurtle }, craftedTurtleFlipped )
                            .setRegistryName( new ResourceLocation( "computercraft:" + family + "_turtle_upgrade_" + upgrade.getUpgradeID().toString().replace( ':', '_' ) + "_2" ) )
                    );

                    /*
                    // A turtle containing this upgrade and another upgrade
                    for( ITurtleUpgrade otherUpgrade : m_turtleUpgrades.values() )
                    {
                        if( isUpgradeVanilla( otherUpgrade ) && isUpgradeSuitableForFamily( family, otherUpgrade ) )
                        {
                            ItemStack otherCraftingItem = otherUpgrade.getCraftingItem();

                            ItemStack otherCraftedTurtle = TurtleItemFactory.create( -1, null, -1, family, null, otherUpgrade, 0, null );
                            ItemStack comboCraftedTurtle = TurtleItemFactory.create( -1, null, -1, family, upgrade, otherUpgrade, 0, null );

                            ItemStack otherCraftedTurtleFlipped = TurtleItemFactory.create( -1, null, -1, family, otherUpgrade, null, 0, null );
                            ItemStack comboCraftedTurtleFlipped = TurtleItemFactory.create( -1, null, -1, family, otherUpgrade, upgrade, 0, null );

                            recipeList.add( new ImpostorRecipe( 2, 1, new ItemStack[] { otherCraftingItem, craftedTurtle }, comboCraftedTurtle ) );
                            recipeList.add( new ImpostorRecipe( 2, 1, new ItemStack[] { otherCraftedTurtle, craftingItem }, comboCraftedTurtle ) );
                            recipeList.add( new ImpostorRecipe( 2, 1, new ItemStack[] { craftedTurtleFlipped, otherCraftingItem }, comboCraftedTurtleFlipped ) );
                            recipeList.add( new ImpostorRecipe( 2, 1, new ItemStack[] { craftingItem, otherCraftedTurtleFlipped }, comboCraftedTurtleFlipped ) );
                            recipeList.add( new ImpostorRecipe( 3, 1, new ItemStack[] { otherCraftingItem, baseTurtle, craftingItem,  }, comboCraftedTurtle ) );
                            recipeList.add( new ImpostorRecipe( 3, 1, new ItemStack[] { craftingItem, baseTurtle, otherCraftingItem }, comboCraftedTurtleFlipped ) );
                        }
                    }
                    */
                }
            }
        }
    }

    private void registerUpgrades()
    {
        // Upgrades
        ComputerCraft.Upgrades.wirelessModem = new TurtleModem( false, new ResourceLocation( "computercraft", "wireless_modem" ), 1 );
        TurtleUpgrades.registerInternal( ComputerCraft.Upgrades.wirelessModem );

        ComputerCraft.Upgrades.craftingTable = new TurtleCraftingTable( 2 );
        TurtleUpgrades.registerInternal( ComputerCraft.Upgrades.craftingTable );

        ComputerCraft.Upgrades.diamondSword = new TurtleSword( new ResourceLocation( "minecraft", "diamond_sword" ), 3, "upgrade.minecraft:diamond_sword.adjective", Items.DIAMOND_SWORD );
        TurtleUpgrades.registerInternal( ComputerCraft.Upgrades.diamondSword );

        ComputerCraft.Upgrades.diamondShovel = new TurtleShovel( new ResourceLocation( "minecraft", "diamond_shovel" ), 4, "upgrade.minecraft:diamond_shovel.adjective", Items.DIAMOND_SHOVEL );
        TurtleUpgrades.registerInternal( ComputerCraft.Upgrades.diamondShovel );

        ComputerCraft.Upgrades.diamondPickaxe = new TurtleTool( new ResourceLocation( "minecraft", "diamond_pickaxe" ), 5, "upgrade.minecraft:diamond_pickaxe.adjective", Items.DIAMOND_PICKAXE );
        TurtleUpgrades.registerInternal( ComputerCraft.Upgrades.diamondPickaxe );

        ComputerCraft.Upgrades.diamondAxe = new TurtleAxe( new ResourceLocation( "minecraft", "diamond_axe" ), 6, "upgrade.minecraft:diamond_axe.adjective", Items.DIAMOND_AXE );
        TurtleUpgrades.registerInternal( ComputerCraft.Upgrades.diamondAxe );

        ComputerCraft.Upgrades.diamondHoe = new TurtleHoe( new ResourceLocation( "minecraft", "diamond_hoe" ), 7, "upgrade.minecraft:diamond_hoe.adjective", Items.DIAMOND_HOE );
        TurtleUpgrades.registerInternal( ComputerCraft.Upgrades.diamondHoe );

        ComputerCraft.Upgrades.advancedModem = new TurtleModem( true, new ResourceLocation( "computercraft", "advanced_modem" ), -1 );
        TurtleUpgrades.registerInternal( ComputerCraft.Upgrades.advancedModem );

        ComputerCraft.Upgrades.turtleSpeaker = new TurtleSpeaker( new ResourceLocation( "computercraft", "speaker" ), 8 );
        TurtleUpgrades.registerInternal( ComputerCraft.Upgrades.turtleSpeaker );
    }

    @SubscribeEvent
    public void remapItems( RegistryEvent.MissingMappings<Item> mappings )
    {
        // We have to use mappings.getAllMappings() as the mod ID is upper case but the domain lower.
        for( RegistryEvent.MissingMappings.Mapping<Item> mapping : mappings.getAllMappings() )
        {
            String domain = mapping.key.getNamespace();
            if( !domain.equalsIgnoreCase( ComputerCraft.MOD_ID ) ) continue;

            String key = mapping.key.getPath();
            if( key.equalsIgnoreCase( "CC-Turtle" ) )
            {
                mapping.remap( Item.getItemFromBlock( ComputerCraft.Blocks.turtle ) );
            }
            else if( key.equalsIgnoreCase( "CC-TurtleExpanded" ) )
            {
                mapping.remap( Item.getItemFromBlock( ComputerCraft.Blocks.turtleExpanded ) );
            }
            else if( key.equalsIgnoreCase( "CC-TurtleAdvanced" ) )
            {
                mapping.remap( Item.getItemFromBlock( ComputerCraft.Blocks.turtleAdvanced ) );
            }
        }
    }

    @SubscribeEvent
    public void remapBlocks( RegistryEvent.MissingMappings<Block> mappings )
    {
        // We have to use mappings.getAllMappings() as the mod ID is upper case but the domain lower.
        for( RegistryEvent.MissingMappings.Mapping<Block> mapping : mappings.getAllMappings() )
        {
            String domain = mapping.key.getNamespace();
            if( !domain.equalsIgnoreCase( ComputerCraft.MOD_ID ) ) continue;

            String key = mapping.key.getPath();
            if( key.equalsIgnoreCase( "CC-Turtle" ) )
            {
                mapping.remap( ComputerCraft.Blocks.turtle );
            }
            else if( key.equalsIgnoreCase( "CC-TurtleExpanded" ) )
            {
                mapping.remap( ComputerCraft.Blocks.turtleExpanded );
            }
            else if( key.equalsIgnoreCase( "CC-TurtleAdvanced" ) )
            {
                mapping.remap( ComputerCraft.Blocks.turtleAdvanced );
            }
        }
    }

    private void registerTileEntities()
    {
        // TileEntities
        GameRegistry.registerTileEntity( TileTurtle.class, new ResourceLocation( ComputerCraft.MOD_ID, "turtle" ) );
        GameRegistry.registerTileEntity( TileTurtleExpanded.class, new ResourceLocation( ComputerCraft.MOD_ID, "turtleex" ) );
        GameRegistry.registerTileEntity( TileTurtleAdvanced.class, new ResourceLocation( ComputerCraft.MOD_ID, "turtleadv" ) );
    }

    private void registerForgeHandlers()
    {
        ForgeHandlers handlers = new ForgeHandlers();
        MinecraftForge.EVENT_BUS.register( handlers );
    }

    private void handleDrops( ItemStack stack )
    {
        ItemStack remaining = dropConsumer.apply( stack );
        if( !remaining.isEmpty() ) remainingDrops.add( remaining );
    }

    private class ForgeHandlers
    {
        @SubscribeEvent( priority = EventPriority.LOWEST )
        public void onEntityLivingDrops( LivingDropsEvent event )
        {
            // Capture any mob drops for the current entity
            if( dropEntity != null && event.getEntity() == dropEntity.get() )
            {
                List<EntityItem> drops = event.getDrops();
                for( EntityItem entityItem : drops ) handleDrops( entityItem.getItem() );
                drops.clear();
            }
        }

        @SubscribeEvent( priority = EventPriority.LOWEST )
        public void onHarvestDrops( BlockEvent.HarvestDropsEvent event )
        {
            // Capture block drops for the current entity
            if( dropWorld != null && dropWorld.get() == event.getWorld()
                && dropPos != null && dropPos.equals( event.getPos() ) )
            {
                for( ItemStack item : event.getDrops() )
                {
                    if( event.getWorld().rand.nextFloat() < event.getDropChance() ) handleDrops( item );
                }
                event.getDrops().clear();
            }
        }

        @SubscribeEvent( priority = EventPriority.LOWEST )
        public void onEntitySpawn( EntityJoinWorldEvent event )
        {
            // Capture any nearby item spawns
            if( dropWorld != null && dropWorld.get() == event.getWorld() && event.getEntity() instanceof EntityItem
                && dropBounds.contains( event.getEntity().getPositionVector() ) )
            {
                handleDrops( ((EntityItem) event.getEntity()).getItem() );
                event.setCanceled( true );
            }
        }

        @SubscribeEvent
        public void onTurtleAction( TurtleActionEvent event )
        {
            if( ComputerCraft.turtleDisabledActions.contains( event.getAction() ) )
            {
                event.setCanceled( true, "Action has been disabled" );
            }
        }
    }

}
