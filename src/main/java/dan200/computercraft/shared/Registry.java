/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.shared.common.ColourableRecipe;
import dan200.computercraft.shared.computer.blocks.BlockComputer;
import dan200.computercraft.shared.computer.blocks.TileCommandComputer;
import dan200.computercraft.shared.computer.blocks.TileComputer;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.items.ItemComputer;
import dan200.computercraft.shared.computer.recipe.ComputerUpgradeRecipe;
import dan200.computercraft.shared.media.items.ItemDisk;
import dan200.computercraft.shared.media.items.ItemPrintout;
import dan200.computercraft.shared.media.items.ItemTreasureDisk;
import dan200.computercraft.shared.media.recipes.DiskRecipe;
import dan200.computercraft.shared.media.recipes.PrintoutRecipe;
import dan200.computercraft.shared.peripheral.diskdrive.BlockDiskDrive;
import dan200.computercraft.shared.peripheral.diskdrive.TileDiskDrive;
import dan200.computercraft.shared.peripheral.modem.wired.*;
import dan200.computercraft.shared.peripheral.modem.wireless.BlockWirelessModem;
import dan200.computercraft.shared.peripheral.modem.wireless.TileWirelessModem;
import dan200.computercraft.shared.peripheral.monitor.BlockMonitor;
import dan200.computercraft.shared.peripheral.monitor.TileMonitor;
import dan200.computercraft.shared.peripheral.printer.BlockPrinter;
import dan200.computercraft.shared.peripheral.printer.TilePrinter;
import dan200.computercraft.shared.peripheral.speaker.BlockSpeaker;
import dan200.computercraft.shared.peripheral.speaker.TileSpeaker;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import dan200.computercraft.shared.pocket.peripherals.PocketModem;
import dan200.computercraft.shared.pocket.peripherals.PocketSpeaker;
import dan200.computercraft.shared.pocket.recipes.PocketComputerUpgradeRecipe;
import dan200.computercraft.shared.turtle.blocks.BlockTurtle;
import dan200.computercraft.shared.turtle.blocks.TileTurtle;
import dan200.computercraft.shared.turtle.items.ItemTurtle;
import dan200.computercraft.shared.turtle.recipes.TurtleRecipe;
import dan200.computercraft.shared.turtle.recipes.TurtleUpgradeRecipe;
import dan200.computercraft.shared.turtle.upgrades.*;
import dan200.computercraft.shared.util.ImpostorRecipe;
import dan200.computercraft.shared.util.ImpostorShapelessRecipe;
import net.fabricmc.fabric.api.block.FabricBlockSettings;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.*;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.MutableRegistry;

public final class Registry
{
    private static final ItemGroup mainItemGroup = FabricItemGroupBuilder
        .create( new Identifier( ComputerCraft.MOD_ID, "main" ) )
        .icon( () -> new ItemStack( ComputerCraft.Items.computerNormal ) )
        .build();

    private Registry()
    {
    }

