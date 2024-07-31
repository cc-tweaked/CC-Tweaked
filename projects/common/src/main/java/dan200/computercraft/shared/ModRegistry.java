// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared;

import com.mojang.brigadier.arguments.ArgumentType;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.component.ComputerComponents;
import dan200.computercraft.api.detail.DetailProvider;
import dan200.computercraft.api.detail.VanillaDetailRegistries;
import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.api.pocket.PocketUpgradeSerialiser;
import dan200.computercraft.api.turtle.TurtleUpgradeSerialiser;
import dan200.computercraft.api.upgrades.UpgradeData;
import dan200.computercraft.core.util.Colour;
import dan200.computercraft.impl.PocketUpgrades;
import dan200.computercraft.impl.TurtleUpgrades;
import dan200.computercraft.shared.command.UserLevel;
import dan200.computercraft.shared.command.arguments.ComputerArgumentType;
import dan200.computercraft.shared.command.arguments.RepeatArgumentType;
import dan200.computercraft.shared.command.arguments.TrackingFieldArgumentType;
import dan200.computercraft.shared.common.ClearColourRecipe;
import dan200.computercraft.shared.common.ColourableRecipe;
import dan200.computercraft.shared.common.DefaultBundledRedstoneProvider;
import dan200.computercraft.shared.common.HeldItemMenu;
import dan200.computercraft.shared.computer.apis.CommandAPI;
import dan200.computercraft.shared.computer.blocks.CommandComputerBlock;
import dan200.computercraft.shared.computer.blocks.ComputerBlock;
import dan200.computercraft.shared.computer.blocks.ComputerBlockEntity;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.inventory.ComputerMenuWithoutInventory;
import dan200.computercraft.shared.computer.items.CommandComputerItem;
import dan200.computercraft.shared.computer.items.ComputerItem;
import dan200.computercraft.shared.computer.recipe.ComputerUpgradeRecipe;
import dan200.computercraft.shared.config.Config;
import dan200.computercraft.shared.data.BlockNamedEntityLootCondition;
import dan200.computercraft.shared.data.ConstantLootConditionSerializer;
import dan200.computercraft.shared.data.HasComputerIdLootCondition;
import dan200.computercraft.shared.data.PlayerCreativeLootCondition;
import dan200.computercraft.shared.details.BlockDetails;
import dan200.computercraft.shared.details.ItemDetails;
import dan200.computercraft.shared.integration.PermissionRegistry;
import dan200.computercraft.shared.media.items.DiskItem;
import dan200.computercraft.shared.media.items.PrintoutItem;
import dan200.computercraft.shared.media.items.RecordMedia;
import dan200.computercraft.shared.media.items.TreasureDiskItem;
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
import dan200.computercraft.shared.pocket.apis.PocketAPI;
import dan200.computercraft.shared.pocket.items.PocketComputerItem;
import dan200.computercraft.shared.pocket.peripherals.PocketModem;
import dan200.computercraft.shared.pocket.peripherals.PocketSpeaker;
import dan200.computercraft.shared.pocket.recipes.PocketComputerUpgradeRecipe;
import dan200.computercraft.shared.recipe.CustomShapedRecipe;
import dan200.computercraft.shared.recipe.CustomShapelessRecipe;
import dan200.computercraft.shared.recipe.ImpostorShapedRecipe;
import dan200.computercraft.shared.recipe.ImpostorShapelessRecipe;
import dan200.computercraft.shared.turtle.FurnaceRefuelHandler;
import dan200.computercraft.shared.turtle.apis.TurtleAPI;
import dan200.computercraft.shared.turtle.blocks.TurtleBlock;
import dan200.computercraft.shared.turtle.blocks.TurtleBlockEntity;
import dan200.computercraft.shared.turtle.core.TurtleAccessInternal;
import dan200.computercraft.shared.turtle.inventory.TurtleMenu;
import dan200.computercraft.shared.turtle.items.TurtleItem;
import dan200.computercraft.shared.turtle.recipes.TurtleOverlayRecipe;
import dan200.computercraft.shared.turtle.recipes.TurtleRecipe;
import dan200.computercraft.shared.turtle.recipes.TurtleUpgradeRecipe;
import dan200.computercraft.shared.turtle.upgrades.*;
import dan200.computercraft.shared.util.ComponentMap;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * Registers ComputerCraft's registry entries and additional objects, such as {@link CauldronInteraction}s and
 * {@link DetailProvider}s
 * <p>
 * The functions in this class should be called from a loader-specific class.
 */
