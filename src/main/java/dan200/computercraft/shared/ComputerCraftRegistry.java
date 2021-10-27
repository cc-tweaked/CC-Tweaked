/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.shared.common.ContainerHeldItem;
import dan200.computercraft.shared.computer.blocks.BlockComputer;
import dan200.computercraft.shared.computer.blocks.TileCommandComputer;
import dan200.computercraft.shared.computer.blocks.TileComputer;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.inventory.ComputerMenuWithoutInventory;
import dan200.computercraft.shared.computer.inventory.ContainerComputerBase;
import dan200.computercraft.shared.computer.inventory.ContainerViewComputer;
import dan200.computercraft.shared.computer.items.ItemComputer;
import dan200.computercraft.shared.media.items.ItemDisk;
import dan200.computercraft.shared.media.items.ItemPrintout;
import dan200.computercraft.shared.media.items.ItemTreasureDisk;
import dan200.computercraft.shared.network.container.ComputerContainerData;
import dan200.computercraft.shared.network.container.ContainerData;
import dan200.computercraft.shared.network.container.HeldItemContainerData;
import dan200.computercraft.shared.network.container.ViewComputerContainerData;
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
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import dan200.computercraft.shared.pocket.peripherals.PocketModem;
import dan200.computercraft.shared.pocket.peripherals.PocketSpeaker;
import dan200.computercraft.shared.turtle.blocks.BlockTurtle;
import dan200.computercraft.shared.turtle.blocks.TileTurtle;
import dan200.computercraft.shared.turtle.core.TurtlePlayer;
import dan200.computercraft.shared.turtle.inventory.ContainerTurtle;
import dan200.computercraft.shared.turtle.items.ItemTurtle;
import dan200.computercraft.shared.turtle.upgrades.*;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.fabricmc.fabric.api.tool.attribute.v1.FabricToolTags;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;

import java.util.function.BiFunction;

import static net.minecraft.util.registry.Registry.BLOCK_ENTITY_TYPE;

public final class ComputerCraftRegistry
{
    public static final String MOD_ID = ComputerCraft.MOD_ID;

    public static void init()
    {
        Object[] o = {
            ModTiles.CABLE,
            ModBlocks.CABLE,
            ModItems.CABLE,
            ModEntities.TURTLE_PLAYER,
            ModContainers.COMPUTER,
        };

        TurtleUpgrades.registerTurtleUpgrades();
        PocketUpgrades.registerPocketUpgrades();
    }

    public static final class ModBlocks
    {
        public static final BlockComputer COMPUTER_NORMAL = register( "computer_normal",
            new BlockComputer( properties(), ComputerFamily.NORMAL, ComputerCraftRegistry.ModTiles.COMPUTER_NORMAL ) );
        public static final BlockComputer COMPUTER_ADVANCED = register( "computer_advanced",
            new BlockComputer( properties(),
                ComputerFamily.ADVANCED,
                ComputerCraftRegistry.ModTiles.COMPUTER_ADVANCED ) );
        public static final BlockComputer COMPUTER_COMMAND = register( "computer_command",
            new BlockComputer( FabricBlockSettings.copyOf( Blocks.STONE )
                .strength( -1, 6000000.0F ),
                ComputerFamily.COMMAND,
                ComputerCraftRegistry.ModTiles.COMPUTER_COMMAND ) );
        public static final BlockTurtle TURTLE_NORMAL = register( "turtle_normal",
            new BlockTurtle( turtleProperties(), ComputerFamily.NORMAL, ComputerCraftRegistry.ModTiles.TURTLE_NORMAL ) );
        public static final BlockTurtle TURTLE_ADVANCED = register( "turtle_advanced",
            new BlockTurtle( turtleProperties(), ComputerFamily.ADVANCED, ComputerCraftRegistry.ModTiles.TURTLE_ADVANCED ) );
        public static final BlockSpeaker SPEAKER = register( "speaker", new BlockSpeaker( properties() ) );
        public static final BlockDiskDrive DISK_DRIVE = register( "disk_drive", new BlockDiskDrive( properties() ) );
        public static final BlockPrinter PRINTER = register( "printer", new BlockPrinter( properties() ) );
        public static final BlockMonitor MONITOR_NORMAL = register( "monitor_normal", new BlockMonitor( properties(), ModTiles.MONITOR_NORMAL, false ) );
        public static final BlockMonitor MONITOR_ADVANCED = register( "monitor_advanced", new BlockMonitor( properties(), ModTiles.MONITOR_ADVANCED, true ) );
        public static final BlockWirelessModem WIRELESS_MODEM_NORMAL = register( "wireless_modem_normal",
            new BlockWirelessModem( properties(), ComputerCraftRegistry.ModTiles.WIRELESS_MODEM_NORMAL, ComputerFamily.NORMAL ) );
        public static final BlockWirelessModem WIRELESS_MODEM_ADVANCED = register( "wireless_modem_advanced",
            new BlockWirelessModem( properties(), ComputerCraftRegistry.ModTiles.WIRELESS_MODEM_ADVANCED, ComputerFamily.ADVANCED ) );
        public static final BlockWiredModemFull WIRED_MODEM_FULL = register( "wired_modem_full",
            new BlockWiredModemFull( modemProperties(), ComputerCraftRegistry.ModTiles.WIRED_MODEM_FULL ) );
        public static final BlockCable CABLE = register( "cable", new BlockCable( modemProperties() ) );

