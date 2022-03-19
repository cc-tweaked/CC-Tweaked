/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.api.network.wired.IWiredElement;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.pocket.PocketUpgradeSerialiser;
import dan200.computercraft.api.turtle.TurtleUpgradeSerialiser;
import dan200.computercraft.shared.command.arguments.ArgumentSerializers;
import dan200.computercraft.shared.common.ColourableRecipe;
import dan200.computercraft.shared.common.ContainerHeldItem;
import dan200.computercraft.shared.common.DefaultBundledRedstoneProvider;
import dan200.computercraft.shared.computer.blocks.BlockComputer;
import dan200.computercraft.shared.computer.blocks.TileCommandComputer;
import dan200.computercraft.shared.computer.blocks.TileComputer;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.inventory.ComputerMenuWithoutInventory;
import dan200.computercraft.shared.computer.inventory.ContainerComputerBase;
import dan200.computercraft.shared.computer.inventory.ContainerViewComputer;
import dan200.computercraft.shared.computer.items.ItemComputer;
import dan200.computercraft.shared.computer.recipe.ComputerUpgradeRecipe;
import dan200.computercraft.shared.data.BlockNamedEntityLootCondition;
import dan200.computercraft.shared.data.HasComputerIdLootCondition;
import dan200.computercraft.shared.data.PlayerCreativeLootCondition;
import dan200.computercraft.shared.media.items.ItemDisk;
import dan200.computercraft.shared.media.items.ItemPrintout;
import dan200.computercraft.shared.media.items.ItemTreasureDisk;
import dan200.computercraft.shared.media.items.RecordMedia;
import dan200.computercraft.shared.media.recipes.DiskRecipe;
import dan200.computercraft.shared.media.recipes.PrintoutRecipe;
import dan200.computercraft.shared.network.NetworkHandler;
import dan200.computercraft.shared.network.container.ComputerContainerData;
import dan200.computercraft.shared.network.container.ContainerData;
import dan200.computercraft.shared.network.container.HeldItemContainerData;
import dan200.computercraft.shared.network.container.ViewComputerContainerData;
import dan200.computercraft.shared.peripheral.diskdrive.BlockDiskDrive;
import dan200.computercraft.shared.peripheral.diskdrive.ContainerDiskDrive;
import dan200.computercraft.shared.peripheral.diskdrive.TileDiskDrive;
import dan200.computercraft.shared.peripheral.generic.methods.EnergyMethods;
import dan200.computercraft.shared.peripheral.generic.methods.FluidMethods;
import dan200.computercraft.shared.peripheral.generic.methods.InventoryMethods;
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
import dan200.computercraft.shared.pocket.recipes.PocketComputerUpgradeRecipe;
import dan200.computercraft.shared.turtle.blocks.BlockTurtle;
import dan200.computercraft.shared.turtle.blocks.TileTurtle;
import dan200.computercraft.shared.turtle.inventory.ContainerTurtle;
import dan200.computercraft.shared.turtle.items.ItemTurtle;
import dan200.computercraft.shared.turtle.recipes.TurtleRecipe;
import dan200.computercraft.shared.turtle.recipes.TurtleUpgradeRecipe;
import dan200.computercraft.shared.turtle.upgrades.*;
import dan200.computercraft.shared.util.CreativeTabMain;
import dan200.computercraft.shared.util.FixedPointTileEntityType;
import dan200.computercraft.shared.util.ImpostorRecipe;
import dan200.computercraft.shared.util.ImpostorShapelessRecipe;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.registries.*;

import java.util.function.BiFunction;

@Mod.EventBusSubscriber( modid = ComputerCraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD )
public final class Registry
{
    private static final CreativeModeTab mainItemGroup = new CreativeTabMain();

    private Registry()
    {
    }

    public static final class ModBlocks
    {
        static final DeferredRegister<Block> BLOCKS = DeferredRegister.create( ForgeRegistries.BLOCKS, ComputerCraft.MOD_ID );