public final class ModRegistry {
    private ModRegistry() {
    }

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
        public static final RegistryEntry<ComputerBlock<ComputerBlockEntity>> COMPUTER_COMMAND = REGISTRY.register("computer_command",
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

        private static <T extends BlockEntity> RegistryEntry<BlockEntityType<T>> ofBlock(RegistryEntry<? extends Block> block, BiFunction<BlockPos, BlockState, T> factory) {
            return REGISTRY.register(block.id().getPath(), () -> PlatformHelper.get().createBlockEntityType(factory, block.get()));
        }

        public static final RegistryEntry<BlockEntityType<MonitorBlockEntity>> MONITOR_NORMAL =
            ofBlock(Blocks.MONITOR_NORMAL, (p, s) -> new MonitorBlockEntity(BlockEntities.MONITOR_NORMAL.get(), p, s, false));
        public static final RegistryEntry<BlockEntityType<MonitorBlockEntity>> MONITOR_ADVANCED =
            ofBlock(Blocks.MONITOR_ADVANCED, (p, s) -> new MonitorBlockEntity(BlockEntities.MONITOR_ADVANCED.get(), p, s, true));

        public static final RegistryEntry<BlockEntityType<ComputerBlockEntity>> COMPUTER_NORMAL =
            ofBlock(Blocks.COMPUTER_NORMAL, (p, s) -> new ComputerBlockEntity(BlockEntities.COMPUTER_NORMAL.get(), p, s, ComputerFamily.NORMAL));
        public static final RegistryEntry<BlockEntityType<ComputerBlockEntity>> COMPUTER_ADVANCED =
            ofBlock(Blocks.COMPUTER_ADVANCED, (p, s) -> new ComputerBlockEntity(BlockEntities.COMPUTER_ADVANCED.get(), p, s, ComputerFamily.ADVANCED));
        public static final RegistryEntry<BlockEntityType<ComputerBlockEntity>> COMPUTER_COMMAND =
            ofBlock(Blocks.COMPUTER_COMMAND, (p, s) -> new ComputerBlockEntity(BlockEntities.COMPUTER_COMMAND.get(), p, s, ComputerFamily.COMMAND));

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

    public static class TurtleSerialisers {
        static final RegistrationHelper<TurtleUpgradeSerialiser<?>> REGISTRY = PlatformHelper.get().createRegistrationHelper(TurtleUpgradeSerialiser.registryId());

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
        static final RegistrationHelper<PocketUpgradeSerialiser<?>> REGISTRY = PlatformHelper.get().createRegistrationHelper(PocketUpgradeSerialiser.registryId());

        public static final RegistryEntry<PocketUpgradeSerialiser<PocketSpeaker>> SPEAKER =
            REGISTRY.register("speaker", () -> PocketUpgradeSerialiser.simpleWithCustomItem(PocketSpeaker::new));
        public static final RegistryEntry<PocketUpgradeSerialiser<PocketModem>> WIRELESS_MODEM_NORMAL =
            REGISTRY.register("wireless_modem_normal", () -> PocketUpgradeSerialiser.simpleWithCustomItem((id, item) -> new PocketModem(id, item, false)));
        public static final RegistryEntry<PocketUpgradeSerialiser<PocketModem>> WIRELESS_MODEM_ADVANCED =
            REGISTRY.register("wireless_modem_advanced", () -> PocketUpgradeSerialiser.simpleWithCustomItem((id, item) -> new PocketModem(id, item, true)));
    }

    public static class Menus {
        static final RegistrationHelper<MenuType<?>> REGISTRY = PlatformHelper.get().createRegistrationHelper(Registries.MENU);

        public static final RegistryEntry<MenuType<ComputerMenuWithoutInventory>> COMPUTER = REGISTRY.register("computer",
            () -> ContainerData.toType(ComputerContainerData::new, (id, inv, data) -> new ComputerMenuWithoutInventory(Menus.COMPUTER.get(), id, inv, data)));

        public static final RegistryEntry<MenuType<ComputerMenuWithoutInventory>> POCKET_COMPUTER_NO_TERM = REGISTRY.register("pocket_computer_no_term",
            () -> ContainerData.toType(ComputerContainerData::new, (id, inv, data) -> new ComputerMenuWithoutInventory(Menus.POCKET_COMPUTER_NO_TERM.get(), id, inv, data)));

        public static final RegistryEntry<MenuType<TurtleMenu>> TURTLE = REGISTRY.register("turtle",
            () -> ContainerData.toType(ComputerContainerData::new, TurtleMenu::ofMenuData));

        public static final RegistryEntry<MenuType<DiskDriveMenu>> DISK_DRIVE = REGISTRY.register("disk_drive",
            () -> new MenuType<>(DiskDriveMenu::new, FeatureFlags.VANILLA_SET));

        public static final RegistryEntry<MenuType<PrinterMenu>> PRINTER = REGISTRY.register("printer",
            () -> new MenuType<>(PrinterMenu::new, FeatureFlags.VANILLA_SET));

        public static final RegistryEntry<MenuType<HeldItemMenu>> PRINTOUT = REGISTRY.register("printout",
            () -> ContainerData.toType(
                HeldItemContainerData::new,
                (id, inventory, data) -> new HeldItemMenu(Menus.PRINTOUT.get(), id, inventory.player, data.getHand())
            ));
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
            () -> ConstantLootConditionSerializer.type(BlockNamedEntityLootCondition.INSTANCE));

        public static final RegistryEntry<LootItemConditionType> PLAYER_CREATIVE = REGISTRY.register("player_creative",
            () -> ConstantLootConditionSerializer.type(PlayerCreativeLootCondition.INSTANCE));

        public static final RegistryEntry<LootItemConditionType> HAS_ID = REGISTRY.register("has_id",
            () -> ConstantLootConditionSerializer.type(HasComputerIdLootCondition.INSTANCE));
    }