    public static void registerBlocks( MutableRegistry<Block> registry )
    {
        // Computers
        ComputerCraft.Blocks.computerNormal = new BlockComputer(
            FabricBlockSettings.of( Material.STONE ).hardness( 2.0f ).build(),
            ComputerFamily.Normal, TileComputer.FACTORY_NORMAL
        );

        ComputerCraft.Blocks.computerAdvanced = new BlockComputer(
            FabricBlockSettings.of( Material.STONE ).hardness( 2.0f ).build(),
            ComputerFamily.Advanced, TileComputer.FACTORY_ADVANCED
        );

        ComputerCraft.Blocks.computerCommand = new BlockComputer(
            FabricBlockSettings.of( Material.STONE ).strength( -1, 6000000.0F ).build(),
            ComputerFamily.Command, TileCommandComputer.FACTORY
        );

        registry.add( new Identifier( ComputerCraft.MOD_ID, "computer_normal" ), ComputerCraft.Blocks.computerNormal );
        registry.add( new Identifier( ComputerCraft.MOD_ID, "computer_advanced" ), ComputerCraft.Blocks.computerAdvanced );
        registry.add( new Identifier( ComputerCraft.MOD_ID, "computer_command" ), ComputerCraft.Blocks.computerCommand );

        // Turtles
        ComputerCraft.Blocks.turtleNormal = new BlockTurtle(
            FabricBlockSettings.of( Material.STONE ).hardness( 2.5f ).build(),
            ComputerFamily.Normal, TileTurtle.FACTORY_NORMAL
        );

        ComputerCraft.Blocks.turtleAdvanced = new BlockTurtle(
            FabricBlockSettings.of( Material.STONE ).hardness( 2.5f ).build(),
            ComputerFamily.Advanced, TileTurtle.FACTORY_ADVANCED
        );

        registry.add( new Identifier( ComputerCraft.MOD_ID, "turtle_normal" ), ComputerCraft.Blocks.turtleNormal );
        registry.add( new Identifier( ComputerCraft.MOD_ID, "turtle_advanced" ), ComputerCraft.Blocks.turtleAdvanced );

        // Peripherals
        ComputerCraft.Blocks.speaker = new BlockSpeaker(
            FabricBlockSettings.of( Material.STONE ).hardness( 2 ).build()
        );

        ComputerCraft.Blocks.diskDrive = new BlockDiskDrive(
            FabricBlockSettings.of( Material.STONE ).hardness( 2 ).build()
        );

        ComputerCraft.Blocks.monitorNormal = new BlockMonitor(
            FabricBlockSettings.of( Material.STONE ).hardness( 2 ).build(),
            TileMonitor.FACTORY_NORMAL
        );

        ComputerCraft.Blocks.monitorAdvanced = new BlockMonitor(
            FabricBlockSettings.of( Material.STONE ).hardness( 2 ).build(),
            TileMonitor.FACTORY_ADVANCED
        );

        ComputerCraft.Blocks.printer = new BlockPrinter(
            FabricBlockSettings.of( Material.STONE ).hardness( 2 ).build()
        );

        ComputerCraft.Blocks.wirelessModemNormal = new BlockWirelessModem(
            FabricBlockSettings.of( Material.STONE ).hardness( 2 ).build(),
            TileWirelessModem.FACTORY_NORMAL
        );

        ComputerCraft.Blocks.wirelessModemAdvanced = new BlockWirelessModem(
            FabricBlockSettings.of( Material.STONE ).hardness( 2 ).build(),
            TileWirelessModem.FACTORY_ADVANCED
        );

        ComputerCraft.Blocks.wiredModemFull = new BlockWiredModemFull(
            FabricBlockSettings.of( Material.STONE ).hardness( 1.5f ).build()
        );

        ComputerCraft.Blocks.cable = new BlockCable(
            FabricBlockSettings.of( Material.STONE ).hardness( 1.5f ).build()
        );

        registry.add( new Identifier( ComputerCraft.MOD_ID, "speaker" ), ComputerCraft.Blocks.speaker );
        registry.add( new Identifier( ComputerCraft.MOD_ID, "disk_drive" ), ComputerCraft.Blocks.diskDrive );
        registry.add( new Identifier( ComputerCraft.MOD_ID, "monitor_normal" ), ComputerCraft.Blocks.monitorNormal );
        registry.add( new Identifier( ComputerCraft.MOD_ID, "monitor_advanced" ), ComputerCraft.Blocks.monitorAdvanced );
        registry.add( new Identifier( ComputerCraft.MOD_ID, "printer" ), ComputerCraft.Blocks.printer );
        registry.add( new Identifier( ComputerCraft.MOD_ID, "wireless_modem_normal" ), ComputerCraft.Blocks.wirelessModemNormal );
        registry.add( new Identifier( ComputerCraft.MOD_ID, "wireless_modem_advanced" ), ComputerCraft.Blocks.wirelessModemAdvanced );
        registry.add( new Identifier( ComputerCraft.MOD_ID, "wired_modem_full" ), ComputerCraft.Blocks.wiredModemFull );
        registry.add( new Identifier( ComputerCraft.MOD_ID, "cable" ), ComputerCraft.Blocks.cable );
    }

