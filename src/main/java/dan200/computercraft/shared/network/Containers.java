/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.network;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.common.ContainerHeldItem;
import dan200.computercraft.shared.computer.blocks.TileComputer;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.inventory.ContainerComputer;
import dan200.computercraft.shared.computer.inventory.ContainerViewComputer;
import dan200.computercraft.shared.media.items.ItemPrintout;
import dan200.computercraft.shared.network.container.ContainerType;
import dan200.computercraft.shared.network.container.PocketComputerContainerType;
import dan200.computercraft.shared.network.container.PrintoutContainerType;
import dan200.computercraft.shared.network.container.TileEntityContainerType;
import dan200.computercraft.shared.network.container.ViewComputerContainerType;
import dan200.computercraft.shared.peripheral.diskdrive.ContainerDiskDrive;
import dan200.computercraft.shared.peripheral.diskdrive.TileDiskDrive;
import dan200.computercraft.shared.peripheral.printer.ContainerPrinter;
import dan200.computercraft.shared.peripheral.printer.TilePrinter;
import dan200.computercraft.shared.pocket.inventory.ContainerPocketComputer;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import dan200.computercraft.shared.turtle.blocks.TileTurtle;
import dan200.computercraft.shared.turtle.inventory.ContainerTurtle;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

public final class Containers {
    private Containers() {
    }

    public static void openDiskDriveGUI(PlayerEntity player, TileDiskDrive drive) {
        TileEntityContainerType.diskDrive(drive.getPos())
                               .open(player);
    }

    public static void openComputerGUI(PlayerEntity player, TileComputer computer) {
        computer.createServerComputer()
                .sendTerminalState(player);
        TileEntityContainerType.computer(computer.getPos())
                               .open(player);
    }

    public static void openPrinterGUI(PlayerEntity player, TilePrinter printer) {
        TileEntityContainerType.printer(printer.getPos())
                               .open(player);
    }

    public static void openTurtleGUI(PlayerEntity player, TileTurtle turtle) {
        turtle.createServerComputer()
              .sendTerminalState(player);
        TileEntityContainerType.turtle(turtle.getPos())
                               .open(player);
    }

    public static void openPrintoutGUI(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        Item item = stack.getItem();
        if (!(item instanceof ItemPrintout)) {
            return;
        }

        new PrintoutContainerType(hand).open(player);
    }

    public static void openPocketComputerGUI(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        Item item = stack.getItem();
        if (!(item instanceof ItemPocketComputer)) {
            return;
        }

        ServerComputer computer = ItemPocketComputer.getServerComputer(stack);
        if (computer != null) {
            computer.sendTerminalState(player);
        }

        new PocketComputerContainerType(hand).open(player);
    }

    public static void openComputerGUI(PlayerEntity player, ServerComputer computer) {
        computer.sendTerminalState(player);
        new ViewComputerContainerType(computer).open(player);
    }

    public static void setup() {
        ContainerType.register(TileEntityContainerType::computer,
                               (id, packet, player) -> new ContainerComputer(id, (TileComputer) packet.getTileEntity(player)));
        ContainerType.register(TileEntityContainerType::turtle, (id, packet, player) -> {
            TileTurtle turtle = (TileTurtle) packet.getTileEntity(player);
            return new ContainerTurtle(id, player.inventory, turtle.getAccess(), turtle.getServerComputer());
        });
        ContainerType.register(TileEntityContainerType::diskDrive,
                               (id, packet, player) -> new ContainerDiskDrive(id, player.inventory, (TileDiskDrive) packet.getTileEntity(player)));
        ContainerType.register(TileEntityContainerType::printer,
                               (id, packet, player) -> new ContainerPrinter(id, player.inventory, (TilePrinter) packet.getTileEntity(player)));

        ContainerType.register(PocketComputerContainerType::new, (id, packet, player) -> new ContainerPocketComputer(id, player, packet.hand));
        ContainerType.register(PrintoutContainerType::new, (id, packet, player) -> new ContainerHeldItem(id, player, packet.hand));
        ContainerType.register(ViewComputerContainerType::new,
                               (id, packet, player) -> new ContainerViewComputer(id, ComputerCraft.serverComputerRegistry.get(packet.instanceId)));
    }
}