    public static class RecipeSerializers {
        static final RegistrationHelper<RecipeSerializer<?>> REGISTRY = PlatformHelper.get().createRegistrationHelper(Registries.RECIPE_SERIALIZER);

        private static <T extends CustomRecipe> RegistryEntry<SimpleCraftingRecipeSerializer<T>> simple(String name, SimpleCraftingRecipeSerializer.Factory<T> factory) {
            return REGISTRY.register(name, () -> new SimpleCraftingRecipeSerializer<>(factory));
        }

        public static final RegistryEntry<RecipeSerializer<CustomShapedRecipe>> SHAPED = REGISTRY.register("shaped", () -> CustomShapedRecipe.serialiser(CustomShapedRecipe::new));
        public static final RegistryEntry<RecipeSerializer<CustomShapelessRecipe>> SHAPELESS = REGISTRY.register("shapeless", () -> CustomShapelessRecipe.serialiser(CustomShapelessRecipe::new));

        public static final RegistryEntry<RecipeSerializer<ImpostorShapedRecipe>> IMPOSTOR_SHAPED = REGISTRY.register("impostor_shaped", () -> CustomShapedRecipe.serialiser(ImpostorShapedRecipe::new));
        public static final RegistryEntry<RecipeSerializer<ImpostorShapelessRecipe>> IMPOSTOR_SHAPELESS = REGISTRY.register("impostor_shapeless", () -> CustomShapelessRecipe.serialiser(ImpostorShapelessRecipe::new));

