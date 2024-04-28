// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.detail.DetailProvider;
import dan200.computercraft.api.detail.VanillaDetailRegistries;
import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.upgrades.UpgradeBase;
import dan200.computercraft.api.upgrades.UpgradeData;
import dan200.computercraft.api.upgrades.UpgradeType;
import dan200.computercraft.core.util.Colour;
import dan200.computercraft.impl.PocketUpgrades;
import dan200.computercraft.impl.RegistryHelper;
import dan200.computercraft.shared.command.UserLevel;
import dan200.computercraft.shared.command.arguments.ComputerArgumentType;
import dan200.computercraft.shared.command.arguments.RepeatArgumentType;
import dan200.computercraft.shared.command.arguments.TrackingFieldArgumentType;
import dan200.computercraft.shared.common.ClearColourRecipe;
import dan200.computercraft.shared.common.ColourableRecipe;
import dan200.computercraft.shared.common.DefaultBundledRedstoneProvider;
import dan200.computercraft.shared.common.HeldItemMenu;
import dan200.computercraft.shared.computer.blocks.CommandComputerBlock;
import dan200.computercraft.shared.computer.blocks.CommandComputerBlockEntity;
import dan200.computercraft.shared.computer.blocks.ComputerBlock;
import dan200.computercraft.shared.computer.blocks.ComputerBlockEntity;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.inventory.ComputerMenuWithoutInventory;
import dan200.computercraft.shared.computer.inventory.ViewComputerMenu;
import dan200.computercraft.shared.computer.items.AbstractComputerItem;
import dan200.computercraft.shared.computer.items.CommandComputerItem;
import dan200.computercraft.shared.computer.items.ComputerItem;
import dan200.computercraft.shared.computer.items.ServerComputerReference;
import dan200.computercraft.shared.config.Config;
import dan200.computercraft.shared.data.BlockNamedEntityLootCondition;
import dan200.computercraft.shared.data.HasComputerIdLootCondition;
import dan200.computercraft.shared.data.PlayerCreativeLootCondition;
import dan200.computercraft.shared.details.BlockDetails;
import dan200.computercraft.shared.details.ItemDetails;
import dan200.computercraft.shared.integration.PermissionRegistry;
import dan200.computercraft.shared.media.items.*;
import dan200.computercraft.shared.media.recipes.DiskRecipe;
import dan200.computercraft.shared.media.recipes.PrintoutRecipe;
import dan200.computercraft.shared.network.container.ComputerContainerData;
import dan200.computercraft.shared.network.container.ContainerData;
import dan200.computercraft.shared.network.container.HeldItemContainerData;
import dan200.computercraft.shared.peripheral.diskdrive.DiskDriveBlock;
import dan200.computercraft.shared.peripheral.diskdrive.DiskDriveBlockEntity;
import dan200.computercraft.shared.peripheral.diskdrive.DiskDriveMenu;
import dan200.computercraft.shared.peripheral.modem.wired.*;
import dan200.computercraft.shared.peripheral.modem.wireless.WirelessModemBlock;
import dan200.computercraft.shared.peripheral.modem.wireless.WirelessModemBlockEntity;
import dan200.computercraft.shared.peripheral.monitor.MonitorBlock;
import dan200.computercraft.shared.peripheral.monitor.MonitorBlockEntity;
import dan200.computercraft.shared.peripheral.printer.PrinterBlock;
import dan200.computercraft.shared.peripheral.printer.PrinterBlockEntity;
import dan200.computercraft.shared.peripheral.printer.PrinterMenu;
import dan200.computercraft.shared.peripheral.speaker.SpeakerBlock;
import dan200.computercraft.shared.peripheral.speaker.SpeakerBlockEntity;
import dan200.computercraft.shared.platform.PlatformHelper;
import dan200.computercraft.shared.platform.RegistrationHelper;
import dan200.computercraft.shared.platform.RegistryEntry;
import dan200.computercraft.shared.pocket.items.PocketComputerItem;
import dan200.computercraft.shared.pocket.peripherals.PocketModem;
import dan200.computercraft.shared.pocket.peripherals.PocketSpeaker;
import dan200.computercraft.shared.pocket.recipes.PocketComputerUpgradeRecipe;
import dan200.computercraft.shared.recipe.*;
import dan200.computercraft.shared.recipe.function.CopyComponents;
import dan200.computercraft.shared.recipe.function.RecipeFunction;
import dan200.computercraft.shared.turtle.FurnaceRefuelHandler;
import dan200.computercraft.shared.turtle.blocks.TurtleBlock;
import dan200.computercraft.shared.turtle.blocks.TurtleBlockEntity;
import dan200.computercraft.shared.turtle.inventory.TurtleMenu;
import dan200.computercraft.shared.turtle.items.TurtleItem;
import dan200.computercraft.shared.turtle.recipes.TurtleUpgradeRecipe;
import dan200.computercraft.shared.turtle.upgrades.TurtleCraftingTable;
import dan200.computercraft.shared.turtle.upgrades.TurtleModem;
import dan200.computercraft.shared.turtle.upgrades.TurtleSpeaker;
import dan200.computercraft.shared.turtle.upgrades.TurtleTool;
import dan200.computercraft.shared.util.DataComponentUtil;
import dan200.computercraft.shared.util.NonNegativeId;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Registers ComputerCraft's registry entries and additional objects, such as {@link CauldronInteraction}s and
 * {@link DetailProvider}s
 * <p>
 * The functions in this class should be called from a loader-specific class.
 */
