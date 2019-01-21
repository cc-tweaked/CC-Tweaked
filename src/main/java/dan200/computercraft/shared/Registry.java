/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.shared.computer.blocks.BlockCommandComputer;
import dan200.computercraft.shared.computer.blocks.BlockComputer;
import dan200.computercraft.shared.computer.blocks.TileCommandComputer;
import dan200.computercraft.shared.computer.blocks.TileComputer;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.items.ItemCommandComputer;
import dan200.computercraft.shared.computer.items.ItemComputer;
import dan200.computercraft.shared.media.items.ItemDiskExpanded;
import dan200.computercraft.shared.media.items.ItemDiskLegacy;
import dan200.computercraft.shared.media.items.ItemPrintout;
import dan200.computercraft.shared.media.items.ItemTreasureDisk;
import dan200.computercraft.shared.peripheral.common.BlockPeripheral;
import dan200.computercraft.shared.peripheral.common.ItemPeripheral;
import dan200.computercraft.shared.peripheral.diskdrive.TileDiskDrive;
import dan200.computercraft.shared.peripheral.modem.wired.*;
import dan200.computercraft.shared.peripheral.modem.wireless.BlockAdvancedModem;
import dan200.computercraft.shared.peripheral.modem.wireless.ItemAdvancedModem;
import dan200.computercraft.shared.peripheral.modem.wireless.TileAdvancedModem;
import dan200.computercraft.shared.peripheral.modem.wireless.TileWirelessModem;
import dan200.computercraft.shared.peripheral.monitor.TileMonitor;
import dan200.computercraft.shared.peripheral.printer.TilePrinter;
import dan200.computercraft.shared.peripheral.speaker.TileSpeaker;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import dan200.computercraft.shared.pocket.items.PocketComputerItemFactory;
import dan200.computercraft.shared.pocket.peripherals.PocketModem;
import dan200.computercraft.shared.pocket.peripherals.PocketSpeaker;
import dan200.computercraft.shared.turtle.blocks.BlockTurtle;
import dan200.computercraft.shared.turtle.blocks.TileTurtle;
import dan200.computercraft.shared.turtle.blocks.TileTurtleAdvanced;
import dan200.computercraft.shared.turtle.blocks.TileTurtleExpanded;
import dan200.computercraft.shared.turtle.items.ItemTurtleAdvanced;
import dan200.computercraft.shared.turtle.items.ItemTurtleLegacy;
import dan200.computercraft.shared.turtle.items.ItemTurtleNormal;
import dan200.computercraft.shared.turtle.items.TurtleItemFactory;
import dan200.computercraft.shared.turtle.upgrades.*;
import dan200.computercraft.shared.util.Colour;
import dan200.computercraft.shared.util.ImpostorRecipe;
import dan200.computercraft.shared.util.ImpostorShapelessRecipe;
import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.IForgeRegistry;

@Mod.EventBusSubscriber( modid = ComputerCraft.MOD_ID )
public final class Registry
{
    private Registry()
    {
    }

