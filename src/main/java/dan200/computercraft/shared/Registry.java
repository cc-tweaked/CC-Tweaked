/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared;

import static net.minecraft.util.registry.Registry.BLOCK_ENTITY_TYPE;

import java.util.function.BiFunction;
import java.util.function.Function;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.common.ContainerHeldItem;
import dan200.computercraft.shared.computer.blocks.BlockComputer;
import dan200.computercraft.shared.computer.blocks.TileCommandComputer;
import dan200.computercraft.shared.computer.blocks.TileComputer;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.inventory.ContainerComputer;
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
import dan200.computercraft.shared.peripheral.modem.wired.BlockCable;
import dan200.computercraft.shared.peripheral.modem.wired.BlockWiredModemFull;
import dan200.computercraft.shared.peripheral.modem.wired.ItemBlockCable;
import dan200.computercraft.shared.peripheral.modem.wired.TileCable;
import dan200.computercraft.shared.peripheral.modem.wired.TileWiredModemFull;
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
import dan200.computercraft.shared.turtle.blocks.BlockTurtle;
import dan200.computercraft.shared.turtle.blocks.TileTurtle;
import dan200.computercraft.shared.turtle.core.TurtlePlayer;
import dan200.computercraft.shared.turtle.inventory.ContainerTurtle;
import dan200.computercraft.shared.turtle.items.ItemTurtle;
import dan200.computercraft.shared.util.CreativeTabMain;
import dan200.computercraft.shared.util.FixedPointTileEntityType;

