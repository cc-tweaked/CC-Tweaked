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
import dan200.computercraft.shared.util.FixedPointTileEntityType;
import dan200.computercraft.shared.util.ImpostorRecipe;
import dan200.computercraft.shared.util.ImpostorShapelessRecipe;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.Items;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.BiFunction;
import java.util.function.Function;

@Mod.EventBusSubscriber( modid = ComputerCraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD )
public final class Registry
{
    private static final ItemGroup mainItemGroup = new CreativeTabMain();

    private Registry()
    {
    }

    public static final class ModBlocks
    {
        static final DeferredRegister<Block> BLOCKS = DeferredRegister.create( ForgeRegistries.BLOCKS, ComputerCraft.MOD_ID );

        private static Block.Properties properties()
        {
            return Block.Properties.of( Material.STONE ).strength( 2 );
        }

        private static Block.Properties turtleProperties()
        {
            return Block.Properties.of( Material.STONE ).strength( 2.5f );
        }

        private static Block.Properties modemProperties()
        {
            return Block.Properties.of( Material.STONE ).strength( 1.5f );
        }

        public static final RegistryObject<BlockComputer> COMPUTER_NORMAL = BLOCKS.register( "computer_normal",
            () -> new BlockComputer( properties(), ComputerFamily.NORMAL, ModTiles.COMPUTER_NORMAL ) );
        public static final RegistryObject<BlockComputer> COMPUTER_ADVANCED = BLOCKS.register( "computer_advanced",
            () -> new BlockComputer( properties(), ComputerFamily.ADVANCED, ModTiles.COMPUTER_ADVANCED ) );

        public static final RegistryObject<BlockComputer> COMPUTER_COMMAND = BLOCKS.register( "computer_command", () -> new BlockComputer(
            Block.Properties.of( Material.STONE ).strength( -1, 6000000.0F ),
            ComputerFamily.COMMAND, ModTiles.COMPUTER_COMMAND
        ) );

        public static final RegistryObject<BlockTurtle> TURTLE_NORMAL = BLOCKS.register( "turtle_normal",
            () -> new BlockTurtle( turtleProperties(), ComputerFamily.NORMAL, ModTiles.TURTLE_NORMAL ) );
        public static final RegistryObject<BlockTurtle> TURTLE_ADVANCED = BLOCKS.register( "turtle_advanced",
            () -> new BlockTurtle( turtleProperties(), ComputerFamily.ADVANCED, ModTiles.TURTLE_ADVANCED ) );

        public static final RegistryObject<BlockSpeaker> SPEAKER = BLOCKS.register( "speaker", () -> new BlockSpeaker( properties() ) );
        public static final RegistryObject<BlockDiskDrive> DISK_DRIVE = BLOCKS.register( "disk_drive", () -> new BlockDiskDrive( properties() ) );
        public static final RegistryObject<BlockPrinter> PRINTER = BLOCKS.register( "printer", () -> new BlockPrinter( properties() ) );

        public static final RegistryObject<BlockMonitor> MONITOR_NORMAL = BLOCKS.register( "monitor_normal",
            () -> new BlockMonitor( properties(), ModTiles.MONITOR_NORMAL ) );
        public static final RegistryObject<BlockMonitor> MONITOR_ADVANCED = BLOCKS.register( "monitor_advanced",
            () -> new BlockMonitor( properties(), ModTiles.MONITOR_ADVANCED ) );

        public static final RegistryObject<BlockWirelessModem> WIRELESS_MODEM_NORMAL = BLOCKS.register( "wireless_modem_normal",
            () -> new BlockWirelessModem( properties(), ModTiles.WIRELESS_MODEM_NORMAL ) );
        public static final RegistryObject<BlockWirelessModem> WIRELESS_MODEM_ADVANCED = BLOCKS.register( "wireless_modem_advanced",
            () -> new BlockWirelessModem( properties(), ModTiles.WIRELESS_MODEM_ADVANCED ) );

        public static final RegistryObject<BlockWiredModemFull> WIRED_MODEM_FULL = BLOCKS.register( "wired_modem_full",
            () -> new BlockWiredModemFull( modemProperties() ) );
        public static final RegistryObject<BlockCable> CABLE = BLOCKS.register( "cable", () -> new BlockCable( modemProperties() ) );
    }

