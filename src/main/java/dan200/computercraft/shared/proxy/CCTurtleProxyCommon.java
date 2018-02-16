/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.proxy;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.event.TurtleActionEvent;
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
import dan200.computercraft.shared.turtle.recipes.TurtleUpgradeRecipe;
import dan200.computercraft.shared.turtle.upgrades.*;
import dan200.computercraft.shared.util.IEntityDropConsumer;
import dan200.computercraft.shared.util.ImpostorRecipe;
import dan200.computercraft.shared.util.InventoryUtil;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.IForgeRegistry;

import javax.annotation.Nonnull;
import java.util.*;

public abstract class CCTurtleProxyCommon implements ICCTurtleProxy
{    
    private Map<Integer, ITurtleUpgrade> m_legacyTurtleUpgrades;
    private Map<String, ITurtleUpgrade> m_turtleUpgrades;
    private Map<Entity, IEntityDropConsumer> m_dropConsumers;

    public CCTurtleProxyCommon()
    {
        m_legacyTurtleUpgrades = new HashMap<>();
        m_turtleUpgrades = new HashMap<>();
        m_dropConsumers = new WeakHashMap<>();
    }
    
    // ICCTurtleProxy implementation
    
    @Override        
    public void preInit()
    {
        MinecraftForge.EVENT_BUS.register( this );

        EntityRegistry.registerModEntity(
            new ResourceLocation( ComputerCraft.MOD_ID, "turtle_player" ), TurtlePlayer.class, "turtle_player",
            0, ComputerCraft.instance, Integer.MAX_VALUE, Integer.MAX_VALUE, false
        );

        registerUpgrades();

        // Recipe types
        // RecipeSorter.register( "computercraft:turtle", TurtleRecipe.class, RecipeSorter.Category.SHAPED, "after:minecraft:shapeless" );
        // RecipeSorter.register( "computercraft:turtle_upgrade", TurtleUpgradeRecipe.class, RecipeSorter.Category.SHAPED, "after:minecraft:shapeless" );
    }
    
    @Override        
    public void init()
    {
        registerForgeHandlers();
        registerTileEntities();
    }

    @Override
    public void registerTurtleUpgrade( ITurtleUpgrade upgrade )
    {
        // Check conditions
        int id = upgrade.getLegacyUpgradeID();
        if( id >= 0 && id < 64 )
        {
            String message = "Error registering '" + upgrade.getUnlocalisedAdjective() + " Turtle'. Legacy UpgradeID '" + id + "' is reserved by ComputerCraft";
            ComputerCraft.log.error( message );
            throw new RuntimeException( message );
        }
        
        // Register
        registerTurtleUpgradeInternal( upgrade );
    }

    @Override
    public ITurtleUpgrade getTurtleUpgrade( String id )
    {
        return m_turtleUpgrades.get( id );
    }

    @Override
    public ITurtleUpgrade getTurtleUpgrade( int legacyId )
    {
        return m_legacyTurtleUpgrades.get( legacyId );
    }
    
    @Override
    public ITurtleUpgrade getTurtleUpgrade( @Nonnull ItemStack stack )
    {
        for( ITurtleUpgrade upgrade : m_turtleUpgrades.values() )
        {
            try
            {
                ItemStack upgradeStack = upgrade.getCraftingItem();
                if( InventoryUtil.areItemsStackable( upgradeStack, stack ) )
                {
                    return upgrade;
                }
            }
            catch( Exception e )
            {
                ComputerCraft.log.error("Error getting computer upgrade item", e);
            }
        }
        return null;
    }

    public static boolean isUpgradeVanilla( ITurtleUpgrade upgrade )
    {
        return upgrade instanceof TurtleTool || upgrade instanceof TurtleModem || upgrade instanceof TurtleCraftingTable || upgrade instanceof TurtleSpeaker;
    }

    public static boolean isUpgradeSuitableForFamily( ComputerFamily family, ITurtleUpgrade upgrade )
    {
        if( family == ComputerFamily.Beginners )
        {
            return upgrade.getType().isTool();
        }
        else
        {
            return true;
        }
    }
    
    private void addAllUpgradedTurtles( ComputerFamily family, NonNullList<ItemStack> list )
    {
        ItemStack basicStack = TurtleItemFactory.create( -1, null, -1, family, null, null, 0, null );
        if( !basicStack.isEmpty() )
        {
            list.add( basicStack );
        }
        addUpgradedTurtle( family, ComputerCraft.Upgrades.diamondPickaxe, list );
        addUpgradedTurtle( family, ComputerCraft.Upgrades.diamondAxe, list );
        addUpgradedTurtle( family, ComputerCraft.Upgrades.diamondSword, list );
        addUpgradedTurtle( family, ComputerCraft.Upgrades.diamondShovel, list );
        addUpgradedTurtle( family, ComputerCraft.Upgrades.diamondHoe, list );
        addUpgradedTurtle( family, ComputerCraft.Upgrades.craftingTable, list );
        addUpgradedTurtle( family, ComputerCraft.Upgrades.wirelessModem, list );
        addUpgradedTurtle( family, ComputerCraft.Upgrades.advancedModem, list );
        addUpgradedTurtle( family, ComputerCraft.Upgrades.turtleSpeaker, list );
    }