        public static final RegistryEntry<SimpleCraftingRecipeSerializer<ColourableRecipe>> DYEABLE_ITEM = simple("colour", ColourableRecipe::new);
        public static final RegistryEntry<SimpleCraftingRecipeSerializer<ClearColourRecipe>> DYEABLE_ITEM_CLEAR = simple("clear_colour", ClearColourRecipe::new);
        public static final RegistryEntry<RecipeSerializer<TurtleRecipe>> TURTLE = REGISTRY.register("turtle", () -> TurtleRecipe.validatingSerialiser(TurtleRecipe::of));
        public static final RegistryEntry<SimpleCraftingRecipeSerializer<TurtleUpgradeRecipe>> TURTLE_UPGRADE = simple("turtle_upgrade", TurtleUpgradeRecipe::new);
        public static final RegistryEntry<RecipeSerializer<TurtleOverlayRecipe>> TURTLE_OVERLAY = REGISTRY.register("turtle_overlay", TurtleOverlayRecipe.Serialiser::new);
        public static final RegistryEntry<SimpleCraftingRecipeSerializer<PocketComputerUpgradeRecipe>> POCKET_COMPUTER_UPGRADE = simple("pocket_computer_upgrade", PocketComputerUpgradeRecipe::new);
        public static final RegistryEntry<SimpleCraftingRecipeSerializer<PrintoutRecipe>> PRINTOUT = simple("printout", PrintoutRecipe::new);
        public static final RegistryEntry<SimpleCraftingRecipeSerializer<DiskRecipe>> DISK = simple("disk", DiskRecipe::new);
        public static final RegistryEntry<RecipeSerializer<ComputerUpgradeRecipe>> COMPUTER_UPGRADE = REGISTRY.register("computer_upgrade", () -> CustomShapedRecipe.validatingSerialiser(ComputerUpgradeRecipe::of));
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
                addTurtle(out, Items.TURTLE_NORMAL.get());
                addTurtle(out, Items.TURTLE_ADVANCED.get());
                addPocket(out, Items.POCKET_COMPUTER_NORMAL.get());
                addPocket(out, Items.POCKET_COMPUTER_ADVANCED.get());

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
                    out.accept(DiskItem.createFromIDAndColour(-1, null, Colour.VALUES[colour].getHex()));
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
        TurtleSerialisers.REGISTRY.register();
        PocketUpgradeSerialisers.REGISTRY.register();
        Menus.REGISTRY.register();
        ArgumentTypes.REGISTRY.register();
        LootItemConditionTypes.REGISTRY.register();
        RecipeSerializers.REGISTRY.register();
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

        ComputerCraftAPI.registerAPIFactory(computer -> {
            var turtle = computer.getComponent(ComputerComponents.TURTLE);
            var metrics = Objects.requireNonNull(computer.getComponent(ComponentMap.METRICS));
            return turtle == null ? null : new TurtleAPI(metrics, (TurtleAccessInternal) turtle);
        });

        ComputerCraftAPI.registerAPIFactory(computer -> {
            var pocket = computer.getComponent(ComputerComponents.POCKET);
            return pocket == null ? null : new PocketAPI(pocket);
        });

        ComputerCraftAPI.registerAPIFactory(computer -> {
            var admin = computer.getComponent(ComputerComponents.ADMIN_COMPUTER);
            return admin == null ? null : new CommandAPI(computer, admin);
        });

        VanillaDetailRegistries.ITEM_STACK.addProvider(ItemDetails::fill);
        VanillaDetailRegistries.BLOCK_IN_WORLD.addProvider(BlockDetails::fill);
    }

    /**
     * Register any objects which must be done on the main thread.
     */
    public static void registerMainThread() {
        CauldronInteraction.WATER.put(Items.TURTLE_NORMAL.get(), TurtleItem.CAULDRON_INTERACTION);
        CauldronInteraction.WATER.put(Items.TURTLE_ADVANCED.get(), TurtleItem.CAULDRON_INTERACTION);
    }

    private static void addTurtle(CreativeModeTab.Output out, TurtleItem turtle) {
        out.accept(turtle.create(-1, null, -1, null, null, 0, null));
        TurtleUpgrades.getVanillaUpgrades()
            .map(x -> turtle.create(-1, null, -1, null, UpgradeData.ofDefault(x), 0, null))
            .forEach(out::accept);
    }

    private static void addPocket(CreativeModeTab.Output out, PocketComputerItem pocket) {
        out.accept(pocket.create(-1, null, -1, null));
        PocketUpgrades.getVanillaUpgrades().map(x -> pocket.create(-1, null, -1, UpgradeData.ofDefault(x))).forEach(out::accept);
    }
}
