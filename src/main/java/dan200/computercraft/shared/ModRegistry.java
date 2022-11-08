/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared;

import com.mojang.brigadier.arguments.ArgumentType;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.detail.IDetailProvider;
import dan200.computercraft.api.detail.VanillaDetailRegistries;
import dan200.computercraft.api.media.IMedia;
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
import dan200.computercraft.shared.data.ConstantLootConditionSerializer;
import dan200.computercraft.shared.data.HasComputerIdLootCondition;
import dan200.computercraft.shared.data.PlayerCreativeLootCondition;
import dan200.computercraft.shared.media.items.ItemDisk;
import dan200.computercraft.shared.media.items.ItemPrintout;
import dan200.computercraft.shared.media.items.ItemTreasureDisk;
import dan200.computercraft.shared.media.items.RecordMedia;
import dan200.computercraft.shared.media.recipes.DiskRecipe;
import dan200.computercraft.shared.media.recipes.PrintoutRecipe;
import dan200.computercraft.shared.network.container.ComputerContainerData;
import dan200.computercraft.shared.network.container.ContainerData;
import dan200.computercraft.shared.network.container.HeldItemContainerData;
import dan200.computercraft.shared.peripheral.diskdrive.BlockDiskDrive;
import dan200.computercraft.shared.peripheral.diskdrive.ContainerDiskDrive;
import dan200.computercraft.shared.peripheral.diskdrive.TileDiskDrive;
import dan200.computercraft.shared.peripheral.generic.data.BlockData;
import dan200.computercraft.shared.peripheral.generic.data.ItemData;
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
import dan200.computercraft.shared.platform.PlatformHelper;
import dan200.computercraft.shared.platform.RegistrationHelper;
import dan200.computercraft.shared.platform.RegistryEntry;
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
import dan200.computercraft.shared.util.ImpostorRecipe;
import dan200.computercraft.shared.util.ImpostorShapelessRecipe;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleRecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Registers ComputerCraft's registry entries and additional objects, such as {@link CauldronInteraction}s and
 * {@link IDetailProvider}s
 * <p>
 * The functions in this class should be called from a loader-specific class.
 */
public final class ModRegistry {
    private static final CreativeModeTab mainItemGroup = new CreativeTabMain();

    private ModRegistry() {
    }

    public static final class Blocks {
        static final RegistrationHelper<Block> REGISTRY = PlatformHelper.get().createRegistrationHelper(Registry.BLOCK_REGISTRY);

        private static BlockBehaviour.Properties properties() {
            return BlockBehaviour.Properties.of(Material.STONE).strength(2);
        }

        private static BlockBehaviour.Properties turtleProperties() {
            return BlockBehaviour.Properties.of(Material.STONE).strength(2.5f);
        }

        private static BlockBehaviour.Properties modemProperties() {
            return BlockBehaviour.Properties.of(Material.STONE).strength(1.5f);
        }

        public static final RegistryEntry<BlockComputer<TileComputer>> COMPUTER_NORMAL = REGISTRY.register("computer_normal",
            () -> new BlockComputer<>(properties(), ComputerFamily.NORMAL, BlockEntities.COMPUTER_NORMAL));
        public static final RegistryEntry<BlockComputer<TileComputer>> COMPUTER_ADVANCED = REGISTRY.register("computer_advanced",
            () -> new BlockComputer<>(properties(), ComputerFamily.ADVANCED, BlockEntities.COMPUTER_ADVANCED));

        public static final RegistryEntry<BlockComputer<TileCommandComputer>> COMPUTER_COMMAND = REGISTRY.register("computer_command", () -> new BlockComputer<>(
            BlockBehaviour.Properties.of(Material.STONE).strength(-1, 6000000.0F),
            ComputerFamily.COMMAND, BlockEntities.COMPUTER_COMMAND
        ));

        public static final RegistryEntry<BlockTurtle> TURTLE_NORMAL = REGISTRY.register("turtle_normal",
            () -> new BlockTurtle(turtleProperties(), ComputerFamily.NORMAL, BlockEntities.TURTLE_NORMAL));
        public static final RegistryEntry<BlockTurtle> TURTLE_ADVANCED = REGISTRY.register("turtle_advanced",
            () -> new BlockTurtle(turtleProperties(), ComputerFamily.ADVANCED, BlockEntities.TURTLE_ADVANCED));