    private void addUpgradedTurtle( ComputerFamily family, ITurtleUpgrade upgrade, List<ItemStack> list )
    {
        if ( isUpgradeSuitableForFamily( family, upgrade ) )
        {
            ItemStack stack = TurtleItemFactory.create( -1, null, -1, family, upgrade, null, 0, null );
            if( !stack.isEmpty() )
            {
                list.add( stack );
            }
        }
    }
    
    @Override
    public void addAllUpgradedTurtles( NonNullList<ItemStack> list )
    {
        addAllUpgradedTurtles( ComputerFamily.Normal, list );
        addAllUpgradedTurtles( ComputerFamily.Advanced, list );
    }
    
    @Override
    public void setEntityDropConsumer( Entity entity, IEntityDropConsumer consumer )
    {
        if( !m_dropConsumers.containsKey( entity ) )
        {
            boolean captured = entity.captureDrops;
            
            if( !captured )
            {
                entity.captureDrops = true;
                ArrayList<EntityItem> items = entity.capturedDrops;
                
                if( items == null || items.size() == 0 )
                {
                    m_dropConsumers.put( entity, consumer );
                }
            }
        }
    }    
    
    @Override
    public void clearEntityDropConsumer( Entity entity )
    {
        if( m_dropConsumers.containsKey( entity ) )
        {
            boolean captured = entity.captureDrops;
            
            if( captured )
            {
                entity.captureDrops = false;
                ArrayList<EntityItem> items = entity.capturedDrops;
                
                if( items != null )
                {
                    dispatchEntityDrops( entity, items );
                    items.clear();
                }
            }
            m_dropConsumers.remove( entity );
        }
    }