        private static BlockBehaviour.Properties properties()
        {
            return BlockBehaviour.Properties.of( Material.STONE ).strength( 2 );
        }

        private static BlockBehaviour.Properties turtleProperties()
        {
            return BlockBehaviour.Properties.of( Material.STONE ).strength( 2.5f );
        }

        private static BlockBehaviour.Properties modemProperties()
        {
            return BlockBehaviour.Properties.of( Material.STONE ).strength( 1.5f );
        }

        public static final RegistryObject<BlockComputer<TileComputer>> COMPUTER_NORMAL = BLOCKS.register( "computer_normal",
            () -> new BlockComputer<>( properties(), ComputerFamily.NORMAL, ModBlockEntities.COMPUTER_NORMAL ) );
        public static final RegistryObject<BlockComputer<TileComputer>> COMPUTER_ADVANCED = BLOCKS.register( "computer_advanced",
            () -> new BlockComputer<>( properties(), ComputerFamily.ADVANCED, ModBlockEntities.COMPUTER_ADVANCED ) );

        public static final RegistryObject<BlockComputer<TileCommandComputer>> COMPUTER_COMMAND = BLOCKS.register( "computer_command", () -> new BlockComputer<>(
            BlockBehaviour.Properties.of( Material.STONE ).strength( -1, 6000000.0F ),
            ComputerFamily.COMMAND, ModBlockEntities.COMPUTER_COMMAND
        ) );

        public static final RegistryObject<BlockTurtle> TURTLE_NORMAL = BLOCKS.register( "turtle_normal",
            () -> new BlockTurtle( turtleProperties(), ComputerFamily.NORMAL, ModBlockEntities.TURTLE_NORMAL ) );
        public static final RegistryObject<BlockTurtle> TURTLE_ADVANCED = BLOCKS.register( "turtle_advanced",
            () -> new BlockTurtle( turtleProperties(), ComputerFamily.ADVANCED, ModBlockEntities.TURTLE_ADVANCED ) );

        public static final RegistryObject<BlockSpeaker> SPEAKER = BLOCKS.register( "speaker", () -> new BlockSpeaker( properties() ) );
        public static final RegistryObject<BlockDiskDrive> DISK_DRIVE = BLOCKS.register( "disk_drive", () -> new BlockDiskDrive( properties() ) );
        public static final RegistryObject<BlockPrinter> PRINTER = BLOCKS.register( "printer", () -> new BlockPrinter( properties() ) );

        public static final RegistryObject<BlockMonitor> MONITOR_NORMAL = BLOCKS.register( "monitor_normal",
            () -> new BlockMonitor( properties(), ModBlockEntities.MONITOR_NORMAL ) );
        public static final RegistryObject<BlockMonitor> MONITOR_ADVANCED = BLOCKS.register( "monitor_advanced",
            () -> new BlockMonitor( properties(), ModBlockEntities.MONITOR_ADVANCED ) );

        public static final RegistryObject<BlockWirelessModem> WIRELESS_MODEM_NORMAL = BLOCKS.register( "wireless_modem_normal",
            () -> new BlockWirelessModem( properties(), ModBlockEntities.WIRELESS_MODEM_NORMAL ) );
        public static final RegistryObject<BlockWirelessModem> WIRELESS_MODEM_ADVANCED = BLOCKS.register( "wireless_modem_advanced",
            () -> new BlockWirelessModem( properties(), ModBlockEntities.WIRELESS_MODEM_ADVANCED ) );

        public static final RegistryObject<BlockWiredModemFull> WIRED_MODEM_FULL = BLOCKS.register( "wired_modem_full",
            () -> new BlockWiredModemFull( modemProperties() ) );
        public static final RegistryObject<BlockCable> CABLE = BLOCKS.register( "cable", () -> new BlockCable( modemProperties() ) );
    }