    public static void registerTileEntities( MutableRegistry<BlockEntityType<?>> registry )
    {
        // Computers
        registry.add( TileComputer.FACTORY_NORMAL.getId(), TileComputer.FACTORY_NORMAL );
        registry.add( TileComputer.FACTORY_ADVANCED.getId(), TileComputer.FACTORY_ADVANCED );
        registry.add( TileCommandComputer.FACTORY.getId(), TileCommandComputer.FACTORY );

        // Turtles
        registry.add( TileTurtle.FACTORY_NORMAL.getId(), TileTurtle.FACTORY_NORMAL );
        registry.add( TileTurtle.FACTORY_ADVANCED.getId(), TileTurtle.FACTORY_ADVANCED );

        // Peripherals
        registry.add( TileSpeaker.FACTORY.getId(), TileSpeaker.FACTORY );
        registry.add( TileDiskDrive.FACTORY.getId(), TileDiskDrive.FACTORY );
        registry.add( TilePrinter.FACTORY.getId(), TilePrinter.FACTORY );

        registry.add( TileMonitor.FACTORY_NORMAL.getId(), TileMonitor.FACTORY_NORMAL );
        registry.add( TileMonitor.FACTORY_ADVANCED.getId(), TileMonitor.FACTORY_ADVANCED );

        registry.add( TileWirelessModem.FACTORY_NORMAL.getId(), TileWirelessModem.FACTORY_NORMAL );
        registry.add( TileWirelessModem.FACTORY_ADVANCED.getId(), TileWirelessModem.FACTORY_ADVANCED );
        registry.add( TileCable.FACTORY.getId(), TileCable.FACTORY );
        registry.add( TileWiredModemFull.FACTORY.getId(), TileWiredModemFull.FACTORY );
    }

    private static void registerItemBlock( MutableRegistry<Item> registry, BlockItem item )
    {
        registry.add( net.minecraft.util.registry.Registry.BLOCK.getId( item.getBlock() ), item );
    }

    private static Item.Settings defaultItem()
    {
        return new Item.Settings().itemGroup( mainItemGroup );
    }

