/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared;

import com.mojang.brigadier.arguments.ArgumentType;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.ForgeComputerCraftAPI;
import dan200.computercraft.api.detail.ForgeDetailRegistries;
import dan200.computercraft.api.detail.VanillaDetailRegistries;
import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.api.network.wired.IWiredElement;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.pocket.PocketUpgradeSerialiser;
import dan200.computercraft.api.turtle.TurtleUpgradeSerialiser;
import dan200.computercraft.shared.command.arguments.ComputerArgumentType;
import dan200.computercraft.shared.command.arguments.ComputersArgumentType;
import dan200.computercraft.shared.command.arguments.RepeatArgumentType;
import dan200.computercraft.shared.command.arguments.TrackingFieldArgumentType;
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
import dan200.computercraft.shared.peripheral.diskdrive.BlockDiskDrive;
import dan200.computercraft.shared.peripheral.diskdrive.ContainerDiskDrive;
import dan200.computercraft.shared.peripheral.diskdrive.TileDiskDrive;
import dan200.computercraft.shared.peripheral.generic.data.BlockData;
import dan200.computercraft.shared.peripheral.generic.data.FluidData;
import dan200.computercraft.shared.peripheral.generic.data.ItemData;
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
import dan200.computercraft.shared.turtle.FurnaceRefuelHandler;
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
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.*;

import java.util.function.BiFunction;