    public static class ModBlockEntities
    {
        static final DeferredRegister<BlockEntityType<?>> TILES = DeferredRegister.create( ForgeRegistries.BLOCK_ENTITIES, ComputerCraft.MOD_ID );

        private static <T extends BlockEntity> RegistryObject<BlockEntityType<T>> ofBlock( RegistryObject<? extends Block> block, FixedPointTileEntityType.FixedPointBlockEntitySupplier<T> factory )
        {
            return TILES.register( block.getId().getPath(), () -> FixedPointTileEntityType.create( block, factory ) );
        }

        public static final RegistryObject<BlockEntityType<TileMonitor>> MONITOR_NORMAL =
            ofBlock( ModBlocks.MONITOR_NORMAL, ( f, p, s ) -> new TileMonitor( f, p, s, false ) );
        public static final RegistryObject<BlockEntityType<TileMonitor>> MONITOR_ADVANCED =
            ofBlock( ModBlocks.MONITOR_ADVANCED, ( f, p, s ) -> new TileMonitor( f, p, s, true ) );

        public static final RegistryObject<BlockEntityType<TileComputer>> COMPUTER_NORMAL =
            ofBlock( ModBlocks.COMPUTER_NORMAL, ( f, p, s ) -> new TileComputer( f, p, s, ComputerFamily.NORMAL ) );
        public static final RegistryObject<BlockEntityType<TileComputer>> COMPUTER_ADVANCED =
            ofBlock( ModBlocks.COMPUTER_ADVANCED, ( f, p, s ) -> new TileComputer( f, p, s, ComputerFamily.ADVANCED ) );
        public static final RegistryObject<BlockEntityType<TileCommandComputer>> COMPUTER_COMMAND =
            ofBlock( ModBlocks.COMPUTER_COMMAND, TileCommandComputer::new );

        public static final RegistryObject<BlockEntityType<TileTurtle>> TURTLE_NORMAL =
            ofBlock( ModBlocks.TURTLE_NORMAL, ( f, p, s ) -> new TileTurtle( f, p, s, ComputerFamily.NORMAL ) );
        public static final RegistryObject<BlockEntityType<TileTurtle>> TURTLE_ADVANCED =
            ofBlock( ModBlocks.TURTLE_ADVANCED, ( f, p, s ) -> new TileTurtle( f, p, s, ComputerFamily.ADVANCED ) );

        public static final RegistryObject<BlockEntityType<TileSpeaker>> SPEAKER = ofBlock( ModBlocks.SPEAKER, TileSpeaker::new );
        public static final RegistryObject<BlockEntityType<TileDiskDrive>> DISK_DRIVE = ofBlock( ModBlocks.DISK_DRIVE, TileDiskDrive::new );
        public static final RegistryObject<BlockEntityType<TilePrinter>> PRINTER = ofBlock( ModBlocks.PRINTER, TilePrinter::new );
        public static final RegistryObject<BlockEntityType<TileWiredModemFull>> WIRED_MODEM_FULL = ofBlock( ModBlocks.WIRED_MODEM_FULL, TileWiredModemFull::new );
        public static final RegistryObject<BlockEntityType<TileCable>> CABLE = ofBlock( ModBlocks.CABLE, TileCable::new );

        public static final RegistryObject<BlockEntityType<TileWirelessModem>> WIRELESS_MODEM_NORMAL =
            ofBlock( ModBlocks.WIRELESS_MODEM_NORMAL, ( f, p, s ) -> new TileWirelessModem( f, p, s, false ) );
        public static final RegistryObject<BlockEntityType<TileWirelessModem>> WIRELESS_MODEM_ADVANCED =
            ofBlock( ModBlocks.WIRELESS_MODEM_ADVANCED, ( f, p, s ) -> new TileWirelessModem( f, p, s, true ) );
    }

    public static final class ModItems
    {
        static final DeferredRegister<Item> ITEMS = DeferredRegister.create( ForgeRegistries.ITEMS, ComputerCraft.MOD_ID );