public final class ModRegistry {
    private ModRegistry() {
    }

    public static final ResourceKey<Registry<ITurtleUpgrade>> TURTLE_UPGRADE = RegistryHelper.TURTLE_UPGRADE;
    public static final ResourceKey<Registry<IPocketUpgrade>> POCKET_UPGRADE = RegistryHelper.POCKET_UPGRADE;

    public static final class Blocks {
        static final RegistrationHelper<Block> REGISTRY = PlatformHelper.get().createRegistrationHelper(Registries.BLOCK);

        private static BlockBehaviour.Properties properties() {
            return BlockBehaviour.Properties.of().strength(2);
        }

        private static BlockBehaviour.Properties computerProperties() {
            // Computers shouldn't conduct redstone through them, so set isRedstoneConductor to false. This still allows
            // redstone to connect to computers though as it's a signal source.
            return properties().isRedstoneConductor((block, level, blockPos) -> false);
        }

        private static BlockBehaviour.Properties turtleProperties() {
            return BlockBehaviour.Properties.of().strength(2.5f);
        }

        private static BlockBehaviour.Properties modemProperties() {
            return BlockBehaviour.Properties.of().strength(1.5f);
        }

        public static final RegistryEntry<ComputerBlock<ComputerBlockEntity>> COMPUTER_NORMAL = REGISTRY.register("computer_normal",
            () -> new ComputerBlock<>(computerProperties().mapColor(MapColor.STONE), BlockEntities.COMPUTER_NORMAL));
        public static final RegistryEntry<ComputerBlock<ComputerBlockEntity>> COMPUTER_ADVANCED = REGISTRY.register("computer_advanced",
            () -> new ComputerBlock<>(computerProperties().mapColor(MapColor.GOLD), BlockEntities.COMPUTER_ADVANCED));

        public static final RegistryEntry<ComputerBlock<CommandComputerBlockEntity>> COMPUTER_COMMAND = REGISTRY.register("computer_command",
            () -> new CommandComputerBlock<>(computerProperties().strength(-1, 6000000.0F), BlockEntities.COMPUTER_COMMAND));

        public static final RegistryEntry<TurtleBlock> TURTLE_NORMAL = REGISTRY.register("turtle_normal",
            () -> new TurtleBlock(turtleProperties().mapColor(MapColor.STONE), BlockEntities.TURTLE_NORMAL));
        public static final RegistryEntry<TurtleBlock> TURTLE_ADVANCED = REGISTRY.register("turtle_advanced",
            () -> new TurtleBlock(turtleProperties().mapColor(MapColor.GOLD).explosionResistance(TurtleBlock.IMMUNE_EXPLOSION_RESISTANCE), BlockEntities.TURTLE_ADVANCED));

        public static final RegistryEntry<SpeakerBlock> SPEAKER = REGISTRY.register("speaker", () -> new SpeakerBlock(properties().mapColor(MapColor.STONE)));
        public static final RegistryEntry<DiskDriveBlock> DISK_DRIVE = REGISTRY.register("disk_drive", () -> new DiskDriveBlock(properties().mapColor(MapColor.STONE)));
        public static final RegistryEntry<PrinterBlock> PRINTER = REGISTRY.register("printer", () -> new PrinterBlock(properties().mapColor(MapColor.STONE)));

        public static final RegistryEntry<MonitorBlock> MONITOR_NORMAL = REGISTRY.register("monitor_normal",
            () -> new MonitorBlock(properties().mapColor(MapColor.STONE), BlockEntities.MONITOR_NORMAL));
        public static final RegistryEntry<MonitorBlock> MONITOR_ADVANCED = REGISTRY.register("monitor_advanced",
            () -> new MonitorBlock(properties().mapColor(MapColor.GOLD), BlockEntities.MONITOR_ADVANCED));

        public static final RegistryEntry<WirelessModemBlock> WIRELESS_MODEM_NORMAL = REGISTRY.register("wireless_modem_normal",
            () -> new WirelessModemBlock(properties().mapColor(MapColor.STONE), BlockEntities.WIRELESS_MODEM_NORMAL));
        public static final RegistryEntry<WirelessModemBlock> WIRELESS_MODEM_ADVANCED = REGISTRY.register("wireless_modem_advanced",
            () -> new WirelessModemBlock(properties().mapColor(MapColor.GOLD), BlockEntities.WIRELESS_MODEM_ADVANCED));

        public static final RegistryEntry<WiredModemFullBlock> WIRED_MODEM_FULL = REGISTRY.register("wired_modem_full",
            () -> new WiredModemFullBlock(modemProperties().mapColor(MapColor.STONE)));
        public static final RegistryEntry<CableBlock> CABLE = REGISTRY.register("cable", () -> new CableBlock(modemProperties().mapColor(MapColor.STONE)));
    }

    public static class BlockEntities {
        static final RegistrationHelper<BlockEntityType<?>> REGISTRY = PlatformHelper.get().createRegistrationHelper(Registries.BLOCK_ENTITY_TYPE);

