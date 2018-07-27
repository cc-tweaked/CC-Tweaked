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
import dan200.computercraft.shared.turtle.blocks.TileTurtleAdvanced;
import dan200.computercraft.shared.turtle.blocks.TileTurtleNormal;
import dan200.computercraft.shared.turtle.core.TurtlePlayer;
import dan200.computercraft.shared.turtle.items.ItemTurtle;
import dan200.computercraft.shared.turtle.items.TurtleItemFactory;
import dan200.computercraft.shared.turtle.recipes.TurtleUpgradeRecipe;
import dan200.computercraft.shared.turtle.upgrades.*;
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

import javax.annotation.Nonnull;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public abstract class CCTurtleProxyCommon implements ICCTurtleProxy
{
    private final Map<String, ITurtleUpgrade> m_turtleUpgrades = new HashMap<>();

    private Consumer<ItemStack> dropConsumer;
    private WeakReference<World> dropWorld;
    private BlockPos dropPos;
    private AxisAlignedBB dropBounds;
    private WeakReference<Entity> dropEntity;

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
        // Register
        registerTurtleUpgradeInternal( upgrade );
    }

    @Override
    public ITurtleUpgrade getTurtleUpgrade( String id )
    {
        return m_turtleUpgrades.get( id );
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
                ComputerCraft.log.error( "Error getting computer upgrade item", e );
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
        if( isUpgradeSuitableForFamily( family, upgrade ) )
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
    public void setDropConsumer( Entity entity, Consumer<ItemStack> consumer )
    {
        dropConsumer = consumer;
        dropEntity = new WeakReference<>( entity );
        dropWorld = new WeakReference<>( entity.world );
        dropPos = null;
        dropBounds = new AxisAlignedBB( entity.getPosition() ).grow( 2, 2, 2 );

        entity.captureDrops = true;
    }

    @Override
    public void setDropConsumer( World world, BlockPos pos, Consumer<ItemStack> consumer )
    {
        dropConsumer = consumer;
        dropEntity = null;
        dropWorld = new WeakReference<>( world );
        dropPos = pos;
        dropBounds = new AxisAlignedBB( pos ).grow( 2, 2, 2 );
    }

    @Override
    public void clearDropConsumer()
    {
        if( dropEntity != null )
        {
            Entity entity = dropEntity.get();
            if( entity != null )
            {
                entity.captureDrops = false;
                if( entity.capturedDrops != null )
                {
                    for( EntityItem entityItem : entity.capturedDrops ) dropConsumer.accept( entityItem.getItem() );
                    entity.capturedDrops.clear();
                }
            }
        }

        dropConsumer = null;
        dropEntity = null;
        dropWorld = null;
        dropPos = null;
        dropBounds = null;
    }

    private void registerTurtleUpgradeInternal( ITurtleUpgrade upgrade )
    {
        // Check conditions
        String id = upgrade.getUpgradeID().toString();
        ITurtleUpgrade existing = m_turtleUpgrades.get( id );
        if( existing != null )
        {
            String message = "Error registering '" + upgrade.getUnlocalisedAdjective() + " Turtle'. UpgradeID '" + id + "' is already registered by '" + existing.getUnlocalisedAdjective() + " Turtle'";
            ComputerCraft.log.error( message );
            throw new RuntimeException( message );
        }

        // Register
        m_turtleUpgrades.put( id, upgrade );
    }

    @SubscribeEvent
    public void registerBlocks( RegistryEvent.Register<Block> event )
    {
        IForgeRegistry<Block> registry = event.getRegistry();

        ComputerCraft.Blocks.turtleNormal = new BlockTurtle( ComputerFamily.Normal, TileTurtleNormal::new );
        registry.register( ComputerCraft.Blocks.turtleNormal.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "turtle_normal" ) ) );

        // Advanced Turtle
        ComputerCraft.Blocks.turtleAdvanced = new BlockTurtle( ComputerFamily.Advanced, TileTurtleAdvanced::new );
        registry.register( ComputerCraft.Blocks.turtleAdvanced.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "turtle_advanced" ) ) );
    }

    @SubscribeEvent
    public void registerItems( RegistryEvent.Register<Item> event )
    {
        IForgeRegistry<Item> registry = event.getRegistry();

        registry.register( new ItemTurtle( ComputerCraft.Blocks.turtleNormal ).setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "turtle_normal" ) ) );
        registry.register( new ItemTurtle( ComputerCraft.Blocks.turtleAdvanced ).setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "turtle_advanced" ) ) );
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
                if( !isUpgradeSuitableForFamily( family, upgrade ) )continue;
                String familyName = family.toString().toLowerCase( Locale.ENGLISH );

                ItemStack baseTurtle = TurtleItemFactory.create( -1, null, -1, family, null, null, 0, null );
                if( !baseTurtle.isEmpty() )
                {
                    ItemStack craftedTurtle = TurtleItemFactory.create( -1, null, -1, family, upgrade, null, 0, null );
                    ItemStack craftedTurtleFlipped = TurtleItemFactory.create( -1, null, -1, family, null, upgrade, 0, null );
                    registry.register(
                        new ImpostorRecipe( "computercraft:turtle_" + familyName + "_upgrade_", 2, 1, new ItemStack[]{ baseTurtle, craftingItem }, craftedTurtle )
                            .setRegistryName( new ResourceLocation( "computercraft:turtle_" + familyName + "_upgrade_" + upgrade.getUpgradeID().toString().replace( ':', '_' ) + "_1" ) )
                    );
                    registry.register(
                        new ImpostorRecipe( "computercraft:turtle_" + familyName + "_upgrade_", 2, 1, new ItemStack[]{ craftingItem, baseTurtle }, craftedTurtleFlipped )
                            .setRegistryName( new ResourceLocation( "computercraft:turtle_" + familyName + "_upgrade_" + upgrade.getUpgradeID().toString().replace( ':', '_' ) + "_2" ) )
                    );
                }
            }
        }
    }

    private void registerUpgrades()
    {
        // Upgrades
        ComputerCraft.Upgrades.wirelessModem = new TurtleModem( false, new ResourceLocation( "computercraft", "wireless_modem" ) );
        registerTurtleUpgradeInternal( ComputerCraft.Upgrades.wirelessModem );

        ComputerCraft.Upgrades.craftingTable = new TurtleCraftingTable( 2 );
        registerTurtleUpgradeInternal( ComputerCraft.Upgrades.craftingTable );

        ComputerCraft.Upgrades.diamondSword = new TurtleSword( new ResourceLocation( "minecraft", "diamond_sword" ), 3, "upgrade.minecraft:diamond_sword.adjective", Items.DIAMOND_SWORD );
        registerTurtleUpgradeInternal( ComputerCraft.Upgrades.diamondSword );

        ComputerCraft.Upgrades.diamondShovel = new TurtleShovel( new ResourceLocation( "minecraft", "diamond_shovel" ), 4, "upgrade.minecraft:diamond_shovel.adjective", Items.DIAMOND_SHOVEL );
        registerTurtleUpgradeInternal( ComputerCraft.Upgrades.diamondShovel );

        ComputerCraft.Upgrades.diamondPickaxe = new TurtleTool( new ResourceLocation( "minecraft", "diamond_pickaxe" ), "upgrade.minecraft:diamond_pickaxe.adjective", Items.DIAMOND_PICKAXE );
        registerTurtleUpgradeInternal( ComputerCraft.Upgrades.diamondPickaxe );

        ComputerCraft.Upgrades.diamondAxe = new TurtleAxe( new ResourceLocation( "minecraft", "diamond_axe" ), 6, "upgrade.minecraft:diamond_axe.adjective", Items.DIAMOND_AXE );
        registerTurtleUpgradeInternal( ComputerCraft.Upgrades.diamondAxe );

        ComputerCraft.Upgrades.diamondHoe = new TurtleHoe( new ResourceLocation( "minecraft", "diamond_hoe" ), 7, "upgrade.minecraft:diamond_hoe.adjective", Items.DIAMOND_HOE );
        registerTurtleUpgradeInternal( ComputerCraft.Upgrades.diamondHoe );

        ComputerCraft.Upgrades.advancedModem = new TurtleModem( true, new ResourceLocation( "computercraft", "advanced_modem" ) );
        registerTurtleUpgradeInternal( ComputerCraft.Upgrades.advancedModem );

        ComputerCraft.Upgrades.turtleSpeaker = new TurtleSpeaker( new ResourceLocation( "computercraft", "speaker" ), 8 );
        registerTurtleUpgradeInternal( ComputerCraft.Upgrades.turtleSpeaker );
    }

    private void registerTileEntities()
    {
        // TileEntities
        GameRegistry.registerTileEntity( TileTurtleNormal.class, ComputerCraft.LOWER_ID + ":turtle_normal" );
        GameRegistry.registerTileEntity( TileTurtleAdvanced.class, ComputerCraft.LOWER_ID + ":turtle_advanced" );
    }

    private void registerForgeHandlers()
    {
        ForgeHandlers handlers = new ForgeHandlers();
        MinecraftForge.EVENT_BUS.register( handlers );
    }

    private class ForgeHandlers
    {
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void onEntityLivingDrops( LivingDropsEvent event )
        {
            // Capture any mob drops for the current entity
            if( dropEntity != null && event.getEntity() == dropEntity.get() )
            {
                List<EntityItem> drops = event.getDrops();
                for( EntityItem entityItem : drops ) dropConsumer.accept( entityItem.getItem() );
                drops.clear();
            }
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void onHarvestDrops( BlockEvent.HarvestDropsEvent event )
        {
            // Capture block drops for the current entity
            if( dropWorld != null && dropWorld.get() == event.getWorld()
                && dropPos != null && dropPos.equals( event.getPos() ) )
            {
                for( ItemStack item : event.getDrops() )
                {
                    if( event.getWorld().rand.nextFloat() < event.getDropChance() ) dropConsumer.accept( item );
                }
                event.getDrops().clear();
            }
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void onEntitySpawn( EntityJoinWorldEvent event )
        {
            // Capture any nearby item spawns
            if( dropWorld != null && dropWorld.get() == event.getWorld() && event.getEntity() instanceof EntityItem
                && dropBounds.contains( event.getEntity().getPositionVector() ) )
            {
                dropConsumer.accept( ((EntityItem) event.getEntity()).getItem() );
                event.setCanceled( true );
            }
        }

        @SubscribeEvent
        public void onTurtleAction( TurtleActionEvent event) {
            if( ComputerCraft.turtleDisabledActions.contains( event.getAction() ) )
            {
                event.setCanceled( true, "Action has been disabled" );
            }
        }
    }

}