import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public final class Registry {
    public static final String MOD_ID = ComputerCraft.MOD_ID;

    public static void init() {
        Object[] o = {ModBlocks.CABLE, ModTiles.CABLE, ModItems.CABLE, ModEntities.TURTLE_PLAYER, ModContainers.COMPUTER};
    }
    public static final class ModBlocks {
        private static Block.Settings properties() {
            return Block.Settings.of(Material.STONE)
                                 .strength(2);
        }

        private static Block.Settings turtleProperties() {
            return Block.Settings.of(Material.STONE)
                                 .strength(2.5f);
        }

        private static Block.Settings emProperties() {
            return Block.Settings.of(Material.STONE)
                                 .strength(1.5f);
        }

        public static final BlockComputer COMPUTER_NORMAL = net.minecraft.util.registry.Registry.register(net.minecraft.util.registry.Registry.BLOCK,
                                                                                                          new Identifier(MOD_ID, "computer_normal"),
                                                                                                          new BlockComputer(properties(), ComputerFamily.NORMAL, ModTiles.COMPUTER_NORMAL));
        public static final BlockComputer COMPUTER_ADVANCED = net.minecraft.util.registry.Registry.register(net.minecraft.util.registry.Registry.BLOCK,
                                                                                                            "computer_advanced",
                                                                                                            new BlockComputer(properties(),
                                                                                                                              ComputerFamily.ADVANCED,
                                                                                                                              ModTiles.COMPUTER_ADVANCED));

        public static final BlockComputer COMPUTER_COMMAND = net.minecraft.util.registry.Registry.register(net.minecraft.util.registry.Registry.BLOCK,
                                                                                                           "computer_command",
                                                                                                           new BlockComputer(Block.Settings.of(Material.STONE)
                                                                                                                                           .strength(-1, 6000000.0F),
                                                                                                                             ComputerFamily.COMMAND,
                                                                                                                             ModTiles.COMPUTER_COMMAND));

        public static final BlockTurtle TURTLE_NORMAL = net.minecraft.util.registry.Registry.register(net.minecraft.util.registry.Registry.BLOCK,
                                                                                                      "turtle_normal",
                                                                                                      new BlockTurtle(turtleProperties(), ComputerFamily.NORMAL, ModTiles.TURTLE_NORMAL));
        public static final BlockTurtle TURTLE_ADVANCED = net.minecraft.util.registry.Registry.register(net.minecraft.util.registry.Registry.BLOCK,
                                                                                                        "turtle_advanced",
                                                                                                        new BlockTurtle(turtleProperties(),
                                                                                                                        ComputerFamily.ADVANCED,
                                                                                                                        ModTiles.TURTLE_ADVANCED));

        public static final BlockSpeaker SPEAKER = net.minecraft.util.registry.Registry.register(net.minecraft.util.registry.Registry.BLOCK, "speaker",
                                                                                                 new BlockSpeaker(properties()));
        public static final BlockDiskDrive DISK_DRIVE = net.minecraft.util.registry.Registry.register(net.minecraft.util.registry.Registry.BLOCK, "disk_drive", new BlockDiskDrive(properties()));
        public static final BlockPrinter PRINTER = net.minecraft.util.registry.Registry.register(net.minecraft.util.registry.Registry.BLOCK, "printer", new BlockPrinter(properties()));

        public static final BlockMonitor MONITOR_NORMAL = net.minecraft.util.registry.Registry.register(net.minecraft.util.registry.Registry.BLOCK,
                                                                                                        "monitor_normal",
                                                                                                        new BlockMonitor(properties(), ModTiles.MONITOR_NORMAL));
        public static final BlockMonitor MONITOR_ADVANCED = net.minecraft.util.registry.Registry.register(net.minecraft.util.registry.Registry.BLOCK,
                                                                                                          "monitor_advanced",
                                                                                                          new BlockMonitor(properties(), ModTiles.MONITOR_ADVANCED));

        public static final BlockWirelessModem WIRELESS_EM_NORMAL = net.minecraft.util.registry.Registry.register(net.minecraft.util.registry.Registry.BLOCK,
                                                                                                                  "wireless_em_normal",
                                                                                                                  new BlockWirelessModem(properties(), ModTiles.WIRELESS_MODEM_NORMAL));
        public static final BlockWirelessModem WIRELESS_EM_ADVANCED = net.minecraft.util.registry.Registry.register(net.minecraft.util.registry.Registry.BLOCK,
                                                                                                                    "wireless_em_advanced",
                                                                                                                    new BlockWirelessModem(properties(),
                                                                                                                                           ModTiles.WIRELESS_MODEM_ADVANCED));

        public static final BlockWiredModemFull WIRED_MODEM_FULL = net.minecraft.util.registry.Registry.register(net.minecraft.util.registry.Registry.BLOCK,
                                                                                                                 "wired_em_full",
                                                                                                                 new BlockWiredModemFull(emProperties(), ModTiles.WIRED_MODEM_FULL));
        public static final BlockCable CABLE = net.minecraft.util.registry.Registry.register(net.minecraft.util.registry.Registry.BLOCK, "cable", new BlockCable(emProperties()));
    }

    public static class ModTiles {

        private static <T extends BlockEntity> BlockEntityType<T> ofBlock(Block block, Function<BlockEntityType<T>, T> factory) {
            return net.minecraft.util.registry.Registry.register(BLOCK_ENTITY_TYPE, net.minecraft.util.registry.Registry.BLOCK.getId(block), FixedPointTileEntityType.create(block, factory));
        }

        public static final BlockEntityType<TileMonitor> MONITOR_NORMAL = ofBlock(ModBlocks.MONITOR_NORMAL, f -> new TileMonitor(f, false));
        public static final BlockEntityType<TileMonitor> MONITOR_ADVANCED = ofBlock(ModBlocks.MONITOR_ADVANCED, f -> new TileMonitor(f, true));

        public static final BlockEntityType<TileComputer> COMPUTER_NORMAL = ofBlock(ModBlocks.COMPUTER_NORMAL,
                                                                                    f -> new TileComputer(ComputerFamily.NORMAL, f));
        public static final BlockEntityType<TileComputer> COMPUTER_ADVANCED = ofBlock(ModBlocks.COMPUTER_ADVANCED,
                                                                                      f -> new TileComputer(ComputerFamily.ADVANCED, f));
        public static final BlockEntityType<TileCommandComputer> COMPUTER_COMMAND = ofBlock(ModBlocks.COMPUTER_COMMAND,
                                                                                            f -> new TileCommandComputer(ComputerFamily.COMMAND, f));

        public static final BlockEntityType<TileTurtle> TURTLE_NORMAL = ofBlock(ModBlocks.TURTLE_NORMAL, f -> new TileTurtle(f, ComputerFamily.NORMAL));
        public static final BlockEntityType<TileTurtle> TURTLE_ADVANCED = ofBlock(ModBlocks.TURTLE_ADVANCED, f -> new TileTurtle(f, ComputerFamily.ADVANCED));

        public static final BlockEntityType<TileSpeaker> SPEAKER = ofBlock(ModBlocks.SPEAKER, TileSpeaker::new);
        public static final BlockEntityType<TileDiskDrive> DISK_DRIVE = ofBlock(ModBlocks.DISK_DRIVE, TileDiskDrive::new);
        public static final BlockEntityType<TilePrinter> PRINTER = ofBlock(ModBlocks.PRINTER, TilePrinter::new);
        public static final BlockEntityType<TileWiredModemFull> WIRED_MODEM_FULL = ofBlock(ModBlocks.WIRED_MODEM_FULL, TileWiredModemFull::new);
        public static final BlockEntityType<TileCable> CABLE = ofBlock(ModBlocks.CABLE, TileCable::new);

        public static final BlockEntityType<TileWirelessModem> WIRELESS_MODEM_NORMAL = ofBlock(ModBlocks.WIRELESS_EM_NORMAL, f -> new TileWirelessModem(f,
                                                                                                                                                        false));
        public static final BlockEntityType<TileWirelessModem> WIRELESS_MODEM_ADVANCED = ofBlock(ModBlocks.WIRELESS_EM_ADVANCED, f -> new TileWirelessModem(f, true));
    }

    public static final class ModItems {
        private static final ItemGroup mainItemGroup = new CreativeTabMain();

        private static Item.Settings properties() {
            return new Item.Settings().group(mainItemGroup);
        }

        private static <B extends Block, I extends Item> I ofBlock(B parent, BiFunction<B, Item.Settings, I> supplier) {
            return net.minecraft.util.registry.Registry.register(net.minecraft.util.registry.Registry.ITEM, net.minecraft.util.registry.Registry.BLOCK.getId(parent), supplier.apply(parent, properties()));
        }

        public static final ItemComputer COMPUTER_NORMAL = ofBlock(ModBlocks.COMPUTER_NORMAL, ItemComputer::new);
        public static final ItemComputer COMPUTER_ADVANCED = ofBlock(ModBlocks.COMPUTER_ADVANCED, ItemComputer::new);
        public static final ItemComputer COMPUTER_COMMAND = ofBlock(ModBlocks.COMPUTER_COMMAND, ItemComputer::new);
        private static <T extends Item> T register(String id, T item) {
            return net.minecraft.util.registry.Registry.register(net.minecraft.util.registry.Registry.ITEM, new Identifier(MOD_ID, id), item);
        }

        public static final ItemPocketComputer POCKET_COMPUTER_NORMAL = register("pocket_computer_normal",
                                                                                 new ItemPocketComputer(properties().maxCount(1),
                                                                                                        ComputerFamily.NORMAL));
        public static final ItemPocketComputer POCKET_COMPUTER_ADVANCED = register("pocket_computer_advanced",
                                                                                   new ItemPocketComputer(properties().maxCount(1),
                                                                                                          ComputerFamily.ADVANCED));

        public static final ItemTurtle TURTLE_NORMAL = ofBlock(ModBlocks.TURTLE_NORMAL, ItemTurtle::new);
        public static final ItemTurtle TURTLE_ADVANCED = ofBlock(ModBlocks.TURTLE_ADVANCED, ItemTurtle::new);

        public static final ItemDisk DISK = register("disk", new ItemDisk(properties().maxCount(1)));
        public static final ItemTreasureDisk TREASURE_DISK = register("treasure_disk", new ItemTreasureDisk(properties().maxCount(1)));

        public static final ItemPrintout PRINTED_PAGE = register("printed_page", new ItemPrintout(properties().maxCount(1), ItemPrintout.Type.PAGE));
        public static final ItemPrintout PRINTED_PAGES = register("printed_pages",
                                                                  new ItemPrintout(properties().maxCount(1), ItemPrintout.Type.PAGES));
        public static final ItemPrintout PRINTED_BOOK = register("printed_book", new ItemPrintout(properties().maxCount(1), ItemPrintout.Type.BOOK));

        public static final BlockItem SPEAKER = ofBlock(ModBlocks.SPEAKER, BlockItem::new);
        public static final BlockItem DISK_DRIVE = ofBlock(ModBlocks.DISK_DRIVE, BlockItem::new);
        public static final BlockItem PRINTER = ofBlock(ModBlocks.PRINTER, BlockItem::new);
        public static final BlockItem MONITOR_NORMAL = ofBlock(ModBlocks.MONITOR_NORMAL, BlockItem::new);
        public static final BlockItem MONITOR_ADVANCED = ofBlock(ModBlocks.MONITOR_ADVANCED, BlockItem::new);
        public static final BlockItem WIRELESS_EM_NORMAL = ofBlock(ModBlocks.WIRELESS_EM_NORMAL, BlockItem::new);
        public static final BlockItem WIRELESS_EM_ADVANCED = ofBlock(ModBlocks.WIRELESS_EM_ADVANCED, BlockItem::new);
        public static final BlockItem WIRED_EM_FULL = ofBlock(ModBlocks.WIRED_MODEM_FULL, BlockItem::new);

        public static final ItemBlockCable.Cable CABLE = register("cable", new ItemBlockCable.Cable(ModBlocks.CABLE, properties()));
        public static final ItemBlockCable.WiredModem WIRED_EM = register("wired_em", new ItemBlockCable.WiredModem(ModBlocks.CABLE, properties()));
    }

    public static class ModEntities {
        public static final EntityType<TurtlePlayer> TURTLE_PLAYER = net.minecraft.util.registry.Registry.register(net.minecraft.util.registry.Registry.ENTITY_TYPE, new Identifier(MOD_ID, "turtle_player"), EntityType.Builder.<TurtlePlayer>create(
            SpawnGroup.MISC).disableSaving()
                            .disableSummon()
                            .setDimensions(
                                0,
                                0)
                            .build(
                                ComputerCraft.MOD_ID + ":turtle_player"));
    }

    public static class ModContainers {
        private static <B extends ScreenHandler, T extends ScreenHandlerType<B>> T register(String id, T item) {
            return net.minecraft.util.registry.Registry.register(net.minecraft.util.registry.Registry.SCREEN_HANDLER, new Identifier(MOD_ID, id), item);
        }

        public static final ScreenHandlerType<ContainerComputer> COMPUTER = register("computer",
                                                                                     ContainerData.toType(ComputerContainerData::new,
                                                                                                          ContainerComputer::new));

        public static final ScreenHandlerType<ContainerPocketComputer> POCKET_COMPUTER = register("pocket_computer",
                                                                                                  ContainerData.toType(ComputerContainerData::new,
                                                                                                                       ContainerPocketComputer::new));

        public static final ScreenHandlerType<ContainerTurtle> TURTLE = register("turtle",
                                                                                 ContainerData.toType(ComputerContainerData::new,
                                                                                                      ContainerTurtle::new));


        public static final ScreenHandlerType<ContainerDiskDrive> DISK_DRIVE = register("disk_drive",
                                                                                        new ScreenHandlerType<>(ContainerDiskDrive::new));

        public static final ScreenHandlerType<ContainerPrinter> PRINTER = register("printer", new ScreenHandlerType<>(ContainerPrinter::new));

        public static final ScreenHandlerType<ContainerHeldItem> PRINTOUT = register("printout",
                                                                                     ContainerData.toType(HeldItemContainerData::new,
                                                                                                          ContainerHeldItem::createPrintout));

        public static final ScreenHandlerType<ContainerViewComputer> VIEW_COMPUTER = register("view_computer",
                                                                                              ContainerData.toType(ViewComputerContainerData::new,
                                                                                                                   ContainerViewComputer::new));
    }
}