        private static <T extends BlockEntity> RegistryEntry<BlockEntityType<T>> ofBlock(RegistryEntry<? extends Block> block, BlockEntityType.BlockEntitySupplier<T> factory) {
            return REGISTRY.register(block.id().getPath(), () -> BlockEntityType.Builder.of(factory, block.get()).build(null));
        }

        public static final RegistryEntry<BlockEntityType<MonitorBlockEntity>> MONITOR_NORMAL =
            ofBlock(Blocks.MONITOR_NORMAL, (p, s) -> new MonitorBlockEntity(BlockEntities.MONITOR_NORMAL.get(), p, s, false));
        public static final RegistryEntry<BlockEntityType<MonitorBlockEntity>> MONITOR_ADVANCED =
            ofBlock(Blocks.MONITOR_ADVANCED, (p, s) -> new MonitorBlockEntity(BlockEntities.MONITOR_ADVANCED.get(), p, s, true));

        public static final RegistryEntry<BlockEntityType<ComputerBlockEntity>> COMPUTER_NORMAL =
            ofBlock(Blocks.COMPUTER_NORMAL, (p, s) -> new ComputerBlockEntity(BlockEntities.COMPUTER_NORMAL.get(), p, s, ComputerFamily.NORMAL));
        public static final RegistryEntry<BlockEntityType<ComputerBlockEntity>> COMPUTER_ADVANCED =
            ofBlock(Blocks.COMPUTER_ADVANCED, (p, s) -> new ComputerBlockEntity(BlockEntities.COMPUTER_ADVANCED.get(), p, s, ComputerFamily.ADVANCED));
        public static final RegistryEntry<BlockEntityType<CommandComputerBlockEntity>> COMPUTER_COMMAND =
            ofBlock(Blocks.COMPUTER_COMMAND, (p, s) -> new CommandComputerBlockEntity(BlockEntities.COMPUTER_COMMAND.get(), p, s));

        public static final RegistryEntry<BlockEntityType<TurtleBlockEntity>> TURTLE_NORMAL =
            ofBlock(Blocks.TURTLE_NORMAL, (p, s) -> new TurtleBlockEntity(BlockEntities.TURTLE_NORMAL.get(), p, s, () -> Config.turtleFuelLimit, ComputerFamily.NORMAL));
        public static final RegistryEntry<BlockEntityType<TurtleBlockEntity>> TURTLE_ADVANCED =
            ofBlock(Blocks.TURTLE_ADVANCED, (p, s) -> new TurtleBlockEntity(BlockEntities.TURTLE_ADVANCED.get(), p, s, () -> Config.advancedTurtleFuelLimit, ComputerFamily.ADVANCED));

        public static final RegistryEntry<BlockEntityType<SpeakerBlockEntity>> SPEAKER =
            ofBlock(Blocks.SPEAKER, (p, s) -> new SpeakerBlockEntity(BlockEntities.SPEAKER.get(), p, s));
        public static final RegistryEntry<BlockEntityType<DiskDriveBlockEntity>> DISK_DRIVE =
            ofBlock(Blocks.DISK_DRIVE, (p, s) -> new DiskDriveBlockEntity(BlockEntities.DISK_DRIVE.get(), p, s));
        public static final RegistryEntry<BlockEntityType<PrinterBlockEntity>> PRINTER =
            ofBlock(Blocks.PRINTER, (p, s) -> new PrinterBlockEntity(BlockEntities.PRINTER.get(), p, s));
        public static final RegistryEntry<BlockEntityType<WiredModemFullBlockEntity>> WIRED_MODEM_FULL =
            ofBlock(Blocks.WIRED_MODEM_FULL, (p, s) -> new WiredModemFullBlockEntity(BlockEntities.WIRED_MODEM_FULL.get(), p, s));
        public static final RegistryEntry<BlockEntityType<CableBlockEntity>> CABLE =
            ofBlock(Blocks.CABLE, (p, s) -> new CableBlockEntity(BlockEntities.CABLE.get(), p, s));

        public static final RegistryEntry<BlockEntityType<WirelessModemBlockEntity>> WIRELESS_MODEM_NORMAL =
            ofBlock(Blocks.WIRELESS_MODEM_NORMAL, (p, s) -> new WirelessModemBlockEntity(BlockEntities.WIRELESS_MODEM_NORMAL.get(), p, s, false));
        public static final RegistryEntry<BlockEntityType<WirelessModemBlockEntity>> WIRELESS_MODEM_ADVANCED =
            ofBlock(Blocks.WIRELESS_MODEM_ADVANCED, (p, s) -> new WirelessModemBlockEntity(BlockEntities.WIRELESS_MODEM_ADVANCED.get(), p, s, true));
    }

    public static final class Items {
        static final RegistrationHelper<Item> REGISTRY = PlatformHelper.get().createRegistrationHelper(Registries.ITEM);

        private static Item.Properties properties() {
            return new Item.Properties();
        }

        private static <B extends Block, I extends Item> RegistryEntry<I> ofBlock(RegistryEntry<B> parent, BiFunction<B, Item.Properties, I> supplier) {
            return REGISTRY.register(parent.id().getPath(), () -> supplier.apply(parent.get(), properties()));
        }