        private static Item.Properties properties()
        {
            return new Item.Properties().tab( mainItemGroup );
        }

        private static <B extends Block, I extends Item> RegistryObject<I> ofBlock( RegistryObject<B> parent, BiFunction<B, Item.Properties, I> supplier )
        {
            return ITEMS.register( parent.getId().getPath(), () -> supplier.apply( parent.get(), properties() ) );
        }

        public static final RegistryObject<ItemComputer> COMPUTER_NORMAL = ofBlock( ModBlocks.COMPUTER_NORMAL, ItemComputer::new );
        public static final RegistryObject<ItemComputer> COMPUTER_ADVANCED = ofBlock( ModBlocks.COMPUTER_ADVANCED, ItemComputer::new );
        public static final RegistryObject<ItemComputer> COMPUTER_COMMAND = ofBlock( ModBlocks.COMPUTER_COMMAND, ItemComputer::new );

        public static final RegistryObject<ItemPocketComputer> POCKET_COMPUTER_NORMAL = ITEMS.register( "pocket_computer_normal",
            () -> new ItemPocketComputer( properties().stacksTo( 1 ), ComputerFamily.NORMAL ) );
        public static final RegistryObject<ItemPocketComputer> POCKET_COMPUTER_ADVANCED = ITEMS.register( "pocket_computer_advanced",
            () -> new ItemPocketComputer( properties().stacksTo( 1 ), ComputerFamily.ADVANCED ) );

        public static final RegistryObject<ItemTurtle> TURTLE_NORMAL = ofBlock( ModBlocks.TURTLE_NORMAL, ItemTurtle::new );
        public static final RegistryObject<ItemTurtle> TURTLE_ADVANCED = ofBlock( ModBlocks.TURTLE_ADVANCED, ItemTurtle::new );

        public static final RegistryObject<ItemDisk> DISK =
            ITEMS.register( "disk", () -> new ItemDisk( properties().stacksTo( 1 ) ) );
        public static final RegistryObject<ItemTreasureDisk> TREASURE_DISK =
            ITEMS.register( "treasure_disk", () -> new ItemTreasureDisk( properties().stacksTo( 1 ) ) );

        public static final RegistryObject<ItemPrintout> PRINTED_PAGE = ITEMS.register( "printed_page",
            () -> new ItemPrintout( properties().stacksTo( 1 ), ItemPrintout.Type.PAGE ) );
        public static final RegistryObject<ItemPrintout> PRINTED_PAGES = ITEMS.register( "printed_pages",
            () -> new ItemPrintout( properties().stacksTo( 1 ), ItemPrintout.Type.PAGES ) );
        public static final RegistryObject<ItemPrintout> PRINTED_BOOK = ITEMS.register( "printed_book",
            () -> new ItemPrintout( properties().stacksTo( 1 ), ItemPrintout.Type.BOOK ) );

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

    public static class ModTurtleSerialisers
    {
        static final DeferredRegister<TurtleUpgradeSerialiser<?>> SERIALISERS = DeferredRegister.create( TurtleUpgradeSerialiser.REGISTRY_ID.location(), ComputerCraft.MOD_ID );

        public static final RegistryObject<TurtleUpgradeSerialiser<TurtleSpeaker>> SPEAKER =
            SERIALISERS.register( "speaker", () -> TurtleUpgradeSerialiser.simpleWithCustomItem( TurtleSpeaker::new ) );
        public static final RegistryObject<TurtleUpgradeSerialiser<TurtleCraftingTable>> WORKBENCH =
            SERIALISERS.register( "workbench", () -> TurtleUpgradeSerialiser.simpleWithCustomItem( TurtleCraftingTable::new ) );
        public static final RegistryObject<TurtleUpgradeSerialiser<TurtleModem>> WIRELESS_MODEM_NORMAL =
            SERIALISERS.register( "wireless_modem_normal", () -> TurtleUpgradeSerialiser.simpleWithCustomItem( ( id, item ) -> new TurtleModem( id, item, false ) ) );
        public static final RegistryObject<TurtleUpgradeSerialiser<TurtleModem>> WIRELESS_MODEM_ADVANCED =
            SERIALISERS.register( "wireless_modem_advanced", () -> TurtleUpgradeSerialiser.simpleWithCustomItem( ( id, item ) -> new TurtleModem( id, item, true ) ) );