    public static class ModTiles
    {
        static final DeferredRegister<BlockEntityType<?>> TILES = DeferredRegister.create( ForgeRegistries.TILE_ENTITIES, ComputerCraft.MOD_ID );

        private static <T extends BlockEntity> RegistryObject<BlockEntityType<T>> ofBlock( RegistryObject<? extends Block> block, Function<BlockEntityType<T>, T> factory )
        {
            return TILES.register( block.getId().getPath(), () -> FixedPointTileEntityType.create( block, factory ) );
        }

        public static final RegistryObject<BlockEntityType<TileMonitor>> MONITOR_NORMAL =
            ofBlock( ModBlocks.MONITOR_NORMAL, f -> new TileMonitor( f, false ) );
        public static final RegistryObject<BlockEntityType<TileMonitor>> MONITOR_ADVANCED =
            ofBlock( ModBlocks.MONITOR_ADVANCED, f -> new TileMonitor( f, true ) );

        public static final RegistryObject<BlockEntityType<TileComputer>> COMPUTER_NORMAL =
            ofBlock( ModBlocks.COMPUTER_NORMAL, f -> new TileComputer( ComputerFamily.NORMAL, f ) );
        public static final RegistryObject<BlockEntityType<TileComputer>> COMPUTER_ADVANCED =
            ofBlock( ModBlocks.COMPUTER_ADVANCED, f -> new TileComputer( ComputerFamily.ADVANCED, f ) );
        public static final RegistryObject<BlockEntityType<TileCommandComputer>> COMPUTER_COMMAND =
            ofBlock( ModBlocks.COMPUTER_COMMAND, f -> new TileCommandComputer( ComputerFamily.COMMAND, f ) );

        public static final RegistryObject<BlockEntityType<TileTurtle>> TURTLE_NORMAL =
            ofBlock( ModBlocks.TURTLE_NORMAL, f -> new TileTurtle( f, ComputerFamily.NORMAL ) );
        public static final RegistryObject<BlockEntityType<TileTurtle>> TURTLE_ADVANCED =
            ofBlock( ModBlocks.TURTLE_ADVANCED, f -> new TileTurtle( f, ComputerFamily.ADVANCED ) );

        public static final RegistryObject<BlockEntityType<TileSpeaker>> SPEAKER = ofBlock( ModBlocks.SPEAKER, TileSpeaker::new );
        public static final RegistryObject<BlockEntityType<TileDiskDrive>> DISK_DRIVE = ofBlock( ModBlocks.DISK_DRIVE, TileDiskDrive::new );
        public static final RegistryObject<BlockEntityType<TilePrinter>> PRINTER = ofBlock( ModBlocks.PRINTER, TilePrinter::new );
        public static final RegistryObject<BlockEntityType<TileWiredModemFull>> WIRED_MODEM_FULL = ofBlock( ModBlocks.WIRED_MODEM_FULL, TileWiredModemFull::new );
        public static final RegistryObject<BlockEntityType<TileCable>> CABLE = ofBlock( ModBlocks.CABLE, TileCable::new );

        public static final RegistryObject<BlockEntityType<TileWirelessModem>> WIRELESS_MODEM_NORMAL =
            ofBlock( ModBlocks.WIRELESS_MODEM_NORMAL, f -> new TileWirelessModem( f, false ) );
        public static final RegistryObject<BlockEntityType<TileWirelessModem>> WIRELESS_MODEM_ADVANCED =
            ofBlock( ModBlocks.WIRELESS_MODEM_ADVANCED, f -> new TileWirelessModem( f, true ) );
    }

    public static final class ModItems
    {
        static final DeferredRegister<Item> ITEMS = DeferredRegister.create( ForgeRegistries.ITEMS, ComputerCraft.MOD_ID );

        private static Item.Settings properties()
        {
            return new Item.Settings().group( mainItemGroup );
        }

        private static <B extends Block, I extends Item> RegistryObject<I> ofBlock( RegistryObject<B> parent, BiFunction<B, Item.Settings, I> supplier )
        {
            return ITEMS.register( parent.getId().getPath(), () -> supplier.apply( parent.get(), properties() ) );
        }

        public static final RegistryObject<ItemComputer> COMPUTER_NORMAL = ofBlock( ModBlocks.COMPUTER_NORMAL, ItemComputer::new );
        public static final RegistryObject<ItemComputer> COMPUTER_ADVANCED = ofBlock( ModBlocks.COMPUTER_ADVANCED, ItemComputer::new );
        public static final RegistryObject<ItemComputer> COMPUTER_COMMAND = ofBlock( ModBlocks.COMPUTER_COMMAND, ItemComputer::new );