    private void registerTurtleUpgradeInternal( ITurtleUpgrade upgrade )
    {
        // Check conditions
        int legacyID = upgrade.getLegacyUpgradeID();
        if( legacyID >= 0 )
        {
            if( legacyID >= Short.MAX_VALUE )
            {
                String message = "Error registering '" + upgrade.getUnlocalisedAdjective() + " Turtle'. UpgradeID '" + legacyID + "' is out of range";
                ComputerCraft.log.error( message );
                throw new RuntimeException( message );
            }

            ITurtleUpgrade existing = m_legacyTurtleUpgrades.get( legacyID );
            if( existing != null )
            {
                String message = "Error registering '" + upgrade.getUnlocalisedAdjective() + " Turtle'. UpgradeID '" + legacyID + "' is already registered by '" + existing.getUnlocalisedAdjective() + " Turtle'";
                ComputerCraft.log.error( message );
                throw new RuntimeException( message );
            }
        }

        String id = upgrade.getUpgradeID().toString();
        ITurtleUpgrade existing = m_turtleUpgrades.get( id );
        if( existing != null )
        {
            String message = "Error registering '" + upgrade.getUnlocalisedAdjective() + " Turtle'. UpgradeID '" + id + "' is already registered by '" + existing.getUnlocalisedAdjective() + " Turtle'";
            ComputerCraft.log.error( message );
            throw new RuntimeException( message );
        }

        // Register
        if( legacyID >= 0 )
        {
            m_legacyTurtleUpgrades.put( legacyID, upgrade );
        }
        m_turtleUpgrades.put( id, upgrade );
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

        registry.register( new ItemTurtleLegacy( ComputerCraft.Blocks.turtle).setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "turtle" ) ) );
        registry.register( new ItemTurtleNormal( ComputerCraft.Blocks.turtleExpanded ).setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "turtle_expanded" ) ) );
        registry.register( new ItemTurtleAdvanced( ComputerCraft.Blocks.turtleAdvanced ).setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "turtle_advanced" ) ) );
    }

    @SubscribeEvent
    public void registerRecipes( RegistryEvent.Register<IRecipe> event )
    {
        IForgeRegistry<IRecipe> registry = event.getRegistry();
        registry.register( new TurtleUpgradeRecipe().setRegistryName( new ResourceLocation( "computercraft:turtle" ) ) );

        // Add a bunch of impostor recipes
        // TODO: Figure out a way to do this in a "nice" way.
        for( ITurtleUpgrade upgrade : m_turtleUpgrades.values() )
        {
            if( !isUpgradeVanilla( upgrade ) ) continue;

            // Add fake recipes to fool NEI
            ItemStack craftingItem = upgrade.getCraftingItem();

            // A turtle just containing this upgrade
            for( ComputerFamily family : ComputerFamily.values() )
            {
                if( !isUpgradeSuitableForFamily( family, upgrade ) )
                {
                    continue;
                }

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
        ComputerCraft.Upgrades.wirelessModem =  new TurtleModem( false, new ResourceLocation( "computercraft", "wireless_modem" ), 1 );
        registerTurtleUpgradeInternal( ComputerCraft.Upgrades.wirelessModem );

        ComputerCraft.Upgrades.craftingTable = new TurtleCraftingTable( 2 );
        registerTurtleUpgradeInternal( ComputerCraft.Upgrades.craftingTable );

        ComputerCraft.Upgrades.diamondSword = new TurtleSword( new ResourceLocation( "minecraft", "diamond_sword" ), 3, "upgrade.minecraft:diamond_sword.adjective", Items.DIAMOND_SWORD );
        registerTurtleUpgradeInternal( ComputerCraft.Upgrades.diamondSword );

        ComputerCraft.Upgrades.diamondShovel = new TurtleShovel( new ResourceLocation( "minecraft", "diamond_shovel" ), 4, "upgrade.minecraft:diamond_shovel.adjective", Items.DIAMOND_SHOVEL );
        registerTurtleUpgradeInternal( ComputerCraft.Upgrades.diamondShovel );

        ComputerCraft.Upgrades.diamondPickaxe = new TurtleTool( new ResourceLocation( "minecraft", "diamond_pickaxe" ), 5, "upgrade.minecraft:diamond_pickaxe.adjective", Items.DIAMOND_PICKAXE );
        registerTurtleUpgradeInternal( ComputerCraft.Upgrades.diamondPickaxe );

        ComputerCraft.Upgrades.diamondAxe = new TurtleAxe( new ResourceLocation( "minecraft", "diamond_axe" ), 6, "upgrade.minecraft:diamond_axe.adjective", Items.DIAMOND_AXE );
        registerTurtleUpgradeInternal( ComputerCraft.Upgrades.diamondAxe );

        ComputerCraft.Upgrades.diamondHoe = new TurtleHoe( new ResourceLocation( "minecraft", "diamond_hoe" ), 7, "upgrade.minecraft:diamond_hoe.adjective", Items.DIAMOND_HOE );
        registerTurtleUpgradeInternal( ComputerCraft.Upgrades.diamondHoe );

        ComputerCraft.Upgrades.advancedModem = new TurtleModem( true, new ResourceLocation( "computercraft", "advanced_modem" ), -1 );
        registerTurtleUpgradeInternal( ComputerCraft.Upgrades.advancedModem );

        ComputerCraft.Upgrades.turtleSpeaker = new TurtleSpeaker( new ResourceLocation( "computercraft", "speaker" ), 8 );
        registerTurtleUpgradeInternal( ComputerCraft.Upgrades.turtleSpeaker );
    }

    @SubscribeEvent
    public void remapItems( RegistryEvent.MissingMappings<Item> mappings )
    {
        // We have to use mappings.getAllMappings() as the mod ID is upper case but the domain lower.
        for( RegistryEvent.MissingMappings.Mapping<Item> mapping : mappings.getAllMappings() )
        {
            String domain = mapping.key.getResourceDomain();
            if( !domain.equalsIgnoreCase( ComputerCraft.MOD_ID ) ) continue;

            String key = mapping.key.getResourcePath();
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
            String domain = mapping.key.getResourceDomain();
            if( !domain.equalsIgnoreCase( ComputerCraft.MOD_ID ) ) continue;

            String key = mapping.key.getResourcePath();
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
        GameRegistry.registerTileEntity( TileTurtle.class, ComputerCraft.LOWER_ID + " : " + "turtle" );
        GameRegistry.registerTileEntity( TileTurtleExpanded.class, ComputerCraft.LOWER_ID + " : " + "turtleex" );
        GameRegistry.registerTileEntity( TileTurtleAdvanced.class, ComputerCraft.LOWER_ID + " : " + "turtleadv" );
    }
    
    private void registerForgeHandlers()
    {
        ForgeHandlers handlers = new ForgeHandlers();
        MinecraftForge.EVENT_BUS.register( handlers );
    }
        
    public class ForgeHandlers
    {
        private ForgeHandlers()
        {
        }

        // Forge event responses 
        @SubscribeEvent
        public void onEntityLivingDrops( LivingDropsEvent event )
        {
            dispatchEntityDrops( event.getEntity(), event.getDrops() );
        }
        
        @SubscribeEvent
        public void onTurtleAction( TurtleActionEvent event) {
            if( ComputerCraft.turtleDisabledActions.contains( event.getAction() ) )
            {
                event.setCanceled( true, "Action has been disabled" );
            }
        }
    }
    
    private void dispatchEntityDrops( Entity entity, java.util.List<EntityItem> drops )
    {
        IEntityDropConsumer consumer = m_dropConsumers.get( entity );
        if( consumer != null )
        {
            // All checks have passed, lets dispatch the drops
            for(EntityItem entityItem : drops)
            {
                consumer.consumeDrop( entity, entityItem.getItem() );
            }
            drops.clear();
        }
    }
}