        public static final RegistryObject<TurtleUpgradeSerialiser<TurtleTool>> TOOL = SERIALISERS.register( "tool", () -> TurtleToolSerialiser.INSTANCE );
    }

    public static class ModPocketUpgradeSerialisers
    {
        static final DeferredRegister<PocketUpgradeSerialiser<?>> SERIALISERS = DeferredRegister.create( PocketUpgradeSerialiser.REGISTRY_ID, ComputerCraft.MOD_ID );

        public static final RegistryObject<PocketUpgradeSerialiser<PocketSpeaker>> SPEAKER =
            SERIALISERS.register( "speaker", () -> PocketUpgradeSerialiser.simpleWithCustomItem( PocketSpeaker::new ) );
        public static final RegistryObject<PocketUpgradeSerialiser<PocketModem>> WIRELESS_MODEM_NORMAL =
            SERIALISERS.register( "wireless_modem_normal", () -> PocketUpgradeSerialiser.simpleWithCustomItem( ( id, item ) -> new PocketModem( id, item, false ) ) );
        public static final RegistryObject<PocketUpgradeSerialiser<PocketModem>> WIRELESS_MODEM_ADVANCED =
            SERIALISERS.register( "wireless_modem_advanced", () -> PocketUpgradeSerialiser.simpleWithCustomItem( ( id, item ) -> new PocketModem( id, item, true ) ) );
    }

    public static class ModContainers
    {
        static final DeferredRegister<MenuType<?>> CONTAINERS = DeferredRegister.create( ForgeRegistries.CONTAINERS, ComputerCraft.MOD_ID );

        public static final RegistryObject<MenuType<ContainerComputerBase>> COMPUTER = CONTAINERS.register( "computer",
            () -> ContainerData.toType( ComputerContainerData::new, ComputerMenuWithoutInventory::new ) );

        public static final RegistryObject<MenuType<ContainerComputerBase>> POCKET_COMPUTER = CONTAINERS.register( "pocket_computer",
            () -> ContainerData.toType( ComputerContainerData::new, ComputerMenuWithoutInventory::new ) );

        public static final RegistryObject<MenuType<ContainerComputerBase>> POCKET_COMPUTER_NO_TERM = CONTAINERS.register( "pocket_computer_no_term",
            () -> ContainerData.toType( ComputerContainerData::new, ComputerMenuWithoutInventory::new ) );

        public static final RegistryObject<MenuType<ContainerTurtle>> TURTLE = CONTAINERS.register( "turtle",
            () -> ContainerData.toType( ComputerContainerData::new, ContainerTurtle::new ) );

        public static final RegistryObject<MenuType<ContainerDiskDrive>> DISK_DRIVE = CONTAINERS.register( "disk_drive",
            () -> new MenuType<>( ContainerDiskDrive::new ) );

        public static final RegistryObject<MenuType<ContainerPrinter>> PRINTER = CONTAINERS.register( "printer",
            () -> new MenuType<>( ContainerPrinter::new ) );

        public static final RegistryObject<MenuType<ContainerHeldItem>> PRINTOUT = CONTAINERS.register( "printout",
            () -> ContainerData.toType( HeldItemContainerData::new, ContainerHeldItem::createPrintout ) );

        public static final RegistryObject<MenuType<ContainerViewComputer>> VIEW_COMPUTER = CONTAINERS.register( "view_computer",
            () -> ContainerData.toType( ViewComputerContainerData::new, ContainerViewComputer::new ) );
    }

