/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.shared.common.ColourableRecipe;
import dan200.computercraft.shared.common.ContainerHeldItem;
import dan200.computercraft.shared.computer.blocks.BlockComputer;
import dan200.computercraft.shared.computer.blocks.TileCommandComputer;
import dan200.computercraft.shared.computer.blocks.TileComputer;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.inventory.ContainerComputer;
import dan200.computercraft.shared.computer.inventory.ContainerViewComputer;
import dan200.computercraft.shared.computer.items.ItemComputer;
import dan200.computercraft.shared.computer.recipe.ComputerUpgradeRecipe;
import dan200.computercraft.shared.media.items.ItemDisk;
import dan200.computercraft.shared.media.items.ItemPrintout;
import dan200.computercraft.shared.media.items.ItemTreasureDisk;
import dan200.computercraft.shared.media.recipes.DiskRecipe;
import dan200.computercraft.shared.media.recipes.PrintoutRecipe;
import dan200.computercraft.shared.peripheral.diskdrive.BlockDiskDrive;
import dan200.computercraft.shared.peripheral.diskdrive.ContainerDiskDrive;
import dan200.computercraft.shared.peripheral.diskdrive.TileDiskDrive;
import dan200.computercraft.shared.peripheral.modem.wired.*;
import dan200.computercraft.shared.peripheral.modem.wireless.BlockWirelessModem;
import dan200.computercraft.shared.peripheral.modem.wireless.TileWirelessModem;
import dan200.computercraft.shared.peripheral.monitor.BlockMonitor;
import dan200.computercraft.shared.peripheral.monitor.TileMonitor;
import dan200.computercraft.shared.peripheral.printer.BlockPrinter;
import dan200.computercraft.shared.peripheral.printer.ContainerPrinter;
import dan200.computercraft.shared.peripheral.printer.TilePrinter;
import dan200.computercraft.shared.peripheral.speaker.BlockSpeaker;
import dan200.computercraft.shared.peripheral.speaker.TileSpeaker;
import dan200.computercraft.shared.pocket.inventory.ContainerPocketComputer;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import dan200.computercraft.shared.pocket.peripherals.PocketModem;
import dan200.computercraft.shared.pocket.peripherals.PocketSpeaker;
import dan200.computercraft.shared.pocket.recipes.PocketComputerUpgradeRecipe;
import dan200.computercraft.shared.turtle.blocks.BlockTurtle;
import dan200.computercraft.shared.turtle.blocks.TileTurtle;
import dan200.computercraft.shared.turtle.core.TurtlePlayer;
import dan200.computercraft.shared.turtle.inventory.ContainerTurtle;
import dan200.computercraft.shared.turtle.items.ItemTurtle;
import dan200.computercraft.shared.turtle.recipes.TurtleRecipe;
import dan200.computercraft.shared.turtle.recipes.TurtleUpgradeRecipe;
import dan200.computercraft.shared.turtle.upgrades.*;
import dan200.computercraft.shared.util.CreativeTabMain;
import dan200.computercraft.shared.util.ImpostorRecipe;
import dan200.computercraft.shared.util.ImpostorShapelessRecipe;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityType;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.IForgeRegistry;

@Mod.EventBusSubscriber( modid = ComputerCraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD )
public final class Registry
{
    private static final ItemGroup mainItemGroup = new CreativeTabMain();

    private Registry()
    {
    }