    public static void registerItems( MutableRegistry<Item> registry )
    {
        // Computer
        ComputerCraft.Items.computerNormal = new ItemComputer( ComputerCraft.Blocks.computerNormal, defaultItem() );
        ComputerCraft.Items.computerAdvanced = new ItemComputer( ComputerCraft.Blocks.computerAdvanced, defaultItem() );
        ComputerCraft.Items.computerCommand = new ItemComputer( ComputerCraft.Blocks.computerCommand, defaultItem() );

        registerItemBlock( registry, ComputerCraft.Items.computerNormal );
        registerItemBlock( registry, ComputerCraft.Items.computerAdvanced );
        registerItemBlock( registry, ComputerCraft.Items.computerCommand );

        // Turtle
        ComputerCraft.Items.turtleNormal = new ItemTurtle( ComputerCraft.Blocks.turtleNormal, defaultItem() );
        ComputerCraft.Items.turtleAdvanced = new ItemTurtle( ComputerCraft.Blocks.turtleAdvanced, defaultItem() );

        registerItemBlock( registry, ComputerCraft.Items.turtleNormal );
        registerItemBlock( registry, ComputerCraft.Items.turtleAdvanced );

        // Pocket computer
        ComputerCraft.Items.pocketComputerNormal = new ItemPocketComputer( defaultItem().stackSize( 1 ), ComputerFamily.Normal );
        ComputerCraft.Items.pocketComputerAdvanced = new ItemPocketComputer( defaultItem().stackSize( 1 ), ComputerFamily.Advanced );

        registry.add( new Identifier( ComputerCraft.MOD_ID, "pocket_computer_normal" ), ComputerCraft.Items.pocketComputerNormal );
        registry.add( new Identifier( ComputerCraft.MOD_ID, "pocket_computer_advanced" ), ComputerCraft.Items.pocketComputerAdvanced );

        // Floppy disk
        ComputerCraft.Items.disk = new ItemDisk( defaultItem().stackSize( 1 ) );
        ComputerCraft.Items.treasureDisk = new ItemTreasureDisk( defaultItem().stackSize( 1 ) );

        registry.add( new Identifier( ComputerCraft.MOD_ID, "disk" ), ComputerCraft.Items.disk );
        registry.add( new Identifier( ComputerCraft.MOD_ID, "treasure_disk" ), ComputerCraft.Items.treasureDisk );

        // Printouts
        ComputerCraft.Items.printedPage = new ItemPrintout( defaultItem().stackSize( 1 ), ItemPrintout.Type.PAGE );
        ComputerCraft.Items.printedPages = new ItemPrintout( defaultItem().stackSize( 1 ), ItemPrintout.Type.PAGES );
        ComputerCraft.Items.printedBook = new ItemPrintout( defaultItem().stackSize( 1 ), ItemPrintout.Type.BOOK );

        registry.add( new Identifier( ComputerCraft.MOD_ID, "printed_page" ), ComputerCraft.Items.printedPage );
        registry.add( new Identifier( ComputerCraft.MOD_ID, "printed_pages" ), ComputerCraft.Items.printedPages );
        registry.add( new Identifier( ComputerCraft.MOD_ID, "printed_book" ), ComputerCraft.Items.printedBook );

        // Peripherals
        registerItemBlock( registry, new BlockItem( ComputerCraft.Blocks.speaker, defaultItem() ) );
        registerItemBlock( registry, new BlockItem( ComputerCraft.Blocks.diskDrive, defaultItem() ) );
        registerItemBlock( registry, new BlockItem( ComputerCraft.Blocks.printer, defaultItem() ) );
        registerItemBlock( registry, new BlockItem( ComputerCraft.Blocks.monitorNormal, defaultItem() ) );
        registerItemBlock( registry, new BlockItem( ComputerCraft.Blocks.monitorAdvanced, defaultItem() ) );
        registerItemBlock( registry, new BlockItem( ComputerCraft.Blocks.wirelessModemNormal, defaultItem() ) );
        registerItemBlock( registry, new BlockItem( ComputerCraft.Blocks.wirelessModemAdvanced, defaultItem() ) );
        registerItemBlock( registry, new BlockItem( ComputerCraft.Blocks.wiredModemFull, defaultItem() ) );

        ComputerCraft.Items.cable = new ItemBlockCable.Cable( ComputerCraft.Blocks.cable, defaultItem() );
        ComputerCraft.Items.wiredModem = new ItemBlockCable.WiredModem( ComputerCraft.Blocks.cable, defaultItem() );

        registry.add( new Identifier( ComputerCraft.MOD_ID, "cable" ), ComputerCraft.Items.cable );
        registry.add( new Identifier( ComputerCraft.MOD_ID, "wired_modem" ), ComputerCraft.Items.wiredModem );

        registerTurtleUpgrades();
        registerPocketUpgrades();
    }