        private static Block.Settings properties()
        {
            //return FabricBlockSettings.copyOf(Blocks.GLASS)
            //                        .strength(2);
            return AbstractBlock.Settings.of( Material.GLASS )
                .strength( 2F )
                .sounds( BlockSoundGroup.STONE )
                .nonOpaque();
        }

        private static Block.Settings turtleProperties()
        {
            return FabricBlockSettings.copyOf( Blocks.STONE )
                .strength( 2.5f );
        }

        private static Block.Settings modemProperties()
        {
            return FabricBlockSettings.copyOf( Blocks.STONE )
                .breakByHand( true )
                .breakByTool( FabricToolTags.PICKAXES )
                .strength( 1.5f );
        }

        public static <T extends Block> T register( String id, T value )
        {
            return Registry.register( Registry.BLOCK, new Identifier( MOD_ID, id ), value );
        }
    }

    public static class ModTiles
    {

        public static final BlockEntityType<TileMonitor> MONITOR_NORMAL = ofBlock( ModBlocks.MONITOR_NORMAL,
            "monitor_normal",
            ( blockPos, blockState ) -> new TileMonitor( ModTiles.MONITOR_NORMAL, false, blockPos, blockState ) );
        public static final BlockEntityType<TileMonitor> MONITOR_ADVANCED = ofBlock( ModBlocks.MONITOR_ADVANCED,
            "monitor_advanced",
            ( blockPos, blockState ) -> new TileMonitor( ModTiles.MONITOR_ADVANCED, true, blockPos, blockState ) );
        public static final BlockEntityType<TileComputer> COMPUTER_NORMAL = ofBlock( ModBlocks.COMPUTER_NORMAL,
            "computer_normal",
            ( blockPos, blockState ) -> new TileComputer( ComputerFamily.NORMAL, ModTiles.COMPUTER_NORMAL, blockPos, blockState ) );
        public static final BlockEntityType<TileComputer> COMPUTER_ADVANCED = ofBlock( ModBlocks.COMPUTER_ADVANCED,
            "computer_advanced",
            ( blockPos, blockState ) -> new TileComputer( ComputerFamily.ADVANCED, ModTiles.COMPUTER_ADVANCED, blockPos, blockState ) );
        public static final BlockEntityType<TileCommandComputer> COMPUTER_COMMAND = ofBlock( ModBlocks.COMPUTER_COMMAND,
            "computer_command",
            ( blockPos, blockState ) -> new TileCommandComputer( ComputerFamily.COMMAND, ModTiles.COMPUTER_COMMAND, blockPos, blockState ) );
        public static final BlockEntityType<TileTurtle> TURTLE_NORMAL = ofBlock( ModBlocks.TURTLE_NORMAL,
            "turtle_normal",
            ( blockPos, blockState ) -> new TileTurtle( ModTiles.TURTLE_NORMAL, blockPos, blockState, ComputerFamily.NORMAL ) );
        public static final BlockEntityType<TileTurtle> TURTLE_ADVANCED = ofBlock( ModBlocks.TURTLE_ADVANCED,
            "turtle_advanced",
            ( blockPos, blockState ) -> new TileTurtle( ModTiles.TURTLE_ADVANCED, blockPos, blockState, ComputerFamily.ADVANCED ) );
        public static final BlockEntityType<TileSpeaker> SPEAKER = ofBlock( ModBlocks.SPEAKER, "speaker",
            ( blockPos, blockState ) -> new TileSpeaker( ModTiles.SPEAKER, blockPos, blockState ) );
        public static final BlockEntityType<TileDiskDrive> DISK_DRIVE = ofBlock( ModBlocks.DISK_DRIVE, "disk_drive",
            ( blockPos, blockState ) -> new TileDiskDrive( ModTiles.DISK_DRIVE, blockPos, blockState ) );
        public static final BlockEntityType<TilePrinter> PRINTER = ofBlock( ModBlocks.PRINTER, "printer",
            ( blockPos, blockState ) -> new TilePrinter( ModTiles.PRINTER, blockPos, blockState ) );
        public static final BlockEntityType<TileWiredModemFull> WIRED_MODEM_FULL = ofBlock( ModBlocks.WIRED_MODEM_FULL,
            "wired_modem_full",
            ( blockPos, blockState ) -> new TileWiredModemFull( ModTiles.WIRED_MODEM_FULL, blockPos, blockState ) );
        public static final BlockEntityType<TileCable> CABLE = ofBlock( ModBlocks.CABLE, "cable",
            ( blockPos, blockState ) -> new TileCable( ModTiles.CABLE, blockPos, blockState ) );
        public static final BlockEntityType<TileWirelessModem> WIRELESS_MODEM_NORMAL = ofBlock( ModBlocks.WIRELESS_MODEM_NORMAL,
            "wireless_modem_normal",
            ( blockPos, blockState ) -> new TileWirelessModem( ModTiles.WIRELESS_MODEM_NORMAL, false, blockPos, blockState ) );
        public static final BlockEntityType<TileWirelessModem> WIRELESS_MODEM_ADVANCED = ofBlock( ModBlocks.WIRELESS_MODEM_ADVANCED,
            "wireless_modem_advanced",
            ( blockPos, blockState ) -> new TileWirelessModem( ModTiles.WIRELESS_MODEM_ADVANCED, true, blockPos, blockState ) );