        public static final RegistryObject<ItemPocketComputer> POCKET_COMPUTER_NORMAL = ITEMS.register( "pocket_computer_normal",
            () -> new ItemPocketComputer( properties().maxCount( 1 ), ComputerFamily.NORMAL ) );
        public static final RegistryObject<ItemPocketComputer> POCKET_COMPUTER_ADVANCED = ITEMS.register( "pocket_computer_advanced",
            () -> new ItemPocketComputer( properties().maxCount( 1 ), ComputerFamily.ADVANCED ) );

        public static final RegistryObject<ItemTurtle> TURTLE_NORMAL = ofBlock( ModBlocks.TURTLE_NORMAL, ItemTurtle::new );
        public static final RegistryObject<ItemTurtle> TURTLE_ADVANCED = ofBlock( ModBlocks.TURTLE_ADVANCED, ItemTurtle::new );

        public static final RegistryObject<ItemDisk> DISK =
            ITEMS.register( "disk", () -> new ItemDisk( properties().maxCount( 1 ) ) );
        public static final RegistryObject<ItemTreasureDisk> TREASURE_DISK =
            ITEMS.register( "treasure_disk", () -> new ItemTreasureDisk( properties().maxCount( 1 ) ) );

        public static final RegistryObject<ItemPrintout> PRINTED_PAGE = ITEMS.register( "printed_page",
            () -> new ItemPrintout( properties().maxCount( 1 ), ItemPrintout.Type.PAGE ) );
        public static final RegistryObject<ItemPrintout> PRINTED_PAGES = ITEMS.register( "printed_pages",
            () -> new ItemPrintout( properties().maxCount( 1 ), ItemPrintout.Type.PAGES ) );
        public static final RegistryObject<ItemPrintout> PRINTED_BOOK = ITEMS.register( "printed_book",
            () -> new ItemPrintout( properties().maxCount( 1 ), ItemPrintout.Type.BOOK ) );

        public static final RegistryObject<BlockItem> SPEAKER = ofBlock( ModBlocks.SPEAKER, BlockItem::new );
        public static final RegistryObject<BlockItem> DISK_DRIVE = ofBlock( ModBlocks.DISK_DRIVE, BlockItem::new );
        public static final RegistryObject<BlockItem> PRINTER = ofBlock( ModBlocks.PRINTER, BlockItem::new );
        public static final RegistryObject<BlockItem> MONITOR_NORMAL = ofBlock( ModBlocks.MONITOR_NORMAL, BlockItem::new );
        public static final RegistryObject<BlockItem> MONITOR_ADVANCED = ofBlock( ModBlocks.MONITOR_ADVANCED, BlockItem::new );
        public static final RegistryObject<BlockItem> WIRELESS_MODEM_NORMAL = ofBlock( ModBlocks.WIRELESS_MODEM_NORMAL, BlockItem::new );
        public static final RegistryObject<BlockItem> WIRELESS_MODEM_ADVANCED = ofBlock( ModBlocks.WIRELESS_MODEM_ADVANCED, BlockItem::new );
        public static final RegistryObject<BlockItem> WIRED_MODEM_FULL = ofBlock( ModBlocks.WIRED_MODEM_FULL, BlockItem::new );

        public static final RegistryObject<ItemBlockCable.Cable> CABLE = ITEMS.register( "cable",
            () -> new ItemBlockCable.Cable( ModBlocks.CABLE.get(), properties() ) );
        public static final RegistryObject<ItemBlockCable.WiredModem> WIRED_MODEM = ITEMS.register( "wired_modem",
            () -> new ItemBlockCable.WiredModem( ModBlocks.CABLE.get(), properties() ) );
    }

    @SubscribeEvent
    public static void registerItems( RegistryEvent.Register<Item> event )
    {
        registerTurtleUpgrades();
        registerPocketUpgrades();
    }