        public static final RegistryEntry<ComputerItem> COMPUTER_NORMAL = ofBlock(Blocks.COMPUTER_NORMAL, ComputerItem::new);
        public static final RegistryEntry<ComputerItem> COMPUTER_ADVANCED = ofBlock(Blocks.COMPUTER_ADVANCED, ComputerItem::new);
        public static final RegistryEntry<ComputerItem> COMPUTER_COMMAND = ofBlock(Blocks.COMPUTER_COMMAND, CommandComputerItem::new);

        public static final RegistryEntry<PocketComputerItem> POCKET_COMPUTER_NORMAL = REGISTRY.register("pocket_computer_normal",
            () -> new PocketComputerItem(properties().stacksTo(1), ComputerFamily.NORMAL));
        public static final RegistryEntry<PocketComputerItem> POCKET_COMPUTER_ADVANCED = REGISTRY.register("pocket_computer_advanced",
            () -> new PocketComputerItem(properties().stacksTo(1), ComputerFamily.ADVANCED));

        public static final RegistryEntry<TurtleItem> TURTLE_NORMAL = ofBlock(Blocks.TURTLE_NORMAL, TurtleItem::new);
        public static final RegistryEntry<TurtleItem> TURTLE_ADVANCED = ofBlock(Blocks.TURTLE_ADVANCED, TurtleItem::new);

        public static final RegistryEntry<DiskItem> DISK =
            REGISTRY.register("disk", () -> new DiskItem(properties().stacksTo(1)));
        public static final RegistryEntry<TreasureDiskItem> TREASURE_DISK =
            REGISTRY.register("treasure_disk", () -> new TreasureDiskItem(properties().stacksTo(1)));

        public static final RegistryEntry<PrintoutItem> PRINTED_PAGE = REGISTRY.register("printed_page",
            () -> new PrintoutItem(properties().stacksTo(1), PrintoutItem.Type.PAGE));
        public static final RegistryEntry<PrintoutItem> PRINTED_PAGES = REGISTRY.register("printed_pages",
            () -> new PrintoutItem(properties().stacksTo(1), PrintoutItem.Type.PAGES));
        public static final RegistryEntry<PrintoutItem> PRINTED_BOOK = REGISTRY.register("printed_book",
            () -> new PrintoutItem(properties().stacksTo(1), PrintoutItem.Type.BOOK));

        public static final RegistryEntry<BlockItem> SPEAKER = ofBlock(Blocks.SPEAKER, BlockItem::new);
        public static final RegistryEntry<BlockItem> DISK_DRIVE = ofBlock(Blocks.DISK_DRIVE, BlockItem::new);
        public static final RegistryEntry<BlockItem> PRINTER = ofBlock(Blocks.PRINTER, BlockItem::new);
        public static final RegistryEntry<BlockItem> MONITOR_NORMAL = ofBlock(Blocks.MONITOR_NORMAL, BlockItem::new);
        public static final RegistryEntry<BlockItem> MONITOR_ADVANCED = ofBlock(Blocks.MONITOR_ADVANCED, BlockItem::new);
        public static final RegistryEntry<BlockItem> WIRELESS_MODEM_NORMAL = ofBlock(Blocks.WIRELESS_MODEM_NORMAL, BlockItem::new);
        public static final RegistryEntry<BlockItem> WIRELESS_MODEM_ADVANCED = ofBlock(Blocks.WIRELESS_MODEM_ADVANCED, BlockItem::new);
        public static final RegistryEntry<BlockItem> WIRED_MODEM_FULL = ofBlock(Blocks.WIRED_MODEM_FULL, BlockItem::new);

        public static final RegistryEntry<CableBlockItem.Cable> CABLE = REGISTRY.register("cable",
            () -> new CableBlockItem.Cable(Blocks.CABLE.get(), properties()));
        public static final RegistryEntry<CableBlockItem.WiredModem> WIRED_MODEM = REGISTRY.register("wired_modem",
            () -> new CableBlockItem.WiredModem(Blocks.CABLE.get(), properties()));
    }

    public static final class DataComponents {
        static final RegistrationHelper<DataComponentType<?>> REGISTRY = PlatformHelper.get().createRegistrationHelper(Registries.DATA_COMPONENT_TYPE);

        private static <T> RegistryEntry<DataComponentType<T>> register(String name, UnaryOperator<DataComponentType.Builder<T>> unaryOperator) {
            return REGISTRY.register(name, () -> unaryOperator.apply(DataComponentType.builder()).build());
        }

        /**
         * The id of a computer.
         *
         * @see AbstractComputerItem
         * @see PocketComputerItem
         */
        public static final RegistryEntry<DataComponentType<NonNegativeId>> COMPUTER_ID = register("computer_id", b -> b
            .persistent(NonNegativeId.CODEC).networkSynchronized(NonNegativeId.STREAM_CODEC)
        );

        /**
         * The left upgrade of a turtle.
         *
         * @see TurtleItem
         */
        public static final RegistryEntry<DataComponentType<UpgradeData<ITurtleUpgrade>>> LEFT_TURTLE_UPGRADE = register("left_turtle_upgrade", b -> b
            .persistent(dan200.computercraft.impl.TurtleUpgrades.instance().upgradeDataCodec()).networkSynchronized(dan200.computercraft.impl.TurtleUpgrades.instance().upgradeDataStreamCodec())
        );