        private static <T extends BlockEntity> BlockEntityType<T> ofBlock( Block block, String id, BiFunction<BlockPos, BlockState, T> factory )
        {
            BlockEntityType<T> blockEntityType = FabricBlockEntityTypeBuilder.create( factory::apply, block ).build();
            return Registry.register( BLOCK_ENTITY_TYPE,
                new Identifier( MOD_ID, id ),
                blockEntityType
            );
        }
    }

    public static final class ModItems
    {
        private static final ItemGroup mainItemGroup = ComputerCraft.MAIN_GROUP;
        public static final ItemComputer COMPUTER_NORMAL = ofBlock( ModBlocks.COMPUTER_NORMAL, ItemComputer::new );
        public static final ItemComputer COMPUTER_ADVANCED = ofBlock( ModBlocks.COMPUTER_ADVANCED, ItemComputer::new );
        public static final ItemComputer COMPUTER_COMMAND = ofBlock( ModBlocks.COMPUTER_COMMAND, ItemComputer::new );
        public static final ItemPocketComputer POCKET_COMPUTER_NORMAL = register( "pocket_computer_normal",
            new ItemPocketComputer( properties().maxCount( 1 ), ComputerFamily.NORMAL ) );
        public static final ItemPocketComputer POCKET_COMPUTER_ADVANCED = register( "pocket_computer_advanced",
            new ItemPocketComputer( properties().maxCount( 1 ),
                ComputerFamily.ADVANCED ) );
        public static final ItemTurtle TURTLE_NORMAL = ofBlock( ModBlocks.TURTLE_NORMAL, ItemTurtle::new );
        public static final ItemTurtle TURTLE_ADVANCED = ofBlock( ModBlocks.TURTLE_ADVANCED, ItemTurtle::new );
        public static final ItemDisk DISK = register( "disk", new ItemDisk( properties().maxCount( 1 ) ) );
        public static final ItemTreasureDisk TREASURE_DISK = register( "treasure_disk", new ItemTreasureDisk( properties().maxCount( 1 ) ) );
        public static final ItemPrintout PRINTED_PAGE = register( "printed_page", new ItemPrintout( properties().maxCount( 1 ), ItemPrintout.Type.PAGE ) );
        public static final ItemPrintout PRINTED_PAGES = register( "printed_pages", new ItemPrintout( properties().maxCount( 1 ), ItemPrintout.Type.PAGES ) );
        public static final ItemPrintout PRINTED_BOOK = register( "printed_book", new ItemPrintout( properties().maxCount( 1 ), ItemPrintout.Type.BOOK ) );
        public static final BlockItem SPEAKER = ofBlock( ModBlocks.SPEAKER, BlockItem::new );
        public static final BlockItem DISK_DRIVE = ofBlock( ModBlocks.DISK_DRIVE, BlockItem::new );
        public static final BlockItem PRINTER = ofBlock( ModBlocks.PRINTER, BlockItem::new );
        public static final BlockItem MONITOR_NORMAL = ofBlock( ModBlocks.MONITOR_NORMAL, BlockItem::new );
        public static final BlockItem MONITOR_ADVANCED = ofBlock( ModBlocks.MONITOR_ADVANCED, BlockItem::new );
        public static final BlockItem WIRELESS_MODEM_NORMAL = ofBlock( ModBlocks.WIRELESS_MODEM_NORMAL, BlockItem::new );
        public static final BlockItem WIRELESS_MODEM_ADVANCED = ofBlock( ModBlocks.WIRELESS_MODEM_ADVANCED, BlockItem::new );
        public static final BlockItem WIRED_MODEM_FULL = ofBlock( ModBlocks.WIRED_MODEM_FULL, BlockItem::new );
        public static final ItemBlockCable.Cable CABLE = register( "cable", new ItemBlockCable.Cable( ModBlocks.CABLE, properties() ) );
        public static final ItemBlockCable.WiredModem WIRED_MODEM = register( "wired_modem", new ItemBlockCable.WiredModem( ModBlocks.CABLE, properties() ) );