    @SubscribeEvent
    public static void registerBlocks( RegistryEvent.Register<Block> event )
    {
        IForgeRegistry<Block> registry = event.getRegistry();

        // Computers
        ComputerCraft.Blocks.computer = new BlockComputer();
        ComputerCraft.Blocks.commandComputer = new BlockCommandComputer();

        registry.registerAll(
            ComputerCraft.Blocks.computer.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "computer" ) ),
            ComputerCraft.Blocks.commandComputer.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "command_computer" ) )
        );

        // Turtle
        ComputerCraft.Blocks.turtle = new BlockTurtle();
        ComputerCraft.Blocks.turtleExpanded = new BlockTurtle();
        ComputerCraft.Blocks.turtleAdvanced = new BlockTurtle();

        registry.registerAll(
            ComputerCraft.Blocks.turtle.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "turtle" ) ),
            ComputerCraft.Blocks.turtleExpanded.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "turtle_expanded" ) ),
            ComputerCraft.Blocks.turtleAdvanced.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "turtle_advanced" ) )
        );

        // Peripheral
        ComputerCraft.Blocks.peripheral = new BlockPeripheral();
        registry.register( ComputerCraft.Blocks.peripheral.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "peripheral" ) ) );

        // Cable
        ComputerCraft.Blocks.cable = new BlockCable();
        registry.register( ComputerCraft.Blocks.cable.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "cable" ) ) );

        // Advanced modem
        ComputerCraft.Blocks.advancedModem = new BlockAdvancedModem();
        registry.register( ComputerCraft.Blocks.advancedModem.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "advanced_modem" ) ) );

        // Full block modem
        ComputerCraft.Blocks.wiredModemFull = new BlockWiredModemFull();
        registry.register( ComputerCraft.Blocks.wiredModemFull.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "wired_modem_full" ) ) );

        registerTileEntities();
    }

    private static void registerTileEntities()
    {
        GameRegistry.registerTileEntity( TileComputer.class, new ResourceLocation( ComputerCraft.MOD_ID, "computer" ) );
        GameRegistry.registerTileEntity( TileCommandComputer.class, new ResourceLocation( ComputerCraft.MOD_ID, "command_computer" ) );

        GameRegistry.registerTileEntity( TileTurtle.class, new ResourceLocation( ComputerCraft.MOD_ID, "turtle" ) );
        GameRegistry.registerTileEntity( TileTurtleExpanded.class, new ResourceLocation( ComputerCraft.MOD_ID, "turtleex" ) );
        GameRegistry.registerTileEntity( TileTurtleAdvanced.class, new ResourceLocation( ComputerCraft.MOD_ID, "turtleadv" ) );

        GameRegistry.registerTileEntity( TileDiskDrive.class, new ResourceLocation( ComputerCraft.MOD_ID, "diskdrive" ) );
        GameRegistry.registerTileEntity( TileWirelessModem.class, new ResourceLocation( ComputerCraft.MOD_ID, "wirelessmodem" ) );
        GameRegistry.registerTileEntity( TileMonitor.class, new ResourceLocation( ComputerCraft.MOD_ID, "monitor" ) );
        GameRegistry.registerTileEntity( TilePrinter.class, new ResourceLocation( ComputerCraft.MOD_ID, "ccprinter" ) );
        GameRegistry.registerTileEntity( TileCable.class, new ResourceLocation( ComputerCraft.MOD_ID, "wiredmodem" ) );
        GameRegistry.registerTileEntity( TileAdvancedModem.class, new ResourceLocation( ComputerCraft.MOD_ID, "advanced_modem" ) );
        GameRegistry.registerTileEntity( TileSpeaker.class, new ResourceLocation( ComputerCraft.MOD_ID, "speaker" ) );
        GameRegistry.registerTileEntity( TileWiredModemFull.class, new ResourceLocation( ComputerCraft.MOD_ID, "wired_modem_full" ) );
    }

    private static <T extends ItemBlock> T setupItemBlock( T item )
    {
        item.setRegistryName( item.getBlock().getRegistryName() );
        return item;
    }

    @SubscribeEvent
    public static void registerItems( RegistryEvent.Register<Item> event )
    {
        IForgeRegistry<Item> registry = event.getRegistry();

        // Computers
        ComputerCraft.Items.computer = new ItemComputer( ComputerCraft.Blocks.computer );
        ComputerCraft.Items.commandComputer = new ItemCommandComputer( ComputerCraft.Blocks.commandComputer );

        registry.registerAll(
            setupItemBlock( ComputerCraft.Items.computer ),
            setupItemBlock( ComputerCraft.Items.commandComputer )
        );

        // Pocket computer
        ComputerCraft.Items.pocketComputer = new ItemPocketComputer();
        registry.register(
            ComputerCraft.Items.pocketComputer.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "pocket_computer" ) )
        );

        // Turtle
        ComputerCraft.Items.turtle = new ItemTurtleLegacy( ComputerCraft.Blocks.turtle );
        ComputerCraft.Items.turtleExpanded = new ItemTurtleNormal( ComputerCraft.Blocks.turtleExpanded );
        ComputerCraft.Items.turtleAdvanced = new ItemTurtleAdvanced( ComputerCraft.Blocks.turtleAdvanced );

        registry.registerAll(
            setupItemBlock( ComputerCraft.Items.turtle ),
            setupItemBlock( ComputerCraft.Items.turtleExpanded ),
            setupItemBlock( ComputerCraft.Items.turtleAdvanced )
        );

        // Printouts
        ComputerCraft.Items.printout = new ItemPrintout();
        registry.register( ComputerCraft.Items.printout.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "printout" ) ) );

        // Disks
        ComputerCraft.Items.disk = new ItemDiskLegacy();
        ComputerCraft.Items.diskExpanded = new ItemDiskExpanded();
        ComputerCraft.Items.treasureDisk = new ItemTreasureDisk();

        registry.registerAll(
            ComputerCraft.Items.disk.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "disk" ) ),
            ComputerCraft.Items.diskExpanded.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "disk_expanded" ) ),
            ComputerCraft.Items.treasureDisk.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "treasure_disk" ) )
        );

        // Peripherals
        ComputerCraft.Items.peripheral = new ItemPeripheral( ComputerCraft.Blocks.peripheral );
        ComputerCraft.Items.advancedModem = new ItemAdvancedModem( ComputerCraft.Blocks.advancedModem );
        ComputerCraft.Items.cable = new ItemCable( ComputerCraft.Blocks.cable );
        ComputerCraft.Items.wiredModemFull = new ItemWiredModemFull( ComputerCraft.Blocks.wiredModemFull );

        registry.registerAll(
            setupItemBlock( ComputerCraft.Items.peripheral ),
            setupItemBlock( ComputerCraft.Items.advancedModem ),
            setupItemBlock( ComputerCraft.Items.cable ),
            setupItemBlock( ComputerCraft.Items.wiredModemFull )
        );

        registerTurtleUpgrades();
        registerPocketUpgrades();
        registerLegacyUpgrades();
    }

    @SubscribeEvent
    public static void registerRecipes( RegistryEvent.Register<IRecipe> event )
    {
        IForgeRegistry<IRecipe> registry = event.getRegistry();

        // Register fake recipes for the recipe book and JEI. We have several dynamic recipes,
        // and we'd like people to be able to see them.

        // Turtle upgrades
        // TODO: Figure out a way to do this in a "nice" way.
        for( ITurtleUpgrade upgrade : TurtleUpgrades.getVanillaUpgrades() )
        {
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
                }
            }
        }

        // Coloured disks
        ItemStack paper = new ItemStack( Items.PAPER, 1 );
        ItemStack redstone = new ItemStack( Items.REDSTONE, 1 );
        for( int colour = 0; colour < 16; colour++ )
        {
            ItemStack disk = ItemDiskLegacy.createFromIDAndColour( -1, null, Colour.values()[colour].getHex() );
            ItemStack dye = new ItemStack( Items.DYE, 1, colour );

            int diskIdx = 0;
            ItemStack[] disks = new ItemStack[15];
            for( int otherColour = 0; otherColour < 16; otherColour++ )
            {
                if( colour != otherColour )
                {
                    disks[diskIdx++] = ItemDiskLegacy.createFromIDAndColour( -1, null, Colour.values()[otherColour].getHex() );
                }
            }

            // Normal recipe
            registry.register(
                new ImpostorShapelessRecipe( "computercraft:disk", disk, new ItemStack[] { redstone, paper, dye } )
                    .setRegistryName( new ResourceLocation( "computercraft:disk_imposter_" + colour ) )
            );

            // Conversion recipe
            registry.register(
                new ImpostorShapelessRecipe( "computercraft:disk", disk, NonNullList.from( Ingredient.EMPTY, Ingredient.fromStacks( disks ), Ingredient.fromStacks( dye ) ) )
                    .setRegistryName( new ResourceLocation( "computercraft:disk_imposter_convert_" + colour ) )
            );
        }

        // Pocket computer upgrades
        ItemStack pocketComputer = PocketComputerItemFactory.create( -1, null, -1, ComputerFamily.Normal, null );
        ItemStack advancedPocketComputer = PocketComputerItemFactory.create( -1, null, -1, ComputerFamily.Advanced, null );
        for( IPocketUpgrade upgrade : PocketUpgrades.getVanillaUpgrades() )
        {
            registry.register( new ImpostorRecipe(
                    "computercraft:normal_pocket_upgrade",
                    1, 2,
                    new ItemStack[] { upgrade.getCraftingItem(), pocketComputer },
                    PocketComputerItemFactory.create( -1, null, -1, ComputerFamily.Normal, upgrade )
                ).setRegistryName( new ResourceLocation( "computercraft:normal_pocket_upgrade_" + upgrade.getUpgradeID().toString().replace( ':', '_' ) ) )
            );

            registry.register(
                new ImpostorRecipe( "computercraft:advanced_pocket_upgrade",
                    1, 2,
                    new ItemStack[] { upgrade.getCraftingItem(), advancedPocketComputer },
                    PocketComputerItemFactory.create( -1, null, -1, ComputerFamily.Advanced, upgrade )
                ).setRegistryName( new ResourceLocation( "computercraft:advanced_pocket_upgrade_" + upgrade.getUpgradeID().toString().replace( ':', '_' ) ) )
            );
        }
    }


    public static void registerTurtleUpgrades()
    {
        // Upgrades
        ComputerCraft.TurtleUpgrades.wirelessModem = new TurtleModem( false, new ResourceLocation( "computercraft", "wireless_modem" ), 1 );
        TurtleUpgrades.registerInternal( ComputerCraft.TurtleUpgrades.wirelessModem );

        ComputerCraft.TurtleUpgrades.advancedModem = new TurtleModem( true, new ResourceLocation( "computercraft", "advanced_modem" ), -1 );
        TurtleUpgrades.registerInternal( ComputerCraft.TurtleUpgrades.advancedModem );

        ComputerCraft.TurtleUpgrades.speaker = new TurtleSpeaker( new ResourceLocation( "computercraft", "speaker" ), 8 );
        TurtleUpgrades.registerInternal( ComputerCraft.TurtleUpgrades.speaker );

        ComputerCraft.TurtleUpgrades.craftingTable = new TurtleCraftingTable( new ResourceLocation( "minecraft", "crafting_table" ), 2 );
        TurtleUpgrades.registerInternal( ComputerCraft.TurtleUpgrades.craftingTable );

        ComputerCraft.TurtleUpgrades.diamondSword = new TurtleSword( new ResourceLocation( "minecraft", "diamond_sword" ), 3, Items.DIAMOND_SWORD );
        TurtleUpgrades.registerInternal( ComputerCraft.TurtleUpgrades.diamondSword );

        ComputerCraft.TurtleUpgrades.diamondShovel = new TurtleShovel( new ResourceLocation( "minecraft", "diamond_shovel" ), 4, Items.DIAMOND_SHOVEL );
        TurtleUpgrades.registerInternal( ComputerCraft.TurtleUpgrades.diamondShovel );

        ComputerCraft.TurtleUpgrades.diamondPickaxe = new TurtleTool( new ResourceLocation( "minecraft", "diamond_pickaxe" ), 5, Items.DIAMOND_PICKAXE );
        TurtleUpgrades.registerInternal( ComputerCraft.TurtleUpgrades.diamondPickaxe );

        ComputerCraft.TurtleUpgrades.diamondAxe = new TurtleAxe( new ResourceLocation( "minecraft", "diamond_axe" ), 6, Items.DIAMOND_AXE );
        TurtleUpgrades.registerInternal( ComputerCraft.TurtleUpgrades.diamondAxe );

        ComputerCraft.TurtleUpgrades.diamondHoe = new TurtleHoe( new ResourceLocation( "minecraft", "diamond_hoe" ), 7, Items.DIAMOND_HOE );
        TurtleUpgrades.registerInternal( ComputerCraft.TurtleUpgrades.diamondHoe );
    }

    public static void registerPocketUpgrades()
    {
        // Register pocket upgrades
        ComputerCraft.PocketUpgrades.wirelessModem = new PocketModem( false );
        ComputerCraftAPI.registerPocketUpgrade( ComputerCraft.PocketUpgrades.wirelessModem );
        ComputerCraft.PocketUpgrades.advancedModem = new PocketModem( true );
        ComputerCraftAPI.registerPocketUpgrade( ComputerCraft.PocketUpgrades.advancedModem );

        ComputerCraft.PocketUpgrades.speaker = new PocketSpeaker();
        ComputerCraftAPI.registerPocketUpgrade( ComputerCraft.PocketUpgrades.speaker );
    }

    @SuppressWarnings( "deprecation" )
    private static void registerLegacyUpgrades()
    {
        ComputerCraft.PocketUpgrades.pocketSpeaker = ComputerCraft.PocketUpgrades.speaker;
        ComputerCraft.Upgrades.advancedModem = ComputerCraft.TurtleUpgrades.advancedModem;
    }

    @SubscribeEvent
    public static void remapItems( RegistryEvent.MissingMappings<Item> mappings )
    {
        // We have to use mappings.getAllMappings() as the mod ID is upper case but the domain lower.
        for( RegistryEvent.MissingMappings.Mapping<Item> mapping : mappings.getAllMappings() )
        {
            String domain = mapping.key.getNamespace();
            if( !domain.equalsIgnoreCase( ComputerCraft.MOD_ID ) ) continue;

            String key = mapping.key.getPath();
            if( key.equalsIgnoreCase( "CC-Computer" ) )
            {
                mapping.remap( ComputerCraft.Items.computer );
            }
            else if( key.equalsIgnoreCase( "CC-Peripheral" ) )
            {
                mapping.remap( ComputerCraft.Items.peripheral );
            }
            else if( key.equalsIgnoreCase( "CC-Cable" ) )
            {
                mapping.remap( ComputerCraft.Items.cable );
            }
            else if( key.equalsIgnoreCase( "diskExpanded" ) )
            {
                mapping.remap( ComputerCraft.Items.diskExpanded );
            }
            else if( key.equalsIgnoreCase( "treasureDisk" ) )
            {
                mapping.remap( ComputerCraft.Items.treasureDisk );
            }
            else if( key.equalsIgnoreCase( "pocketComputer" ) )
            {
                mapping.remap( ComputerCraft.Items.pocketComputer );
            }
            else if( key.equalsIgnoreCase( "CC-Turtle" ) )
            {
                mapping.remap( ComputerCraft.Items.turtle );
            }
            else if( key.equalsIgnoreCase( "CC-TurtleExpanded" ) )
            {
                mapping.remap( ComputerCraft.Items.turtleExpanded );
            }
            else if( key.equalsIgnoreCase( "CC-TurtleAdvanced" ) )
            {
                mapping.remap( ComputerCraft.Items.turtleAdvanced );
            }
        }
    }

    @SubscribeEvent
    public static void remapBlocks( RegistryEvent.MissingMappings<Block> mappings )
    {
        // We have to use mappings.getAllMappings() as the mod ID is upper case but the domain lower.
        for( RegistryEvent.MissingMappings.Mapping<Block> mapping : mappings.getAllMappings() )
        {
            String domain = mapping.key.getNamespace();
            if( !domain.equalsIgnoreCase( ComputerCraft.MOD_ID ) ) continue;

            String key = mapping.key.getPath();
            if( key.equalsIgnoreCase( "CC-Computer" ) )
            {
                mapping.remap( ComputerCraft.Blocks.computer );
            }
            else if( key.equalsIgnoreCase( "CC-Peripheral" ) )
            {
                mapping.remap( ComputerCraft.Blocks.peripheral );
            }
            else if( key.equalsIgnoreCase( "CC-Cable" ) )
            {
                mapping.remap( ComputerCraft.Blocks.cable );
            }
            else if( key.equalsIgnoreCase( "CC-Turtle" ) )
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
}