@Mod.EventBusSubscriber(modid = ComputerCraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModRegistry {
    private static final CreativeModeTab mainItemGroup = new CreativeTabMain();

    private ModRegistry() {
    }

    public static final class Blocks {
        static final DeferredRegister<Block> REGISTRY = DeferredRegister.create(ForgeRegistries.BLOCKS, ComputerCraft.MOD_ID);

        private static BlockBehaviour.Properties properties() {
            return BlockBehaviour.Properties.of(Material.STONE).strength(2);
        }

        private static BlockBehaviour.Properties turtleProperties() {
            return BlockBehaviour.Properties.of(Material.STONE).strength(2.5f);
        }

        private static BlockBehaviour.Properties modemProperties() {
            return BlockBehaviour.Properties.of(Material.STONE).strength(1.5f);
        }

        public static final RegistryObject<BlockComputer<TileComputer>> COMPUTER_NORMAL = REGISTRY.register("computer_normal",
            () -> new BlockComputer<>(properties(), ComputerFamily.NORMAL, BlockEntities.COMPUTER_NORMAL));
        public static final RegistryObject<BlockComputer<TileComputer>> COMPUTER_ADVANCED = REGISTRY.register("computer_advanced",
            () -> new BlockComputer<>(properties(), ComputerFamily.ADVANCED, BlockEntities.COMPUTER_ADVANCED));

        public static final RegistryObject<BlockComputer<TileCommandComputer>> COMPUTER_COMMAND = REGISTRY.register("computer_command", () -> new BlockComputer<>(
            BlockBehaviour.Properties.of(Material.STONE).strength(-1, 6000000.0F),
            ComputerFamily.COMMAND, BlockEntities.COMPUTER_COMMAND
        ));

        public static final RegistryObject<BlockTurtle> TURTLE_NORMAL = REGISTRY.register("turtle_normal",
            () -> new BlockTurtle(turtleProperties(), ComputerFamily.NORMAL, BlockEntities.TURTLE_NORMAL));
        public static final RegistryObject<BlockTurtle> TURTLE_ADVANCED = REGISTRY.register("turtle_advanced",
            () -> new BlockTurtle(turtleProperties(), ComputerFamily.ADVANCED, BlockEntities.TURTLE_ADVANCED));

        public static final RegistryObject<BlockSpeaker> SPEAKER = REGISTRY.register("speaker", () -> new BlockSpeaker(properties()));
        public static final RegistryObject<BlockDiskDrive> DISK_DRIVE = REGISTRY.register("disk_drive", () -> new BlockDiskDrive(properties()));
        public static final RegistryObject<BlockPrinter> PRINTER = REGISTRY.register("printer", () -> new BlockPrinter(properties()));

        public static final RegistryObject<BlockMonitor> MONITOR_NORMAL = REGISTRY.register("monitor_normal",
            () -> new BlockMonitor(properties(), BlockEntities.MONITOR_NORMAL));
        public static final RegistryObject<BlockMonitor> MONITOR_ADVANCED = REGISTRY.register("monitor_advanced",
            () -> new BlockMonitor(properties(), BlockEntities.MONITOR_ADVANCED));

        public static final RegistryObject<BlockWirelessModem> WIRELESS_MODEM_NORMAL = REGISTRY.register("wireless_modem_normal",
            () -> new BlockWirelessModem(properties(), BlockEntities.WIRELESS_MODEM_NORMAL));
        public static final RegistryObject<BlockWirelessModem> WIRELESS_MODEM_ADVANCED = REGISTRY.register("wireless_modem_advanced",
            () -> new BlockWirelessModem(properties(), BlockEntities.WIRELESS_MODEM_ADVANCED));

        public static final RegistryObject<BlockWiredModemFull> WIRED_MODEM_FULL = REGISTRY.register("wired_modem_full",
            () -> new BlockWiredModemFull(modemProperties()));
        public static final RegistryObject<BlockCable> CABLE = REGISTRY.register("cable", () -> new BlockCable(modemProperties()));
    }

    public static class BlockEntities {
        static final DeferredRegister<BlockEntityType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ComputerCraft.MOD_ID);

        private static <T extends BlockEntity> RegistryObject<BlockEntityType<T>> ofBlock(RegistryObject<? extends Block> block, FixedPointTileEntityType.FixedPointBlockEntitySupplier<T> factory) {
            return REGISTRY.register(block.getId().getPath(), () -> FixedPointTileEntityType.create(block, factory));
        }

        public static final RegistryObject<BlockEntityType<TileMonitor>> MONITOR_NORMAL =
            ofBlock(Blocks.MONITOR_NORMAL, (f, p, s) -> new TileMonitor(f, p, s, false));
        public static final RegistryObject<BlockEntityType<TileMonitor>> MONITOR_ADVANCED =
            ofBlock(Blocks.MONITOR_ADVANCED, (f, p, s) -> new TileMonitor(f, p, s, true));

        public static final RegistryObject<BlockEntityType<TileComputer>> COMPUTER_NORMAL =
            ofBlock(Blocks.COMPUTER_NORMAL, (f, p, s) -> new TileComputer(f, p, s, ComputerFamily.NORMAL));
        public static final RegistryObject<BlockEntityType<TileComputer>> COMPUTER_ADVANCED =
            ofBlock(Blocks.COMPUTER_ADVANCED, (f, p, s) -> new TileComputer(f, p, s, ComputerFamily.ADVANCED));
        public static final RegistryObject<BlockEntityType<TileCommandComputer>> COMPUTER_COMMAND =
            ofBlock(Blocks.COMPUTER_COMMAND, TileCommandComputer::new);

        public static final RegistryObject<BlockEntityType<TileTurtle>> TURTLE_NORMAL =
            ofBlock(Blocks.TURTLE_NORMAL, (f, p, s) -> new TileTurtle(f, p, s, ComputerFamily.NORMAL));
        public static final RegistryObject<BlockEntityType<TileTurtle>> TURTLE_ADVANCED =
            ofBlock(Blocks.TURTLE_ADVANCED, (f, p, s) -> new TileTurtle(f, p, s, ComputerFamily.ADVANCED));

        public static final RegistryObject<BlockEntityType<TileSpeaker>> SPEAKER = ofBlock(Blocks.SPEAKER, TileSpeaker::new);
        public static final RegistryObject<BlockEntityType<TileDiskDrive>> DISK_DRIVE = ofBlock(Blocks.DISK_DRIVE, TileDiskDrive::new);
        public static final RegistryObject<BlockEntityType<TilePrinter>> PRINTER = ofBlock(Blocks.PRINTER, TilePrinter::new);
        public static final RegistryObject<BlockEntityType<TileWiredModemFull>> WIRED_MODEM_FULL = ofBlock(Blocks.WIRED_MODEM_FULL, TileWiredModemFull::new);
        public static final RegistryObject<BlockEntityType<TileCable>> CABLE = ofBlock(Blocks.CABLE, TileCable::new);

        public static final RegistryObject<BlockEntityType<TileWirelessModem>> WIRELESS_MODEM_NORMAL =
            ofBlock(Blocks.WIRELESS_MODEM_NORMAL, (f, p, s) -> new TileWirelessModem(f, p, s, false));
        public static final RegistryObject<BlockEntityType<TileWirelessModem>> WIRELESS_MODEM_ADVANCED =
            ofBlock(Blocks.WIRELESS_MODEM_ADVANCED, (f, p, s) -> new TileWirelessModem(f, p, s, true));
    }

    public static final class Items {
        static final DeferredRegister<Item> REGISTRY = DeferredRegister.create(ForgeRegistries.ITEMS, ComputerCraft.MOD_ID);

        private static Item.Properties properties() {
            return new Item.Properties().tab(mainItemGroup);
        }

        private static <B extends Block, I extends Item> RegistryObject<I> ofBlock(RegistryObject<B> parent, BiFunction<B, Item.Properties, I> supplier) {
            return REGISTRY.register(parent.getId().getPath(), () -> supplier.apply(parent.get(), properties()));
        }

        public static final RegistryObject<ItemComputer> COMPUTER_NORMAL = ofBlock(Blocks.COMPUTER_NORMAL, ItemComputer::new);
        public static final RegistryObject<ItemComputer> COMPUTER_ADVANCED = ofBlock(Blocks.COMPUTER_ADVANCED, ItemComputer::new);
        public static final RegistryObject<ItemComputer> COMPUTER_COMMAND = ofBlock(Blocks.COMPUTER_COMMAND, ItemComputer::new);

        public static final RegistryObject<ItemPocketComputer> POCKET_COMPUTER_NORMAL = REGISTRY.register("pocket_computer_normal",
            () -> new ItemPocketComputer(properties().stacksTo(1), ComputerFamily.NORMAL));
        public static final RegistryObject<ItemPocketComputer> POCKET_COMPUTER_ADVANCED = REGISTRY.register("pocket_computer_advanced",
            () -> new ItemPocketComputer(properties().stacksTo(1), ComputerFamily.ADVANCED));

        public static final RegistryObject<ItemTurtle> TURTLE_NORMAL = ofBlock(Blocks.TURTLE_NORMAL, ItemTurtle::new);
        public static final RegistryObject<ItemTurtle> TURTLE_ADVANCED = ofBlock(Blocks.TURTLE_ADVANCED, ItemTurtle::new);

        public static final RegistryObject<ItemDisk> DISK =
            REGISTRY.register("disk", () -> new ItemDisk(properties().stacksTo(1)));
        public static final RegistryObject<ItemTreasureDisk> TREASURE_DISK =
            REGISTRY.register("treasure_disk", () -> new ItemTreasureDisk(properties().stacksTo(1)));

        public static final RegistryObject<ItemPrintout> PRINTED_PAGE = REGISTRY.register("printed_page",
            () -> new ItemPrintout(properties().stacksTo(1), ItemPrintout.Type.PAGE));
        public static final RegistryObject<ItemPrintout> PRINTED_PAGES = REGISTRY.register("printed_pages",
            () -> new ItemPrintout(properties().stacksTo(1), ItemPrintout.Type.PAGES));
        public static final RegistryObject<ItemPrintout> PRINTED_BOOK = REGISTRY.register("printed_book",
            () -> new ItemPrintout(properties().stacksTo(1), ItemPrintout.Type.BOOK));

        public static final RegistryObject<BlockItem> SPEAKER = ofBlock(Blocks.SPEAKER, BlockItem::new);
        public static final RegistryObject<BlockItem> DISK_DRIVE = ofBlock(Blocks.DISK_DRIVE, BlockItem::new);
        public static final RegistryObject<BlockItem> PRINTER = ofBlock(Blocks.PRINTER, BlockItem::new);
        public static final RegistryObject<BlockItem> MONITOR_NORMAL = ofBlock(Blocks.MONITOR_NORMAL, BlockItem::new);
        public static final RegistryObject<BlockItem> MONITOR_ADVANCED = ofBlock(Blocks.MONITOR_ADVANCED, BlockItem::new);
        public static final RegistryObject<BlockItem> WIRELESS_MODEM_NORMAL = ofBlock(Blocks.WIRELESS_MODEM_NORMAL, BlockItem::new);
        public static final RegistryObject<BlockItem> WIRELESS_MODEM_ADVANCED = ofBlock(Blocks.WIRELESS_MODEM_ADVANCED, BlockItem::new);
        public static final RegistryObject<BlockItem> WIRED_MODEM_FULL = ofBlock(Blocks.WIRED_MODEM_FULL, BlockItem::new);

        public static final RegistryObject<ItemBlockCable.Cable> CABLE = REGISTRY.register("cable",
            () -> new ItemBlockCable.Cable(Blocks.CABLE.get(), properties()));
        public static final RegistryObject<ItemBlockCable.WiredModem> WIRED_MODEM = REGISTRY.register("wired_modem",
            () -> new ItemBlockCable.WiredModem(Blocks.CABLE.get(), properties()));
    }

    public static class TurtleSerialisers {
        static final DeferredRegister<TurtleUpgradeSerialiser<?>> REGISTRY = DeferredRegister.create(TurtleUpgradeSerialiser.REGISTRY_ID.location(), ComputerCraft.MOD_ID);

        public static final RegistryObject<TurtleUpgradeSerialiser<TurtleSpeaker>> SPEAKER =
            REGISTRY.register("speaker", () -> TurtleUpgradeSerialiser.simpleWithCustomItem(TurtleSpeaker::new));
        public static final RegistryObject<TurtleUpgradeSerialiser<TurtleCraftingTable>> WORKBENCH =
            REGISTRY.register("workbench", () -> TurtleUpgradeSerialiser.simpleWithCustomItem(TurtleCraftingTable::new));
        public static final RegistryObject<TurtleUpgradeSerialiser<TurtleModem>> WIRELESS_MODEM_NORMAL =
            REGISTRY.register("wireless_modem_normal", () -> TurtleUpgradeSerialiser.simpleWithCustomItem((id, item) -> new TurtleModem(id, item, false)));
        public static final RegistryObject<TurtleUpgradeSerialiser<TurtleModem>> WIRELESS_MODEM_ADVANCED =
            REGISTRY.register("wireless_modem_advanced", () -> TurtleUpgradeSerialiser.simpleWithCustomItem((id, item) -> new TurtleModem(id, item, true)));

        public static final RegistryObject<TurtleUpgradeSerialiser<TurtleTool>> TOOL = REGISTRY.register("tool", () -> TurtleToolSerialiser.INSTANCE);
    }

    public static class PocketUpgradeSerialisers {
        static final DeferredRegister<PocketUpgradeSerialiser<?>> REGISTRY = DeferredRegister.create(PocketUpgradeSerialiser.REGISTRY_ID, ComputerCraft.MOD_ID);

        public static final RegistryObject<PocketUpgradeSerialiser<PocketSpeaker>> SPEAKER =
            REGISTRY.register("speaker", () -> PocketUpgradeSerialiser.simpleWithCustomItem(PocketSpeaker::new));
        public static final RegistryObject<PocketUpgradeSerialiser<PocketModem>> WIRELESS_MODEM_NORMAL =
            REGISTRY.register("wireless_modem_normal", () -> PocketUpgradeSerialiser.simpleWithCustomItem((id, item) -> new PocketModem(id, item, false)));
        public static final RegistryObject<PocketUpgradeSerialiser<PocketModem>> WIRELESS_MODEM_ADVANCED =
            REGISTRY.register("wireless_modem_advanced", () -> PocketUpgradeSerialiser.simpleWithCustomItem((id, item) -> new PocketModem(id, item, true)));
    }

    public static class Menus {
        static final DeferredRegister<MenuType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.MENU_TYPES, ComputerCraft.MOD_ID);

        public static final RegistryObject<MenuType<ContainerComputerBase>> COMPUTER = REGISTRY.register("computer",
            () -> ContainerData.toType(ComputerContainerData::new, ComputerMenuWithoutInventory::new));

        public static final RegistryObject<MenuType<ContainerComputerBase>> POCKET_COMPUTER = REGISTRY.register("pocket_computer",
            () -> ContainerData.toType(ComputerContainerData::new, ComputerMenuWithoutInventory::new));

        public static final RegistryObject<MenuType<ContainerComputerBase>> POCKET_COMPUTER_NO_TERM = REGISTRY.register("pocket_computer_no_term",
            () -> ContainerData.toType(ComputerContainerData::new, ComputerMenuWithoutInventory::new));

        public static final RegistryObject<MenuType<ContainerTurtle>> TURTLE = REGISTRY.register("turtle",
            () -> ContainerData.toType(ComputerContainerData::new, ContainerTurtle::ofMenuData));

        public static final RegistryObject<MenuType<ContainerDiskDrive>> DISK_DRIVE = REGISTRY.register("disk_drive",
            () -> new MenuType<>(ContainerDiskDrive::new));

        public static final RegistryObject<MenuType<ContainerPrinter>> PRINTER = REGISTRY.register("printer",
            () -> new MenuType<>(ContainerPrinter::new));

        public static final RegistryObject<MenuType<ContainerHeldItem>> PRINTOUT = REGISTRY.register("printout",
            () -> ContainerData.toType(HeldItemContainerData::new, ContainerHeldItem::createPrintout));

        public static final RegistryObject<MenuType<ContainerViewComputer>> VIEW_COMPUTER = REGISTRY.register("view_computer",
            () -> ContainerData.toType(ComputerContainerData::new, ContainerViewComputer::new));
    }

    static class ArgumentTypes {
        static final DeferredRegister<ArgumentTypeInfo<?, ?>> REGISTRY = DeferredRegister.create(Registry.COMMAND_ARGUMENT_TYPE_REGISTRY, ComputerCraft.MOD_ID);

        @SuppressWarnings("unchecked")
        private static <T extends ArgumentType<?>> void registerUnsafe(String name, Class<T> type, ArgumentTypeInfo<?, ?> serializer) {
            REGISTRY.register(name, () -> ArgumentTypeInfos.registerByClass(type, (ArgumentTypeInfo<T, ?>) serializer));
        }

        private static <T extends ArgumentType<?>> void register(String name, Class<T> type, ArgumentTypeInfo<T, ?> serializer) {
            REGISTRY.register(name, () -> ArgumentTypeInfos.registerByClass(type, serializer));
        }

        private static <T extends ArgumentType<?>> void register(String name, Class<T> type, T instance) {
            register(name, type, SingletonArgumentInfo.contextFree(() -> instance));
        }

        static {
            register("tracking_field", TrackingFieldArgumentType.class, TrackingFieldArgumentType.metric());
            register("computer", ComputerArgumentType.class, ComputerArgumentType.oneComputer());
            register("computers", ComputersArgumentType.class, new ComputersArgumentType.Info());
            registerUnsafe("repeat", RepeatArgumentType.class, new RepeatArgumentType.Info());
        }
    }


    @SubscribeEvent
    public static void registerRegistries(NewRegistryEvent event) {
        event.create(new RegistryBuilder<TurtleUpgradeSerialiser<?>>()
            .setName(TurtleUpgradeSerialiser.REGISTRY_ID.location())
            .disableSaving().disableSync());

        event.create(new RegistryBuilder<PocketUpgradeSerialiser<?>>()
            .setName(PocketUpgradeSerialiser.REGISTRY_ID.location())
            .disableSaving().disableSync());
    }

    @SubscribeEvent
    public static void registerRecipeSerializers(RegisterEvent event) {
        event.register(ForgeRegistries.RECIPE_SERIALIZERS.getRegistryKey(), registry -> {
            registry.register(new ResourceLocation(ComputerCraft.MOD_ID, "colour"), ColourableRecipe.SERIALIZER);
            registry.register(new ResourceLocation(ComputerCraft.MOD_ID, "computer_upgrade"), ComputerUpgradeRecipe.SERIALIZER);
            registry.register(new ResourceLocation(ComputerCraft.MOD_ID, "pocket_computer_upgrade"), PocketComputerUpgradeRecipe.SERIALIZER);
            registry.register(new ResourceLocation(ComputerCraft.MOD_ID, "disk"), DiskRecipe.SERIALIZER);
            registry.register(new ResourceLocation(ComputerCraft.MOD_ID, "printout"), PrintoutRecipe.SERIALIZER);
            registry.register(new ResourceLocation(ComputerCraft.MOD_ID, "turtle"), TurtleRecipe.SERIALIZER);
            registry.register(new ResourceLocation(ComputerCraft.MOD_ID, "turtle_upgrade"), TurtleUpgradeRecipe.SERIALIZER);
            registry.register(new ResourceLocation(ComputerCraft.MOD_ID, "impostor_shapeless"), ImpostorShapelessRecipe.SERIALIZER);
            registry.register(new ResourceLocation(ComputerCraft.MOD_ID, "impostor_shaped"), ImpostorRecipe.SERIALIZER);
        });
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(IWiredElement.class);
        event.register(IPeripheral.class);
    }

    @SubscribeEvent
    public static void init(FMLCommonSetupEvent event) {
        NetworkHandler.setup();

        event.enqueueWork(() -> {
            registerProviders();
            registerLoot();
        });

        ComputerCraftAPI.registerGenericSource(new InventoryMethods());
        ComputerCraftAPI.registerGenericSource(new FluidMethods());
        ComputerCraftAPI.registerGenericSource(new EnergyMethods());

        ComputerCraftAPI.registerRefuelHandler(new FurnaceRefuelHandler());
    }

    private static void registerProviders() {
        // Register bundled power providers
        ComputerCraftAPI.registerBundledRedstoneProvider(new DefaultBundledRedstoneProvider());

        // Register media providers
        ComputerCraftAPI.registerMediaProvider(stack -> {
            var item = stack.getItem();
            if (item instanceof IMedia media) return media;
            if (item instanceof RecordItem) return RecordMedia.INSTANCE;
            return null;
        });

        // Register generic capabilities. This can technically be done off-thread, but we need it to happen
        // after Forge's common setup, so this is easiest.
        ForgeComputerCraftAPI.registerGenericCapability(ForgeCapabilities.ITEM_HANDLER);
        ForgeComputerCraftAPI.registerGenericCapability(ForgeCapabilities.ENERGY);
        ForgeComputerCraftAPI.registerGenericCapability(ForgeCapabilities.FLUID_HANDLER);

        VanillaDetailRegistries.ITEM_STACK.addProvider(ItemData::fill);
        VanillaDetailRegistries.BLOCK_IN_WORLD.addProvider(BlockData::fill);
        ForgeDetailRegistries.FLUID_STACK.addProvider(FluidData::fill);

        CauldronInteraction.WATER.put(Items.TURTLE_NORMAL.get(), ItemTurtle.CAULDRON_INTERACTION);
        CauldronInteraction.WATER.put(Items.TURTLE_ADVANCED.get(), ItemTurtle.CAULDRON_INTERACTION);
    }

    public static void registerLoot() {
        registerCondition("block_named", BlockNamedEntityLootCondition.TYPE);
        registerCondition("player_creative", PlayerCreativeLootCondition.TYPE);
        registerCondition("has_id", HasComputerIdLootCondition.TYPE);
    }

    private static void registerCondition(String name, LootItemConditionType serializer) {
        Registry.register(
            Registry.LOOT_CONDITION_TYPE,
            new ResourceLocation(ComputerCraft.MOD_ID, name), serializer
        );
    }

    public static void setup() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        Blocks.REGISTRY.register(bus);
        BlockEntities.REGISTRY.register(bus);
        Items.REGISTRY.register(bus);
        TurtleSerialisers.REGISTRY.register(bus);
        PocketUpgradeSerialisers.REGISTRY.register(bus);
        Menus.REGISTRY.register(bus);
        ArgumentTypes.REGISTRY.register(bus);
    }
}