        private static <B extends Block, I extends Item> I ofBlock( B parent, BiFunction<B, Item.Settings, I> supplier )
        {
            return Registry.register( Registry.ITEM, Registry.BLOCK.getId( parent ), supplier.apply( parent, properties() ) );
        }

        private static Item.Settings properties()
        {
            return new Item.Settings().group( mainItemGroup );
        }

        private static <T extends Item> T register( String id, T item )
        {
            return Registry.register( Registry.ITEM, new Identifier( MOD_ID, id ), item );
        }
    }

    public static class ModEntities
    {
        public static final EntityType<TurtlePlayer> TURTLE_PLAYER = Registry.register( Registry.ENTITY_TYPE,
            new Identifier( MOD_ID, "turtle_player" ),
            EntityType.Builder.<TurtlePlayer>create( SpawnGroup.MISC ).disableSaving()
                .disableSummon()
                .setDimensions(
                    0,
                    0 )
                .build(
                    ComputerCraft.MOD_ID + ":turtle_player" ) );
    }

    public static class ModContainers
    {
        public static final ScreenHandlerType<ContainerComputerBase> COMPUTER = ContainerData.toType( new Identifier( MOD_ID, "computer" ), ModContainers.COMPUTER, ComputerContainerData::new, ComputerMenuWithoutInventory::new );
        public static final ScreenHandlerType<ContainerComputerBase> POCKET_COMPUTER = ContainerData.toType( new Identifier( MOD_ID, "pocket_computer" ), ModContainers.POCKET_COMPUTER, ComputerContainerData::new, ComputerMenuWithoutInventory::new );
        public static final ScreenHandlerType<ContainerComputerBase> POCKET_COMPUTER_NO_TERM = ContainerData.toType( new Identifier( MOD_ID, "pocket_computer_no_term" ), ModContainers.POCKET_COMPUTER_NO_TERM, ComputerContainerData::new, ComputerMenuWithoutInventory::new );
        public static final ScreenHandlerType<ContainerTurtle> TURTLE = ContainerData.toType( new Identifier( MOD_ID, "turtle" ), ComputerContainerData::new, ContainerTurtle::new );
        public static final ScreenHandlerType<ContainerDiskDrive> DISK_DRIVE = registerSimple( "disk_drive", ContainerDiskDrive::new );
        public static final ScreenHandlerType<ContainerPrinter> PRINTER = registerSimple( "printer", ContainerPrinter::new );
        public static final ScreenHandlerType<ContainerHeldItem> PRINTOUT = ContainerData.toType( new Identifier( MOD_ID, "printout" ), HeldItemContainerData::new, ContainerHeldItem::createPrintout );
        public static final ScreenHandlerType<ContainerViewComputer> VIEW_COMPUTER = ContainerData.toType( new Identifier( MOD_ID, "view_computer" ), ViewComputerContainerData::new, ContainerViewComputer::new );