    @SubscribeEvent
    public static void registerRegistries( NewRegistryEvent event )
    {
        @SuppressWarnings( "unchecked" )
        Class<TurtleUpgradeSerialiser<?>> turtleType = (Class<TurtleUpgradeSerialiser<?>>) (Class<?>) TurtleUpgradeSerialiser.class;
        event.create( new RegistryBuilder<TurtleUpgradeSerialiser<?>>()
            .setName( TurtleUpgradeSerialiser.REGISTRY_ID.location() )
            .setType( turtleType )
            .disableSaving().disableSync() );

        @SuppressWarnings( "unchecked" )
        Class<PocketUpgradeSerialiser<?>> pocketType = (Class<PocketUpgradeSerialiser<?>>) (Class<?>) PocketUpgradeSerialiser.class;
        event.create( new RegistryBuilder<PocketUpgradeSerialiser<?>>()
            .setName( PocketUpgradeSerialiser.REGISTRY_ID.location() )
            .setType( pocketType )
            .disableSaving().disableSync() );
    }

    @SubscribeEvent
    public static void registerRecipeSerializers( RegistryEvent.Register<RecipeSerializer<?>> event )
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

    @SubscribeEvent
    public static void registerCapabilities( RegisterCapabilitiesEvent event )
    {
        event.register( IWiredElement.class );
        event.register( IPeripheral.class );
    }

    @SubscribeEvent
    public static void init( FMLCommonSetupEvent event )
    {
        NetworkHandler.setup();

        event.enqueueWork( () -> {
            registerProviders();
            ArgumentSerializers.register();
            registerLoot();
        } );

        ComputerCraftAPI.registerGenericSource( new InventoryMethods() );
        ComputerCraftAPI.registerGenericSource( new FluidMethods() );
        ComputerCraftAPI.registerGenericSource( new EnergyMethods() );
    }

    private static void registerProviders()
    {
        // Register bundled power providers
        ComputerCraftAPI.registerBundledRedstoneProvider( new DefaultBundledRedstoneProvider() );

        // Register media providers
        ComputerCraftAPI.registerMediaProvider( stack -> {
            Item item = stack.getItem();
            if( item instanceof IMedia ) return (IMedia) item;
            if( item instanceof RecordItem ) return RecordMedia.INSTANCE;
            return null;
        } );

        // Register generic capabilities. This can technically be done off-thread, but we need it to happen
        // after Forge's common setup, so this is easiest.
        ComputerCraftAPI.registerGenericCapability( CapabilityItemHandler.ITEM_HANDLER_CAPABILITY );
        ComputerCraftAPI.registerGenericCapability( CapabilityEnergy.ENERGY );
        ComputerCraftAPI.registerGenericCapability( CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY );

        CauldronInteraction.WATER.put( ModItems.TURTLE_NORMAL.get(), ItemTurtle.CAULDRON_INTERACTION );
        CauldronInteraction.WATER.put( ModItems.TURTLE_ADVANCED.get(), ItemTurtle.CAULDRON_INTERACTION );
    }

    public static void registerLoot()
    {
        registerCondition( "block_named", BlockNamedEntityLootCondition.TYPE );
        registerCondition( "player_creative", PlayerCreativeLootCondition.TYPE );
        registerCondition( "has_id", HasComputerIdLootCondition.TYPE );
    }

    private static void registerCondition( String name, LootItemConditionType serializer )
    {
        net.minecraft.core.Registry.register(
            net.minecraft.core.Registry.LOOT_CONDITION_TYPE,
            new ResourceLocation( ComputerCraft.MOD_ID, name ), serializer
        );
    }

    public static void setup()
    {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register( bus );
        ModBlockEntities.TILES.register( bus );
        ModItems.ITEMS.register( bus );
        ModTurtleSerialisers.SERIALISERS.register( bus );
        ModPocketUpgradeSerialisers.SERIALISERS.register( bus );
        ModContainers.CONTAINERS.register( bus );
    }
}