        public static final RegistryEntry<BlockSpeaker> SPEAKER = REGISTRY.register("speaker", () -> new BlockSpeaker(properties()));
        public static final RegistryEntry<BlockDiskDrive> DISK_DRIVE = REGISTRY.register("disk_drive", () -> new BlockDiskDrive(properties()));
        public static final RegistryEntry<BlockPrinter> PRINTER = REGISTRY.register("printer", () -> new BlockPrinter(properties()));

        public static final RegistryEntry<BlockMonitor> MONITOR_NORMAL = REGISTRY.register("monitor_normal",
            () -> new BlockMonitor(properties(), BlockEntities.MONITOR_NORMAL));
        public static final RegistryEntry<BlockMonitor> MONITOR_ADVANCED = REGISTRY.register("monitor_advanced",
            () -> new BlockMonitor(properties(), BlockEntities.MONITOR_ADVANCED));

        public static final RegistryEntry<BlockWirelessModem> WIRELESS_MODEM_NORMAL = REGISTRY.register("wireless_modem_normal",
            () -> new BlockWirelessModem(properties(), BlockEntities.WIRELESS_MODEM_NORMAL));
        public static final RegistryEntry<BlockWirelessModem> WIRELESS_MODEM_ADVANCED = REGISTRY.register("wireless_modem_advanced",
            () -> new BlockWirelessModem(properties(), BlockEntities.WIRELESS_MODEM_ADVANCED));

        public static final RegistryEntry<BlockWiredModemFull> WIRED_MODEM_FULL = REGISTRY.register("wired_modem_full",
            () -> new BlockWiredModemFull(modemProperties()));
        public static final RegistryEntry<BlockCable> CABLE = REGISTRY.register("cable", () -> new BlockCable(modemProperties()));
    }

    public static class BlockEntities {
        static final RegistrationHelper<BlockEntityType<?>> REGISTRY = PlatformHelper.get().createRegistrationHelper(Registry.BLOCK_ENTITY_TYPE_REGISTRY);

        private static <T extends BlockEntity> RegistryEntry<BlockEntityType<T>> ofBlock(RegistryEntry<? extends Block> block, BiFunction<BlockPos, BlockState, T> factory) {
            return REGISTRY.register(block.id().getPath(), () -> PlatformHelper.get().createBlockEntityType(factory, block.get()));
        }

        public static final RegistryEntry<BlockEntityType<TileMonitor>> MONITOR_NORMAL =
            ofBlock(Blocks.MONITOR_NORMAL, (p, s) -> new TileMonitor(BlockEntities.MONITOR_NORMAL.get(), p, s, false));
        public static final RegistryEntry<BlockEntityType<TileMonitor>> MONITOR_ADVANCED =
            ofBlock(Blocks.MONITOR_ADVANCED, (p, s) -> new TileMonitor(BlockEntities.MONITOR_ADVANCED.get(), p, s, true));

        public static final RegistryEntry<BlockEntityType<TileComputer>> COMPUTER_NORMAL =
            ofBlock(Blocks.COMPUTER_NORMAL, (p, s) -> new TileComputer(BlockEntities.COMPUTER_NORMAL.get(), p, s, ComputerFamily.NORMAL));
        public static final RegistryEntry<BlockEntityType<TileComputer>> COMPUTER_ADVANCED =
            ofBlock(Blocks.COMPUTER_ADVANCED, (p, s) -> new TileComputer(BlockEntities.COMPUTER_ADVANCED.get(), p, s, ComputerFamily.ADVANCED));
        public static final RegistryEntry<BlockEntityType<TileCommandComputer>> COMPUTER_COMMAND =
            ofBlock(Blocks.COMPUTER_COMMAND, (p, s) -> new TileCommandComputer(BlockEntities.COMPUTER_COMMAND.get(), p, s));