    private static void registerTurtleUpgrades()
    {
        // Upgrades
        ComputerCraft.TurtleUpgrades.wirelessModemNormal = new TurtleModem( false, new Identifier( ComputerCraft.MOD_ID, "wireless_modem_normal" ) );
        TurtleUpgrades.register( ComputerCraft.TurtleUpgrades.wirelessModemNormal );

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

    public static class ModEntities
    {
        static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create( ForgeRegistries.ENTITIES, ComputerCraft.MOD_ID );

        public static final RegistryObject<EntityType<TurtlePlayer>> TURTLE_PLAYER = ENTITIES.register( "turtle_player", () ->
            EntityType.Builder.<TurtlePlayer>create( SpawnGroup.MISC )
                .disableSaving()
                .disableSummon()
                .setDimensions( 0, 0 )
                .build( ComputerCraft.MOD_ID + ":turtle_player" ) );
    }

    public static class ModContainers
    {
        static final DeferredRegister<ScreenHandlerType<?>> CONTAINERS = DeferredRegister.create( ForgeRegistries.CONTAINERS, ComputerCraft.MOD_ID );

        public static final RegistryObject<ScreenHandlerType<ContainerComputer>> COMPUTER = CONTAINERS.register( "computer",
            () -> ContainerData.toType( ComputerContainerData::new, ContainerComputer::new ) );

        public static final RegistryObject<ScreenHandlerType<ContainerPocketComputer>> POCKET_COMPUTER = CONTAINERS.register( "pocket_computer",
            () -> ContainerData.toType( ComputerContainerData::new, ContainerPocketComputer::new ) );

        public static final RegistryObject<ScreenHandlerType<ContainerTurtle>> TURTLE = CONTAINERS.register( "turtle",
            () -> ContainerData.toType( ComputerContainerData::new, ContainerTurtle::new ) );

        public static final RegistryObject<ScreenHandlerType<ContainerDiskDrive>> DISK_DRIVE = CONTAINERS.register( "disk_drive",
            () -> new ScreenHandlerType<>( ContainerDiskDrive::new ) );

        public static final RegistryObject<ScreenHandlerType<ContainerPrinter>> PRINTER = CONTAINERS.register( "printer",
            () -> new ScreenHandlerType<>( ContainerPrinter::new ) );

        public static final RegistryObject<ScreenHandlerType<ContainerHeldItem>> PRINTOUT = CONTAINERS.register( "printout",
            () -> ContainerData.toType( HeldItemContainerData::new, ContainerHeldItem::createPrintout ) );

        public static final RegistryObject<ScreenHandlerType<ContainerViewComputer>> VIEW_COMPUTER = CONTAINERS.register( "view_computer",
            () -> ContainerData.toType( ViewComputerContainerData::new, ContainerViewComputer::new ) );
    }

    @SubscribeEvent
    public static void registerRecipeSerializers( RegistryEvent.Register<RecipeSerializer<?>> event )
    {
        event.getRegistry().registerAll(
            ColourableRecipe.SERIALIZER.setRegistryName( new Identifier( ComputerCraft.MOD_ID, "colour" ) ),
            ComputerUpgradeRecipe.SERIALIZER.setRegistryName( new Identifier( ComputerCraft.MOD_ID, "computer_upgrade" ) ),
            PocketComputerUpgradeRecipe.SERIALIZER.setRegistryName( new Identifier( ComputerCraft.MOD_ID, "pocket_computer_upgrade" ) ),
            DiskRecipe.SERIALIZER.setRegistryName( new Identifier( ComputerCraft.MOD_ID, "disk" ) ),
            PrintoutRecipe.SERIALIZER.setRegistryName( new Identifier( ComputerCraft.MOD_ID, "printout" ) ),
            TurtleRecipe.SERIALIZER.setRegistryName( new Identifier( ComputerCraft.MOD_ID, "turtle" ) ),
            TurtleUpgradeRecipe.SERIALIZER.setRegistryName( new Identifier( ComputerCraft.MOD_ID, "turtle_upgrade" ) ),
            ImpostorShapelessRecipe.SERIALIZER.setRegistryName( new Identifier( ComputerCraft.MOD_ID, "impostor_shapeless" ) ),
            ImpostorRecipe.SERIALIZER.setRegistryName( new Identifier( ComputerCraft.MOD_ID, "impostor_shaped" ) )
        );
    }

    public static void setup()
    {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register( bus );
        ModTiles.TILES.register( bus );
        ModItems.ITEMS.register( bus );
        ModEntities.ENTITIES.register( bus );
        ModContainers.CONTAINERS.register( bus );
    }
}