    private static void registerTurtleUpgrades()
    {
        // Upgrades
        ComputerCraft.TurtleUpgrades.wirelessModemNormal = new TurtleModem( false, new Identifier( ComputerCraft.MOD_ID, "wireless_modem_normal" ) );
        ComputerCraftAPI.registerTurtleUpgrade( ComputerCraft.TurtleUpgrades.wirelessModemNormal );

        ComputerCraft.TurtleUpgrades.wirelessModemAdvanced = new TurtleModem( true, new Identifier( ComputerCraft.MOD_ID, "wireless_modem_advanced" ) );
        ComputerCraftAPI.registerTurtleUpgrade( ComputerCraft.TurtleUpgrades.wirelessModemAdvanced );

        ComputerCraft.TurtleUpgrades.speaker = new TurtleSpeaker( new Identifier( ComputerCraft.MOD_ID, "speaker" ) );
        ComputerCraftAPI.registerTurtleUpgrade( ComputerCraft.TurtleUpgrades.speaker );

        ComputerCraft.TurtleUpgrades.craftingTable = new TurtleCraftingTable( new Identifier( "minecraft", "crafting_table" ) );
        ComputerCraftAPI.registerTurtleUpgrade( ComputerCraft.TurtleUpgrades.craftingTable );

        ComputerCraft.TurtleUpgrades.diamondSword = new TurtleSword( new Identifier( "minecraft", "diamond_sword" ), Items.DIAMOND_SWORD );
        ComputerCraftAPI.registerTurtleUpgrade( ComputerCraft.TurtleUpgrades.diamondSword );

        ComputerCraft.TurtleUpgrades.diamondShovel = new TurtleShovel( new Identifier( "minecraft", "diamond_shovel" ), Items.DIAMOND_SHOVEL );
        ComputerCraftAPI.registerTurtleUpgrade( ComputerCraft.TurtleUpgrades.diamondShovel );

        ComputerCraft.TurtleUpgrades.diamondPickaxe = new TurtleTool( new Identifier( "minecraft", "diamond_pickaxe" ), Items.DIAMOND_PICKAXE );
        ComputerCraftAPI.registerTurtleUpgrade( ComputerCraft.TurtleUpgrades.diamondPickaxe );

        ComputerCraft.TurtleUpgrades.diamondAxe = new TurtleAxe( new Identifier( "minecraft", "diamond_axe" ), Items.DIAMOND_AXE );
        ComputerCraftAPI.registerTurtleUpgrade( ComputerCraft.TurtleUpgrades.diamondAxe );

        ComputerCraft.TurtleUpgrades.diamondHoe = new TurtleHoe( new Identifier( "minecraft", "diamond_hoe" ), Items.DIAMOND_HOE );
        ComputerCraftAPI.registerTurtleUpgrade( ComputerCraft.TurtleUpgrades.diamondHoe );
    }

    private static void registerPocketUpgrades()
    {
        ComputerCraftAPI.registerPocketUpgrade( ComputerCraft.PocketUpgrades.wirelessModemNormal = new PocketModem( false ) );
        ComputerCraftAPI.registerPocketUpgrade( ComputerCraft.PocketUpgrades.wirelessModemAdvanced = new PocketModem( true ) );
        ComputerCraftAPI.registerPocketUpgrade( ComputerCraft.PocketUpgrades.speaker = new PocketSpeaker() );
    }

    public static void registerRecipes( MutableRegistry<RecipeSerializer<?>> registry )
    {
        registry.add( new Identifier( ComputerCraft.MOD_ID, "colour" ), ColourableRecipe.SERIALIZER );
        registry.add( new Identifier( ComputerCraft.MOD_ID, "computer_upgrade" ), ComputerUpgradeRecipe.SERIALIZER );
        registry.add( new Identifier( ComputerCraft.MOD_ID, "pocket_computer_upgrade" ), PocketComputerUpgradeRecipe.SERIALIZER );
        registry.add( new Identifier( ComputerCraft.MOD_ID, "disk" ), DiskRecipe.SERIALIZER );
        registry.add( new Identifier( ComputerCraft.MOD_ID, "printout" ), PrintoutRecipe.SERIALIZER );
        registry.add( new Identifier( ComputerCraft.MOD_ID, "turtle" ), TurtleRecipe.SERIALIZER );
        registry.add( new Identifier( ComputerCraft.MOD_ID, "turtle_upgrade" ), TurtleUpgradeRecipe.SERIALIZER );
        registry.add( new Identifier( ComputerCraft.MOD_ID, "impostor_shaped" ), ImpostorRecipe.SERIALIZER );
        registry.add( new Identifier( ComputerCraft.MOD_ID, "impostor_shapeless" ), ImpostorShapelessRecipe.SERIALIZER );
    }
}