        private static <T extends ScreenHandler> ScreenHandlerType<T> registerSimple( String id,
                                                                                      ScreenHandlerRegistry.SimpleClientHandlerFactory<T> function )
        {
            return ScreenHandlerRegistry.registerSimple( new Identifier( MOD_ID, id ), function );
        }
    }

    public static final class TurtleUpgrades
    {
        public static TurtleModem wirelessModemNormal = new TurtleModem( false, new Identifier( ComputerCraft.MOD_ID, "wireless_modem_normal" ) );
        public static TurtleModem wirelessModemAdvanced = new TurtleModem( true, new Identifier( ComputerCraft.MOD_ID, "wireless_modem_advanced" ) );
        public static TurtleSpeaker speaker = new TurtleSpeaker( new Identifier( ComputerCraft.MOD_ID, "speaker" ) );

        public static TurtleCraftingTable craftingTable = new TurtleCraftingTable( new Identifier( "minecraft", "crafting_table" ) );
        public static TurtleSword diamondSword = new TurtleSword( new Identifier( "minecraft", "diamond_sword" ), Items.DIAMOND_SWORD );
        public static TurtleShovel diamondShovel = new TurtleShovel( new Identifier( "minecraft", "diamond_shovel" ), Items.DIAMOND_SHOVEL );
        public static TurtleTool diamondPickaxe = new TurtleTool( new Identifier( "minecraft", "diamond_pickaxe" ), Items.DIAMOND_PICKAXE );
        public static TurtleAxe diamondAxe = new TurtleAxe( new Identifier( "minecraft", "diamond_axe" ), Items.DIAMOND_AXE );
        public static TurtleHoe diamondHoe = new TurtleHoe( new Identifier( "minecraft", "diamond_hoe" ), Items.DIAMOND_HOE );

        public static TurtleTool netheritePickaxe = new TurtleTool( new Identifier( "minecraft", "netherite_pickaxe" ), Items.NETHERITE_PICKAXE );

        public static void registerTurtleUpgrades()
        {
            ComputerCraftAPI.registerTurtleUpgrade( wirelessModemNormal );
            ComputerCraftAPI.registerTurtleUpgrade( wirelessModemAdvanced );
            ComputerCraftAPI.registerTurtleUpgrade( speaker );

            ComputerCraftAPI.registerTurtleUpgrade( craftingTable );
            ComputerCraftAPI.registerTurtleUpgrade( diamondSword );
            ComputerCraftAPI.registerTurtleUpgrade( diamondShovel );
            ComputerCraftAPI.registerTurtleUpgrade( diamondPickaxe );
            ComputerCraftAPI.registerTurtleUpgrade( diamondAxe );
            ComputerCraftAPI.registerTurtleUpgrade( diamondHoe );

            ComputerCraftAPI.registerTurtleUpgrade( netheritePickaxe );
        }
    }

    public static final class PocketUpgrades
    {
        public static PocketModem wirelessModemNormal = new PocketModem( false );
        public static PocketModem wirelessModemAdvanced = new PocketModem( true );
        public static PocketSpeaker speaker = new PocketSpeaker();

        public static void registerPocketUpgrades()
        {
            ComputerCraftAPI.registerPocketUpgrade( wirelessModemNormal );
            ComputerCraftAPI.registerPocketUpgrade( wirelessModemAdvanced );
            ComputerCraftAPI.registerPocketUpgrade( speaker );
        }
    }


}