        public static final RegistryEntry<BlockEntityType<TileTurtle>> TURTLE_NORMAL =
            ofBlock(Blocks.TURTLE_NORMAL, (p, s) -> new TileTurtle(BlockEntities.TURTLE_NORMAL.get(), p, s, ComputerFamily.NORMAL));
        public static final RegistryEntry<BlockEntityType<TileTurtle>> TURTLE_ADVANCED =
            ofBlock(Blocks.TURTLE_ADVANCED, (p, s) -> new TileTurtle(BlockEntities.TURTLE_ADVANCED.get(), p, s, ComputerFamily.ADVANCED));

        public static final RegistryEntry<BlockEntityType<TileSpeaker>> SPEAKER =
            ofBlock(Blocks.SPEAKER, (p, s) -> new TileSpeaker(BlockEntities.SPEAKER.get(), p, s));
        public static final RegistryEntry<BlockEntityType<TileDiskDrive>> DISK_DRIVE =
            ofBlock(Blocks.DISK_DRIVE, (p, s) -> new TileDiskDrive(BlockEntities.DISK_DRIVE.get(), p, s));
        public static final RegistryEntry<BlockEntityType<TilePrinter>> PRINTER =
            ofBlock(Blocks.PRINTER, (p, s) -> new TilePrinter(BlockEntities.PRINTER.get(), p, s));
        public static final RegistryEntry<BlockEntityType<TileWiredModemFull>> WIRED_MODEM_FULL =
            ofBlock(Blocks.WIRED_MODEM_FULL, (p, s) -> new TileWiredModemFull(BlockEntities.WIRED_MODEM_FULL.get(), p, s));
        public static final RegistryEntry<BlockEntityType<TileCable>> CABLE =
            ofBlock(Blocks.CABLE, (p, s) -> new TileCable(BlockEntities.CABLE.get(), p, s));

        public static final RegistryEntry<BlockEntityType<TileWirelessModem>> WIRELESS_MODEM_NORMAL =
            ofBlock(Blocks.WIRELESS_MODEM_NORMAL, (p, s) -> new TileWirelessModem(BlockEntities.WIRELESS_MODEM_NORMAL.get(), p, s, false));
        public static final RegistryEntry<BlockEntityType<TileWirelessModem>> WIRELESS_MODEM_ADVANCED =
            ofBlock(Blocks.WIRELESS_MODEM_ADVANCED, (p, s) -> new TileWirelessModem(BlockEntities.WIRELESS_MODEM_ADVANCED.get(), p, s, true));
    }

    public static final class Items {
        static final RegistrationHelper<Item> REGISTRY = PlatformHelper.get().createRegistrationHelper(Registry.ITEM_REGISTRY);

        private static Item.Properties properties() {
            return new Item.Properties().tab(mainItemGroup);
        }

        private static <B extends Block, I extends Item> RegistryEntry<I> ofBlock(RegistryEntry<B> parent, BiFunction<B, Item.Properties, I> supplier) {
            return REGISTRY.register(parent.id().getPath(), () -> supplier.apply(parent.get(), properties()));
        }

        public static final RegistryEntry<ItemComputer> COMPUTER_NORMAL = ofBlock(Blocks.COMPUTER_NORMAL, ItemComputer::new);
        public static final RegistryEntry<ItemComputer> COMPUTER_ADVANCED = ofBlock(Blocks.COMPUTER_ADVANCED, ItemComputer::new);
        public static final RegistryEntry<ItemComputer> COMPUTER_COMMAND = ofBlock(Blocks.COMPUTER_COMMAND, ItemComputer::new);

        public static final RegistryEntry<ItemPocketComputer> POCKET_COMPUTER_NORMAL = REGISTRY.register("pocket_computer_normal",
            () -> new ItemPocketComputer(properties().stacksTo(1), ComputerFamily.NORMAL));
        public static final RegistryEntry<ItemPocketComputer> POCKET_COMPUTER_ADVANCED = REGISTRY.register("pocket_computer_advanced",
            () -> new ItemPocketComputer(properties().stacksTo(1), ComputerFamily.ADVANCED));

        public static final RegistryEntry<ItemTurtle> TURTLE_NORMAL = ofBlock(Blocks.TURTLE_NORMAL, ItemTurtle::new);
        public static final RegistryEntry<ItemTurtle> TURTLE_ADVANCED = ofBlock(Blocks.TURTLE_ADVANCED, ItemTurtle::new);