        /**
         * The right upgrade of a turtle.
         *
         * @see TurtleItem
         */
        public static final RegistryEntry<DataComponentType<UpgradeData<ITurtleUpgrade>>> RIGHT_TURTLE_UPGRADE = register("right_turtle_upgrade", b -> b
            .persistent(dan200.computercraft.impl.TurtleUpgrades.instance().upgradeDataCodec()).networkSynchronized(dan200.computercraft.impl.TurtleUpgrades.instance().upgradeDataStreamCodec())
        );

        /**
         * The fuel level of a turtle.
         */
        public static final RegistryEntry<DataComponentType<Integer>> FUEL = register("fuel", b -> b
            .persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT)
        );

        /**
         * The overlay on a turtle.
         */
        public static final RegistryEntry<DataComponentType<ResourceLocation>> OVERLAY = register("overlay", b -> b
            .persistent(ResourceLocation.CODEC).networkSynchronized(ResourceLocation.STREAM_CODEC)
        );

        /**
         * The back upgrade of a pocket computer.
         *
         * @see PocketComputerItem
         */
        public static final RegistryEntry<DataComponentType<UpgradeData<IPocketUpgrade>>> POCKET_UPGRADE = register("pocket_upgrade", b -> b
            .persistent(PocketUpgrades.instance().upgradeDataCodec()).networkSynchronized(PocketUpgrades.instance().upgradeDataStreamCodec())
        );

        /**
         * A reference to the currently running {@link dan200.computercraft.shared.computer.core.ServerComputer}.
         *
         * @see ServerComputerReference
         * @see PocketComputerItem
         */
        public static final RegistryEntry<DataComponentType<ServerComputerReference>> COMPUTER = register("computer", b -> b
            .persistent(ServerComputerReference.CODEC).networkSynchronized(ServerComputerReference.STREAM_CODEC)
        );