    @SubscribeEvent
    public static void registerBlocks( RegistryEvent.Register<Block> event )
    {
        IForgeRegistry<Block> registry = event.getRegistry();

        // Computers
        ComputerCraft.Blocks.computerNormal = new BlockComputer(
            Block.Properties.create( Material.ROCK ).hardnessAndResistance( 2.0f ),
            ComputerFamily.Normal, TileComputer.FACTORY_NORMAL
        );

        ComputerCraft.Blocks.computerAdvanced = new BlockComputer(
            Block.Properties.create( Material.ROCK ).hardnessAndResistance( 2.0f ),
            ComputerFamily.Advanced, TileComputer.FACTORY_ADVANCED
        );

        ComputerCraft.Blocks.computerCommand = new BlockComputer(
            Block.Properties.create( Material.ROCK ).hardnessAndResistance( -1, 6000000.0F ),
            ComputerFamily.Command, TileCommandComputer.FACTORY
        );

        registry.registerAll(
            ComputerCraft.Blocks.computerNormal.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "computer_normal" ) ),
            ComputerCraft.Blocks.computerAdvanced.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "computer_advanced" ) ),
            ComputerCraft.Blocks.computerCommand.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "computer_command" ) )
        );

        // Turtles
        ComputerCraft.Blocks.turtleNormal = new BlockTurtle(
            Block.Properties.create( Material.ROCK ).hardnessAndResistance( 2.5f ),
            ComputerFamily.Normal, TileTurtle.FACTORY_NORMAL
        );

        ComputerCraft.Blocks.turtleAdvanced = new BlockTurtle(
            Block.Properties.create( Material.ROCK ).hardnessAndResistance( 2.5f ),
            ComputerFamily.Advanced, TileTurtle.FACTORY_ADVANCED
        );

        registry.registerAll(
            ComputerCraft.Blocks.turtleNormal.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "turtle_normal" ) ),
            ComputerCraft.Blocks.turtleAdvanced.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "turtle_advanced" ) )
        );

        // Peripherals
        ComputerCraft.Blocks.speaker = new BlockSpeaker(
            Block.Properties.create( Material.ROCK ).hardnessAndResistance( 2 )
        );

        ComputerCraft.Blocks.diskDrive = new BlockDiskDrive(
            Block.Properties.create( Material.ROCK ).hardnessAndResistance( 2 )
        );

        ComputerCraft.Blocks.monitorNormal = new BlockMonitor(
            Block.Properties.create( Material.ROCK ).hardnessAndResistance( 2 ),
            TileMonitor.FACTORY_NORMAL
        );

        ComputerCraft.Blocks.monitorAdvanced = new BlockMonitor(
            Block.Properties.create( Material.ROCK ).hardnessAndResistance( 2 ),
            TileMonitor.FACTORY_ADVANCED
        );

        ComputerCraft.Blocks.printer = new BlockPrinter(
            Block.Properties.create( Material.ROCK ).hardnessAndResistance( 2 )
        );

        ComputerCraft.Blocks.wirelessModemNormal = new BlockWirelessModem(
            Block.Properties.create( Material.ROCK ).hardnessAndResistance( 2 ),
            TileWirelessModem.FACTORY_NORMAL
        );

        ComputerCraft.Blocks.wirelessModemAdvanced = new BlockWirelessModem(
            Block.Properties.create( Material.ROCK ).hardnessAndResistance( 2 ),
            TileWirelessModem.FACTORY_ADVANCED
        );

        ComputerCraft.Blocks.wiredModemFull = new BlockWiredModemFull(
            Block.Properties.create( Material.ROCK ).hardnessAndResistance( 1.5f )
        );

        ComputerCraft.Blocks.cable = new BlockCable(
            Block.Properties.create( Material.ROCK ).hardnessAndResistance( 1.5f )
        );

        registry.registerAll(
            ComputerCraft.Blocks.speaker.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "speaker" ) ),
            ComputerCraft.Blocks.diskDrive.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "disk_drive" ) ),
            ComputerCraft.Blocks.monitorNormal.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "monitor_normal" ) ),
            ComputerCraft.Blocks.monitorAdvanced.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "monitor_advanced" ) ),
            ComputerCraft.Blocks.printer.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "printer" ) ),
            ComputerCraft.Blocks.wirelessModemNormal.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "wireless_modem_normal" ) ),
            ComputerCraft.Blocks.wirelessModemAdvanced.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "wireless_modem_advanced" ) ),
            ComputerCraft.Blocks.wiredModemFull.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "wired_modem_full" ) ),
            ComputerCraft.Blocks.cable.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "cable" ) )
        );
    }

    @SubscribeEvent
    public static void registerTileEntities( RegistryEvent.Register<TileEntityType<?>> event )
    {
        IForgeRegistry<TileEntityType<?>> registry = event.getRegistry();

        // Computers
        registry.registerAll( TileComputer.FACTORY_NORMAL, TileComputer.FACTORY_ADVANCED, TileCommandComputer.FACTORY );

        // Turtles
        registry.registerAll( TileTurtle.FACTORY_NORMAL, TileTurtle.FACTORY_ADVANCED );

        // Peripherals
        registry.registerAll(
            TileSpeaker.FACTORY,
            TileDiskDrive.FACTORY,
            TileMonitor.FACTORY_NORMAL,
            TileMonitor.FACTORY_ADVANCED,
            TilePrinter.FACTORY,
            TileWirelessModem.FACTORY_NORMAL,
            TileWirelessModem.FACTORY_ADVANCED,
            TileWiredModemFull.FACTORY,
            TileCable.FACTORY
        );
    }

    private static <T extends BlockItem> T setupItemBlock( T item )
    {
        item.setRegistryName( item.getBlock().getRegistryName() );
        return item;
    }

    private static Item.Properties defaultItem()
    {
        return new Item.Properties().group( mainItemGroup );
    }

    @SubscribeEvent
    public static void registerItems( RegistryEvent.Register<Item> event )
    {
        IForgeRegistry<Item> registry = event.getRegistry();

        // Computer
        ComputerCraft.Items.computerNormal = new ItemComputer( ComputerCraft.Blocks.computerNormal, defaultItem() );
        ComputerCraft.Items.computerAdvanced = new ItemComputer( ComputerCraft.Blocks.computerAdvanced, defaultItem() );
        ComputerCraft.Items.computerCommand = new ItemComputer( ComputerCraft.Blocks.computerCommand, defaultItem() );

        registry.registerAll(
            setupItemBlock( ComputerCraft.Items.computerNormal ),
            setupItemBlock( ComputerCraft.Items.computerAdvanced ),
            setupItemBlock( ComputerCraft.Items.computerCommand )
        );

        // Turtle
        ComputerCraft.Items.turtleNormal = new ItemTurtle( ComputerCraft.Blocks.turtleNormal, defaultItem() );
        ComputerCraft.Items.turtleAdvanced = new ItemTurtle( ComputerCraft.Blocks.turtleAdvanced, defaultItem() );
        registry.registerAll(
            setupItemBlock( ComputerCraft.Items.turtleNormal ),
            setupItemBlock( ComputerCraft.Items.turtleAdvanced )
        );

        // Pocket computer
        ComputerCraft.Items.pocketComputerNormal = new ItemPocketComputer( defaultItem().maxStackSize( 1 ), ComputerFamily.Normal );
        ComputerCraft.Items.pocketComputerAdvanced = new ItemPocketComputer( defaultItem().maxStackSize( 1 ), ComputerFamily.Advanced );

        registry.registerAll(
            ComputerCraft.Items.pocketComputerNormal.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "pocket_computer_normal" ) ),
            ComputerCraft.Items.pocketComputerAdvanced.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "pocket_computer_advanced" ) )
        );

        // Floppy disk
        ComputerCraft.Items.disk = new ItemDisk( defaultItem().maxStackSize( 1 ) );
        ComputerCraft.Items.treasureDisk = new ItemTreasureDisk( defaultItem().maxStackSize( 1 ) );

        registry.registerAll(
            ComputerCraft.Items.disk.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "disk" ) ),
            ComputerCraft.Items.treasureDisk.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "treasure_disk" ) )
        );

        // Printouts
        ComputerCraft.Items.printedPage = new ItemPrintout( defaultItem().maxStackSize( 1 ), ItemPrintout.Type.PAGE );
        ComputerCraft.Items.printedPages = new ItemPrintout( defaultItem().maxStackSize( 1 ), ItemPrintout.Type.PAGES );
        ComputerCraft.Items.printedBook = new ItemPrintout( defaultItem().maxStackSize( 1 ), ItemPrintout.Type.BOOK );

        registry.registerAll(
            ComputerCraft.Items.printedPage.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "printed_page" ) ),
            ComputerCraft.Items.printedPages.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "printed_pages" ) ),
            ComputerCraft.Items.printedBook.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "printed_book" ) )
        );

        // Peripherals
        registry.registerAll(
            setupItemBlock( new BlockItem( ComputerCraft.Blocks.speaker, defaultItem() ) ),
            setupItemBlock( new BlockItem( ComputerCraft.Blocks.diskDrive, defaultItem() ) ),
            setupItemBlock( new BlockItem( ComputerCraft.Blocks.printer, defaultItem() ) ),
            setupItemBlock( new BlockItem( ComputerCraft.Blocks.monitorNormal, defaultItem() ) ),
            setupItemBlock( new BlockItem( ComputerCraft.Blocks.monitorAdvanced, defaultItem() ) ),
            setupItemBlock( new BlockItem( ComputerCraft.Blocks.wirelessModemNormal, defaultItem() ) ),
            setupItemBlock( new BlockItem( ComputerCraft.Blocks.wirelessModemAdvanced, defaultItem() ) ),
            setupItemBlock( new BlockItem( ComputerCraft.Blocks.wiredModemFull, defaultItem() ) )
        );

        ComputerCraft.Items.cable = new ItemBlockCable.Cable( ComputerCraft.Blocks.cable, defaultItem() );
        ComputerCraft.Items.wiredModem = new ItemBlockCable.WiredModem( ComputerCraft.Blocks.cable, defaultItem() );
        registry.registerAll(
            ComputerCraft.Items.cable.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "cable" ) ),
            ComputerCraft.Items.wiredModem.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "wired_modem" ) )
        );

        registerTurtleUpgrades();
        registerPocketUpgrades();
    }

    private static void registerTurtleUpgrades()
    {
        // Upgrades
        ComputerCraft.TurtleUpgrades.wirelessModemNormal = new TurtleModem( false, new ResourceLocation( ComputerCraft.MOD_ID, "wireless_modem_normal" ) );
        TurtleUpgrades.register( ComputerCraft.TurtleUpgrades.wirelessModemNormal );

        ComputerCraft.TurtleUpgrades.wirelessModemAdvanced = new TurtleModem( true, new ResourceLocation( ComputerCraft.MOD_ID, "wireless_modem_advanced" ) );
        ComputerCraftAPI.registerTurtleUpgrade( ComputerCraft.TurtleUpgrades.wirelessModemAdvanced );

        ComputerCraft.TurtleUpgrades.speaker = new TurtleSpeaker( new ResourceLocation( ComputerCraft.MOD_ID, "speaker" ) );
        ComputerCraftAPI.registerTurtleUpgrade( ComputerCraft.TurtleUpgrades.speaker );

        ComputerCraft.TurtleUpgrades.craftingTable = new TurtleCraftingTable( new ResourceLocation( "minecraft", "crafting_table" ) );
        ComputerCraftAPI.registerTurtleUpgrade( ComputerCraft.TurtleUpgrades.craftingTable );

        ComputerCraft.TurtleUpgrades.diamondSword = new TurtleSword( new ResourceLocation( "minecraft", "diamond_sword" ), Items.DIAMOND_SWORD );
        ComputerCraftAPI.registerTurtleUpgrade( ComputerCraft.TurtleUpgrades.diamondSword );

        ComputerCraft.TurtleUpgrades.diamondShovel = new TurtleShovel( new ResourceLocation( "minecraft", "diamond_shovel" ), net.minecraft.item.Items.DIAMOND_SHOVEL );
        ComputerCraftAPI.registerTurtleUpgrade( ComputerCraft.TurtleUpgrades.diamondShovel );

        ComputerCraft.TurtleUpgrades.diamondPickaxe = new TurtleTool( new ResourceLocation( "minecraft", "diamond_pickaxe" ), Items.DIAMOND_PICKAXE );
        ComputerCraftAPI.registerTurtleUpgrade( ComputerCraft.TurtleUpgrades.diamondPickaxe );

        ComputerCraft.TurtleUpgrades.diamondAxe = new TurtleAxe( new ResourceLocation( "minecraft", "diamond_axe" ), Items.DIAMOND_AXE );
        ComputerCraftAPI.registerTurtleUpgrade( ComputerCraft.TurtleUpgrades.diamondAxe );

        ComputerCraft.TurtleUpgrades.diamondHoe = new TurtleHoe( new ResourceLocation( "minecraft", "diamond_hoe" ), Items.DIAMOND_HOE );
        ComputerCraftAPI.registerTurtleUpgrade( ComputerCraft.TurtleUpgrades.diamondHoe );
    }

    private static void registerPocketUpgrades()
    {
        ComputerCraftAPI.registerPocketUpgrade( ComputerCraft.PocketUpgrades.wirelessModemNormal = new PocketModem( false ) );
        ComputerCraftAPI.registerPocketUpgrade( ComputerCraft.PocketUpgrades.wirelessModemAdvanced = new PocketModem( true ) );
        ComputerCraftAPI.registerPocketUpgrade( ComputerCraft.PocketUpgrades.speaker = new PocketSpeaker() );
    }

    @SubscribeEvent
    public static void registerEntities( RegistryEvent.Register<EntityType<?>> registry )
    {
        registry.getRegistry().register( TurtlePlayer.TYPE.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "turtle_player" ) ) );
    }

    @SubscribeEvent
    public static void registerContainers( RegistryEvent.Register<ContainerType<?>> event )
    {
        event.getRegistry().registerAll(
            ContainerComputer.TYPE.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "computer" ) ),
            ContainerPocketComputer.TYPE.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "pocket_computer" ) ),
            ContainerTurtle.TYPE.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "turtle" ) ),

            ContainerDiskDrive.TYPE.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "disk_drive" ) ),
            ContainerPrinter.TYPE.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "printer" ) ),
            ContainerHeldItem.PRINTOUT_TYPE.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "printout" ) ),

            ContainerViewComputer.TYPE.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "view_computer" ) )
        );
    }

    @SubscribeEvent
    public static void regsterRecipeSerializers( RegistryEvent.Register<IRecipeSerializer<?>> event )
    {

        event.getRegistry().registerAll(
            ColourableRecipe.SERIALIZER.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "colour" ) ),
            ComputerUpgradeRecipe.SERIALIZER.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "computer_upgrade" ) ),
            PocketComputerUpgradeRecipe.SERIALIZER.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "pocket_computer_upgrade" ) ),
            DiskRecipe.SERIALIZER.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "disk" ) ),
            PrintoutRecipe.SERIALIZER.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "printout" ) ),
            TurtleRecipe.SERIALIZER.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "turtle" ) ),
            TurtleUpgradeRecipe.SERIALIZER.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "turtle_upgrade" ) ),
            ImpostorShapelessRecipe.SERIALIZER.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "impostor_shapeless" ) ),
            ImpostorRecipe.SERIALIZER.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "impostor_shaped" ) )
        );
    }
}