        public static final RegistryEntry<ItemDisk> DISK =
            REGISTRY.register("disk", () -> new ItemDisk(properties().stacksTo(1)));
        public static final RegistryEntry<ItemTreasureDisk> TREASURE_DISK =
            REGISTRY.register("treasure_disk", () -> new ItemTreasureDisk(properties().stacksTo(1)));

        public static final RegistryEntry<ItemPrintout> PRINTED_PAGE = REGISTRY.register("printed_page",
            () -> new ItemPrintout(properties().stacksTo(1), ItemPrintout.Type.PAGE));
        public static final RegistryEntry<ItemPrintout> PRINTED_PAGES = REGISTRY.register("printed_pages",
            () -> new ItemPrintout(properties().stacksTo(1), ItemPrintout.Type.PAGES));
        public static final RegistryEntry<ItemPrintout> PRINTED_BOOK = REGISTRY.register("printed_book",
            () -> new ItemPrintout(properties().stacksTo(1), ItemPrintout.Type.BOOK));

        public static final RegistryEntry<BlockItem> SPEAKER = ofBlock(Blocks.SPEAKER, BlockItem::new);
        public static final RegistryEntry<BlockItem> DISK_DRIVE = ofBlock(Blocks.DISK_DRIVE, BlockItem::new);
        public static final RegistryEntry<BlockItem> PRINTER = ofBlock(Blocks.PRINTER, BlockItem::new);
        public static final RegistryEntry<BlockItem> MONITOR_NORMAL = ofBlock(Blocks.MONITOR_NORMAL, BlockItem::new);
        public static final RegistryEntry<BlockItem> MONITOR_ADVANCED = ofBlock(Blocks.MONITOR_ADVANCED, BlockItem::new);
        public static final RegistryEntry<BlockItem> WIRELESS_MODEM_NORMAL = ofBlock(Blocks.WIRELESS_MODEM_NORMAL, BlockItem::new);
        public static final RegistryEntry<BlockItem> WIRELESS_MODEM_ADVANCED = ofBlock(Blocks.WIRELESS_MODEM_ADVANCED, BlockItem::new);
        public static final RegistryEntry<BlockItem> WIRED_MODEM_FULL = ofBlock(Blocks.WIRED_MODEM_FULL, BlockItem::new);

        public static final RegistryEntry<ItemBlockCable.Cable> CABLE = REGISTRY.register("cable",
            () -> new ItemBlockCable.Cable(Blocks.CABLE.get(), properties()));
        public static final RegistryEntry<ItemBlockCable.WiredModem> WIRED_MODEM = REGISTRY.register("wired_modem",
            () -> new ItemBlockCable.WiredModem(Blocks.CABLE.get(), properties()));
    }

    public static class TurtleSerialisers {
        static final RegistrationHelper<TurtleUpgradeSerialiser<?>> REGISTRY = PlatformHelper.get().createRegistrationHelper(TurtleUpgradeSerialiser.REGISTRY_ID);

        public static final RegistryEntry<TurtleUpgradeSerialiser<TurtleSpeaker>> SPEAKER =
            REGISTRY.register("speaker", () -> TurtleUpgradeSerialiser.simpleWithCustomItem(TurtleSpeaker::new));
        public static final RegistryEntry<TurtleUpgradeSerialiser<TurtleCraftingTable>> WORKBENCH =
            REGISTRY.register("workbench", () -> TurtleUpgradeSerialiser.simpleWithCustomItem(TurtleCraftingTable::new));
        public static final RegistryEntry<TurtleUpgradeSerialiser<TurtleModem>> WIRELESS_MODEM_NORMAL =
            REGISTRY.register("wireless_modem_normal", () -> TurtleUpgradeSerialiser.simpleWithCustomItem((id, item) -> new TurtleModem(id, item, false)));
        public static final RegistryEntry<TurtleUpgradeSerialiser<TurtleModem>> WIRELESS_MODEM_ADVANCED =
            REGISTRY.register("wireless_modem_advanced", () -> TurtleUpgradeSerialiser.simpleWithCustomItem((id, item) -> new TurtleModem(id, item, true)));

        public static final RegistryEntry<TurtleUpgradeSerialiser<TurtleTool>> TOOL = REGISTRY.register("tool", () -> TurtleToolSerialiser.INSTANCE);
    }