        /**
         * Whether this item is currently on.
         *
         * @see PocketComputerItem
         * @see TurtleModem
         */
        public static final RegistryEntry<DataComponentType<Boolean>> ON = register("on", b -> b
            .persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL)
        );

        /**
         * Information about a treasure disk's mount.
         *
         * @see TreasureDiskItem
         * @see TreasureDisk
         */
        public static final RegistryEntry<DataComponentType<TreasureDisk>> TREASURE_DISK = register("treasure_disk", b -> b
            .persistent(TreasureDisk.CODEC).networkSynchronized(TreasureDisk.STREAM_CODEC)
        );

        /**
         * The id of a disk.
         *
         * @see DiskItem
         */
        public static final RegistryEntry<DataComponentType<NonNegativeId>> DISK_ID = register("disk_id", b -> b
            .persistent(NonNegativeId.CODEC).networkSynchronized(NonNegativeId.STREAM_CODEC)
        );

        /**
         * The contents of a printed page/printed pages.
         *
         * @see PrintoutItem
         * @see PrintoutData
         */
        public static final RegistryEntry<DataComponentType<PrintoutData>> PRINTOUT = register("printout", b -> b
            .persistent(PrintoutData.CODEC).networkSynchronized(PrintoutData.STREAM_CODEC)
        );
    }

    public static class TurtleUpgradeTypes {
        static final RegistrationHelper<UpgradeType<? extends ITurtleUpgrade>> REGISTRY = PlatformHelper.get().createRegistrationHelper(ITurtleUpgrade.typeRegistry());

        public static final RegistryEntry<UpgradeType<TurtleSpeaker>> SPEAKER =
            REGISTRY.register("speaker", () -> UpgradeType.simpleWithCustomItem(TurtleSpeaker::new));
        public static final RegistryEntry<UpgradeType<TurtleCraftingTable>> WORKBENCH =
            REGISTRY.register("workbench", () -> UpgradeType.simpleWithCustomItem(TurtleCraftingTable::new));
        public static final RegistryEntry<UpgradeType<TurtleModem>> WIRELESS_MODEM_NORMAL =
            REGISTRY.register("wireless_modem_normal", () -> UpgradeType.simpleWithCustomItem(item -> new TurtleModem(item, false)));
        public static final RegistryEntry<UpgradeType<TurtleModem>> WIRELESS_MODEM_ADVANCED =
            REGISTRY.register("wireless_modem_advanced", () -> UpgradeType.simpleWithCustomItem(item -> new TurtleModem(item, true)));

        public static final RegistryEntry<UpgradeType<TurtleTool>> TOOL = REGISTRY.register("tool", () -> UpgradeType.create(TurtleTool.CODEC));
    }

    public static class PocketUpgradeTypes {
        static final RegistrationHelper<UpgradeType<? extends IPocketUpgrade>> REGISTRY = PlatformHelper.get().createRegistrationHelper(IPocketUpgrade.typeRegistry());

        public static final RegistryEntry<UpgradeType<PocketSpeaker>> SPEAKER =
            REGISTRY.register("speaker", () -> UpgradeType.simpleWithCustomItem(PocketSpeaker::new));
        public static final RegistryEntry<UpgradeType<PocketModem>> WIRELESS_MODEM_NORMAL =
            REGISTRY.register("wireless_modem_normal", () -> UpgradeType.simpleWithCustomItem(item -> new PocketModem(item, false)));
        public static final RegistryEntry<UpgradeType<PocketModem>> WIRELESS_MODEM_ADVANCED =
            REGISTRY.register("wireless_modem_advanced", () -> UpgradeType.simpleWithCustomItem(item -> new PocketModem(item, true)));
    }

    public static class Menus {
        static final RegistrationHelper<MenuType<?>> REGISTRY = PlatformHelper.get().createRegistrationHelper(Registries.MENU);

        public static final RegistryEntry<MenuType<ComputerMenuWithoutInventory>> COMPUTER = REGISTRY.register("computer",
            () -> ContainerData.toType(ComputerContainerData.STREAM_CODEC, (id, inv, data) -> new ComputerMenuWithoutInventory(Menus.COMPUTER.get(), id, inv, data)));

        public static final RegistryEntry<MenuType<ComputerMenuWithoutInventory>> POCKET_COMPUTER = REGISTRY.register("pocket_computer",
            () -> ContainerData.toType(ComputerContainerData.STREAM_CODEC, (id, inv, data) -> new ComputerMenuWithoutInventory(Menus.POCKET_COMPUTER.get(), id, inv, data)));

        public static final RegistryEntry<MenuType<ComputerMenuWithoutInventory>> POCKET_COMPUTER_NO_TERM = REGISTRY.register("pocket_computer_no_term",
            () -> ContainerData.toType(ComputerContainerData.STREAM_CODEC, (id, inv, data) -> new ComputerMenuWithoutInventory(Menus.POCKET_COMPUTER_NO_TERM.get(), id, inv, data)));

        public static final RegistryEntry<MenuType<TurtleMenu>> TURTLE = REGISTRY.register("turtle",
            () -> ContainerData.toType(ComputerContainerData.STREAM_CODEC, TurtleMenu::ofMenuData));

        public static final RegistryEntry<MenuType<DiskDriveMenu>> DISK_DRIVE = REGISTRY.register("disk_drive",
            () -> new MenuType<>(DiskDriveMenu::new, FeatureFlags.VANILLA_SET));

        public static final RegistryEntry<MenuType<PrinterMenu>> PRINTER = REGISTRY.register("printer",
            () -> new MenuType<>(PrinterMenu::new, FeatureFlags.VANILLA_SET));

        public static final RegistryEntry<MenuType<HeldItemMenu>> PRINTOUT = REGISTRY.register("printout",
            () -> ContainerData.toType(
                HeldItemContainerData.STREAM_CODEC,
                (id, inventory, data) -> new HeldItemMenu(Menus.PRINTOUT.get(), id, inventory.player, data.hand())
            ));

        public static final RegistryEntry<MenuType<ViewComputerMenu>> VIEW_COMPUTER = REGISTRY.register("view_computer",
            () -> ContainerData.toType(ComputerContainerData.STREAM_CODEC, ViewComputerMenu::new));
    }

    static class ArgumentTypes {
        static final RegistrationHelper<ArgumentTypeInfo<?, ?>> REGISTRY = PlatformHelper.get().createRegistrationHelper(Registries.COMMAND_ARGUMENT_TYPE);

        @SuppressWarnings("unchecked")
        private static <T extends ArgumentType<?>> void registerUnsafe(String name, Class<T> type, ArgumentTypeInfo<?, ?> serializer) {
            REGISTRY.register(name, () -> PlatformHelper.get().registerArgumentTypeInfo(type, (ArgumentTypeInfo<T, ?>) serializer));
        }

        private static <T extends ArgumentType<?>> void register(String name, Class<T> type, ArgumentTypeInfo<T, ?> serializer) {
            REGISTRY.register(name, () -> PlatformHelper.get().registerArgumentTypeInfo(type, serializer));
        }

        private static <T extends ArgumentType<?>> void register(String name, Class<T> type, T instance) {
            register(name, type, SingletonArgumentInfo.contextFree(() -> instance));
        }

        static {
            register("tracking_field", TrackingFieldArgumentType.class, TrackingFieldArgumentType.metric());
            register("computer", ComputerArgumentType.class, ComputerArgumentType.get());
            registerUnsafe("repeat", RepeatArgumentType.class, new RepeatArgumentType.Info());
        }
    }

    public static class LootItemConditionTypes {
        static final RegistrationHelper<LootItemConditionType> REGISTRY = PlatformHelper.get().createRegistrationHelper(Registries.LOOT_CONDITION_TYPE);

        public static final RegistryEntry<LootItemConditionType> BLOCK_NAMED = REGISTRY.register("block_named",
            () -> new LootItemConditionType(MapCodec.unit(BlockNamedEntityLootCondition.INSTANCE)));

        public static final RegistryEntry<LootItemConditionType> PLAYER_CREATIVE = REGISTRY.register("player_creative",
            () -> new LootItemConditionType(MapCodec.unit(PlayerCreativeLootCondition.INSTANCE)));

        public static final RegistryEntry<LootItemConditionType> HAS_ID = REGISTRY.register("has_id",
            () -> new LootItemConditionType(MapCodec.unit(HasComputerIdLootCondition.INSTANCE)));
    }

    public static class RecipeSerializers {
        static final RegistrationHelper<RecipeSerializer<?>> REGISTRY = PlatformHelper.get().createRegistrationHelper(Registries.RECIPE_SERIALIZER);

        private static <T extends CustomRecipe> RegistryEntry<SimpleCraftingRecipeSerializer<T>> simple(String name, SimpleCraftingRecipeSerializer.Factory<T> factory) {
            return REGISTRY.register(name, () -> new SimpleCraftingRecipeSerializer<>(factory));
        }

        private static <T extends Recipe<?>> RegistryEntry<RecipeSerializer<T>> register(String name, MapCodec<T> codec, StreamCodec<RegistryFriendlyByteBuf, T> streamCodec) {
            return REGISTRY.register(name, () -> new BasicRecipeSerialiser<>(codec, streamCodec));
        }

        public static final RegistryEntry<RecipeSerializer<ImpostorShapedRecipe>> IMPOSTOR_SHAPED = REGISTRY.register("impostor_shaped", () -> CustomShapedRecipe.serialiser(ImpostorShapedRecipe::new));
        public static final RegistryEntry<RecipeSerializer<ImpostorShapelessRecipe>> IMPOSTOR_SHAPELESS = REGISTRY.register("impostor_shapeless", () -> CustomShapelessRecipe.serialiser(ImpostorShapelessRecipe::new));

        public static final RegistryEntry<RecipeSerializer<TransformShapedRecipe>> TRANSFORM_SHAPED = register("transform_shaped", TransformShapedRecipe.CODEC, TransformShapedRecipe.STREAM_CODEC);
        public static final RegistryEntry<RecipeSerializer<TransformShapelessRecipe>> TRANSFORM_SHAPELESS = register("transform_shapeless", TransformShapelessRecipe.CODEC, TransformShapelessRecipe.STREAM_CODEC);

        public static final RegistryEntry<SimpleCraftingRecipeSerializer<ColourableRecipe>> DYEABLE_ITEM = simple("colour", ColourableRecipe::new);
        public static final RegistryEntry<SimpleCraftingRecipeSerializer<ClearColourRecipe>> DYEABLE_ITEM_CLEAR = simple("clear_colour", ClearColourRecipe::new);
        public static final RegistryEntry<SimpleCraftingRecipeSerializer<TurtleUpgradeRecipe>> TURTLE_UPGRADE = simple("turtle_upgrade", TurtleUpgradeRecipe::new);
        public static final RegistryEntry<SimpleCraftingRecipeSerializer<PocketComputerUpgradeRecipe>> POCKET_COMPUTER_UPGRADE = simple("pocket_computer_upgrade", PocketComputerUpgradeRecipe::new);
        public static final RegistryEntry<SimpleCraftingRecipeSerializer<PrintoutRecipe>> PRINTOUT = simple("printout", PrintoutRecipe::new);
        public static final RegistryEntry<SimpleCraftingRecipeSerializer<DiskRecipe>> DISK = simple("disk", DiskRecipe::new);
    }

    public static class RecipeFunctions {
        static final RegistrationHelper<RecipeFunction.Type<?>> REGISTRY = PlatformHelper.get().createRegistrationHelper(RecipeFunction.REGISTRY);

        private static <T extends RecipeFunction> RegistryEntry<RecipeFunction.Type<T>> register(String name, MapCodec<T> codec, StreamCodec<RegistryFriendlyByteBuf, T> streamCodec) {
            return REGISTRY.register(name, () -> new RecipeFunction.Type<>(codec, streamCodec));
        }

        public static final RegistryEntry<RecipeFunction.Type<CopyComponents>> COPY_COMPONENTS = register("copy_components", CopyComponents.CODEC, CopyComponents.STREAM_CODEC);
    }

    public static class Permissions {
        static final PermissionRegistry REGISTRY = PermissionRegistry.create();

        public static final Predicate<CommandSourceStack> PERMISSION_DUMP = REGISTRY.registerCommand("dump", UserLevel.OWNER_OP);
        public static final Predicate<CommandSourceStack> PERMISSION_SHUTDOWN = REGISTRY.registerCommand("shutdown", UserLevel.OWNER_OP);
        public static final Predicate<CommandSourceStack> PERMISSION_TURN_ON = REGISTRY.registerCommand("turn_on", UserLevel.OWNER_OP);
        public static final Predicate<CommandSourceStack> PERMISSION_TP = REGISTRY.registerCommand("tp", UserLevel.OP);
        public static final Predicate<CommandSourceStack> PERMISSION_TRACK = REGISTRY.registerCommand("track", UserLevel.OWNER_OP);
        public static final Predicate<CommandSourceStack> PERMISSION_QUEUE = REGISTRY.registerCommand("queue", UserLevel.ANYONE);
        public static final Predicate<CommandSourceStack> PERMISSION_VIEW = REGISTRY.registerCommand("view", UserLevel.OP);
    }

    static class CreativeTabs {
        static final RegistrationHelper<CreativeModeTab> REGISTRY = PlatformHelper.get().createRegistrationHelper(Registries.CREATIVE_MODE_TAB);

        @SuppressWarnings("unused")
        private static final RegistryEntry<CreativeModeTab> TAB = REGISTRY.register("tab", () -> PlatformHelper.get().newCreativeModeTab()
            .icon(() -> new ItemStack(Items.COMPUTER_NORMAL.get()))
            .title(Component.translatable("itemGroup.computercraft"))
            .displayItems((context, out) -> {
                out.accept(new ItemStack(Items.COMPUTER_NORMAL.get()));
                out.accept(new ItemStack(Items.COMPUTER_ADVANCED.get()));
                if (context.hasPermissions()) out.accept(new ItemStack(Items.COMPUTER_COMMAND.get()));
                addTurtle(out, Items.TURTLE_NORMAL.get(), context.holders());
                addTurtle(out, Items.TURTLE_ADVANCED.get(), context.holders());
                addPocket(out, Items.POCKET_COMPUTER_NORMAL.get(), context.holders());
                addPocket(out, Items.POCKET_COMPUTER_ADVANCED.get(), context.holders());

                out.accept(Items.WIRELESS_MODEM_NORMAL.get());
                out.accept(Items.WIRELESS_MODEM_ADVANCED.get());
                out.accept(Items.CABLE.get());
                out.accept(Items.WIRED_MODEM.get());
                out.accept(Items.WIRED_MODEM_FULL.get());

                out.accept(Items.MONITOR_NORMAL.get());
                out.accept(Items.MONITOR_ADVANCED.get());

                out.accept(Items.SPEAKER.get());

                out.accept(Items.PRINTER.get());
                out.accept(Items.PRINTED_PAGE.get());
                out.accept(Items.PRINTED_PAGES.get());
                out.accept(Items.PRINTED_BOOK.get());

                out.accept(Items.DISK_DRIVE.get());
                for (var colour = 0; colour < 16; colour++) {
                    out.accept(DataComponentUtil.createStack(Items.DISK.get(), net.minecraft.core.component.DataComponents.DYED_COLOR, new DyedItemColor(Colour.VALUES[colour].getHex(), false)));
                }
            })
            .build());
    }

    /**
     * Register any objects which don't have to be done on the main thread.
     */
    public static void register() {
        Blocks.REGISTRY.register();
        BlockEntities.REGISTRY.register();
        Items.REGISTRY.register();
        DataComponents.REGISTRY.register();
        TurtleUpgradeTypes.REGISTRY.register();
        PocketUpgradeTypes.REGISTRY.register();
        Menus.REGISTRY.register();
        ArgumentTypes.REGISTRY.register();
        LootItemConditionTypes.REGISTRY.register();
        RecipeSerializers.REGISTRY.register();
        RecipeFunctions.REGISTRY.register();
        Permissions.REGISTRY.register();
        CreativeTabs.REGISTRY.register();

        // Register bundled power providers
        ComputerCraftAPI.registerBundledRedstoneProvider(new DefaultBundledRedstoneProvider());
        ComputerCraftAPI.registerRefuelHandler(new FurnaceRefuelHandler());
        ComputerCraftAPI.registerMediaProvider(stack -> {
            var item = stack.getItem();
            if (item instanceof IMedia media) return media;
            if (item instanceof RecordItem) return RecordMedia.INSTANCE;
            return null;
        });

        VanillaDetailRegistries.ITEM_STACK.addProvider(ItemDetails::fill);
        VanillaDetailRegistries.BLOCK_IN_WORLD.addProvider(BlockDetails::fill);
    }

    /**
     * Register any objects which must be done on the main thread.
     */
    public static void registerMainThread() {
        CauldronInteraction.WATER.map().put(Items.TURTLE_NORMAL.get(), TurtleItem.CAULDRON_INTERACTION);
        CauldronInteraction.WATER.map().put(Items.TURTLE_ADVANCED.get(), TurtleItem.CAULDRON_INTERACTION);
    }

    private static void addTurtle(CreativeModeTab.Output out, TurtleItem turtle, HolderLookup.Provider registries) {
        out.accept(new ItemStack(turtle));
        registries.lookupOrThrow(TURTLE_UPGRADE).listElements()
            .filter(ModRegistry::isOurUpgrade)
            .map(x -> DataComponentUtil.createStack(turtle, DataComponents.RIGHT_TURTLE_UPGRADE.get(), UpgradeData.ofDefault(x)))
            .forEach(out::accept);
    }

    private static void addPocket(CreativeModeTab.Output out, PocketComputerItem pocket, HolderLookup.Provider registries) {
        out.accept(new ItemStack(pocket));
        registries.lookupOrThrow(POCKET_UPGRADE).listElements()
            .filter(ModRegistry::isOurUpgrade)
            .map(x -> DataComponentUtil.createStack(pocket, DataComponents.POCKET_UPGRADE.get(), UpgradeData.ofDefault(x))).forEach(out::accept);
    }

    private static boolean isOurUpgrade(Holder.Reference<? extends UpgradeBase> upgrade) {
        var namespace = upgrade.key().location().getNamespace();
        return namespace.equals("minecraft") || namespace.equals(ComputerCraftAPI.MOD_ID);
    }
}