    public static class PocketUpgradeSerialisers {
        static final RegistrationHelper<PocketUpgradeSerialiser<?>> REGISTRY = PlatformHelper.get().createRegistrationHelper(PocketUpgradeSerialiser.REGISTRY_ID);

        public static final RegistryEntry<PocketUpgradeSerialiser<PocketSpeaker>> SPEAKER =
            REGISTRY.register("speaker", () -> PocketUpgradeSerialiser.simpleWithCustomItem(PocketSpeaker::new));
        public static final RegistryEntry<PocketUpgradeSerialiser<PocketModem>> WIRELESS_MODEM_NORMAL =
            REGISTRY.register("wireless_modem_normal", () -> PocketUpgradeSerialiser.simpleWithCustomItem((id, item) -> new PocketModem(id, item, false)));
        public static final RegistryEntry<PocketUpgradeSerialiser<PocketModem>> WIRELESS_MODEM_ADVANCED =
            REGISTRY.register("wireless_modem_advanced", () -> PocketUpgradeSerialiser.simpleWithCustomItem((id, item) -> new PocketModem(id, item, true)));
    }

    public static class Menus {
        static final RegistrationHelper<MenuType<?>> REGISTRY = PlatformHelper.get().createRegistrationHelper(Registry.MENU_REGISTRY);

        public static final RegistryEntry<MenuType<ContainerComputerBase>> COMPUTER = REGISTRY.register("computer",
            () -> ContainerData.toType(ComputerContainerData::new, (id, inv, data) -> new ComputerMenuWithoutInventory(Menus.COMPUTER.get(), id, inv, data)));

        public static final RegistryEntry<MenuType<ContainerComputerBase>> POCKET_COMPUTER = REGISTRY.register("pocket_computer",
            () -> ContainerData.toType(ComputerContainerData::new, (id, inv, data) -> new ComputerMenuWithoutInventory(Menus.POCKET_COMPUTER.get(), id, inv, data)));

        public static final RegistryEntry<MenuType<ContainerComputerBase>> POCKET_COMPUTER_NO_TERM = REGISTRY.register("pocket_computer_no_term",
            () -> ContainerData.toType(ComputerContainerData::new, (id, inv, data) -> new ComputerMenuWithoutInventory(Menus.POCKET_COMPUTER_NO_TERM.get(), id, inv, data)));

        public static final RegistryEntry<MenuType<ContainerTurtle>> TURTLE = REGISTRY.register("turtle",
            () -> ContainerData.toType(ComputerContainerData::new, ContainerTurtle::ofMenuData));

        public static final RegistryEntry<MenuType<ContainerDiskDrive>> DISK_DRIVE = REGISTRY.register("disk_drive",
            () -> new MenuType<>(ContainerDiskDrive::new));

        public static final RegistryEntry<MenuType<ContainerPrinter>> PRINTER = REGISTRY.register("printer",
            () -> new MenuType<>(ContainerPrinter::new));

        public static final RegistryEntry<MenuType<ContainerHeldItem>> PRINTOUT = REGISTRY.register("printout",
            () -> ContainerData.toType(HeldItemContainerData::new, ContainerHeldItem::createPrintout));

        public static final RegistryEntry<MenuType<ContainerViewComputer>> VIEW_COMPUTER = REGISTRY.register("view_computer",
            () -> ContainerData.toType(ComputerContainerData::new, ContainerViewComputer::new));
    }

    static class ArgumentTypes {
        static final RegistrationHelper<ArgumentTypeInfo<?, ?>> REGISTRY = PlatformHelper.get().createRegistrationHelper(Registry.COMMAND_ARGUMENT_TYPE_REGISTRY);

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

    public static class LootItemConditionTypes {
        static final RegistrationHelper<LootItemConditionType> REGISTRY = PlatformHelper.get().createRegistrationHelper(Registry.LOOT_ITEM_REGISTRY);

        public static final RegistryEntry<LootItemConditionType> BLOCK_NAMED = REGISTRY.register("block_named",
            () -> ConstantLootConditionSerializer.type(BlockNamedEntityLootCondition.INSTANCE));

        public static final RegistryEntry<LootItemConditionType> PLAYER_CREATIVE = REGISTRY.register("player_creative",
            () -> ConstantLootConditionSerializer.type(PlayerCreativeLootCondition.INSTANCE));

        public static final RegistryEntry<LootItemConditionType> HAS_ID = REGISTRY.register("has_id",
            () -> ConstantLootConditionSerializer.type(HasComputerIdLootCondition.INSTANCE));
    }

    public static class RecipeSerializers {
        static final RegistrationHelper<RecipeSerializer<?>> REGISTRY = PlatformHelper.get().createRegistrationHelper(Registry.RECIPE_SERIALIZER_REGISTRY);

        private static <T extends CustomRecipe> RegistryEntry<SimpleRecipeSerializer<T>> simple(String name, Function<ResourceLocation, T> factory) {
            return REGISTRY.register(name, () -> new SimpleRecipeSerializer<>(factory));
        }

        public static final RegistryEntry<SimpleRecipeSerializer<ColourableRecipe>> DYEABLE_ITEM = simple("colour", ColourableRecipe::new);
        public static final RegistryEntry<TurtleRecipe.Serializer> TURTLE = REGISTRY.register("turtle", TurtleRecipe.Serializer::new);
        public static final RegistryEntry<SimpleRecipeSerializer<TurtleUpgradeRecipe>> TURTLE_UPGRADE = simple("turtle_upgrade", TurtleUpgradeRecipe::new);
        public static final RegistryEntry<SimpleRecipeSerializer<PocketComputerUpgradeRecipe>> POCKET_COMPUTER_UPGRADE = simple("pocket_computer_upgrade", PocketComputerUpgradeRecipe::new);
        public static final RegistryEntry<SimpleRecipeSerializer<PrintoutRecipe>> PRINTOUT = simple("printout", PrintoutRecipe::new);
        public static final RegistryEntry<SimpleRecipeSerializer<DiskRecipe>> DISK = simple("disk", DiskRecipe::new);
        public static final RegistryEntry<ComputerUpgradeRecipe.Serializer> COMPUTER_UPGRADE = REGISTRY.register("computer_upgrade", ComputerUpgradeRecipe.Serializer::new);
        public static final RegistryEntry<ImpostorRecipe.Serializer> IMPOSTOR_SHAPED = REGISTRY.register("impostor_shaped", ImpostorRecipe.Serializer::new);
        public static final RegistryEntry<ImpostorShapelessRecipe.Serializer> IMPOSTOR_SHAPELESS = REGISTRY.register("impostor_shapeless", ImpostorShapelessRecipe.Serializer::new);
    }

    /**
     * Register any objects which don't have to be done on the main thread.
     */
    public static void register() {
        Blocks.REGISTRY.register();
        BlockEntities.REGISTRY.register();
        Items.REGISTRY.register();
        TurtleSerialisers.REGISTRY.register();
        PocketUpgradeSerialisers.REGISTRY.register();
        Menus.REGISTRY.register();
        ArgumentTypes.REGISTRY.register();
        LootItemConditionTypes.REGISTRY.register();
        RecipeSerializers.REGISTRY.register();

        // Register bundled power providers
        ComputerCraftAPI.registerBundledRedstoneProvider(new DefaultBundledRedstoneProvider());
        ComputerCraftAPI.registerRefuelHandler(new FurnaceRefuelHandler());
        ComputerCraftAPI.registerMediaProvider(stack -> {
            var item = stack.getItem();
            if (item instanceof IMedia media) return media;
            if (item instanceof RecordItem) return RecordMedia.INSTANCE;
            return null;
        });

        VanillaDetailRegistries.ITEM_STACK.addProvider(ItemData::fill);
        VanillaDetailRegistries.BLOCK_IN_WORLD.addProvider(BlockData::fill);
    }

    /**
     * Register any objects which must be done on the main thread.
     */
    public static void registerMainThread() {
        CauldronInteraction.WATER.put(ModRegistry.Items.TURTLE_NORMAL.get(), ItemTurtle.CAULDRON_INTERACTION);
        CauldronInteraction.WATER.put(ModRegistry.Items.TURTLE_ADVANCED.get(), ItemTurtle.CAULDRON_INTERACTION);
    }
}
