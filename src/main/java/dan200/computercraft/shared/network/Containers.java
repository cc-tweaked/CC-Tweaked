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
import dan200.computercraft.shared.network.container.*;
import dan200.computercraft.shared.peripheral.diskdrive.ContainerDiskDrive;
import dan200.computercraft.shared.peripheral.diskdrive.TileDiskDrive;
import dan200.computercraft.shared.peripheral.printer.ContainerPrinter;
import dan200.computercraft.shared.peripheral.printer.TilePrinter;
import dan200.computercraft.shared.pocket.inventory.ContainerPocketComputer;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import dan200.computercraft.shared.turtle.blocks.TileTurtle;
import dan200.computercraft.shared.turtle.inventory.ContainerTurtle;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;

public final class Containers
{
    private Containers()
    {
    }

    public static void openDiskDriveGUI( EntityPlayer player, TileDiskDrive drive )
    {
        TileEntityContainerType.diskDrive( drive.getPos() ).open( player );
    }

    public static void openComputerGUI( EntityPlayer player, TileComputer computer )
    {
        TileEntityContainerType.computer( computer.getPos() ).open( player );
    }

    public static void openPrinterGUI( EntityPlayer player, TilePrinter printer )
    {
        TileEntityContainerType.printer( printer.getPos() ).open( player );
    }

    public static void openTurtleGUI( EntityPlayer player, TileTurtle turtle )
    {
        TileEntityContainerType.turtle( turtle.getPos() ).open( player );
    }

    public static void openPrintoutGUI( EntityPlayer player, EnumHand hand )
    {
        ItemStack stack = player.getHeldItem( hand );
        Item item = stack.getItem();
        if( !(item instanceof ItemPrintout) ) return;

        new PrintoutContainerType( hand ).open( player );
    }

    public static void openPocketComputerGUI( EntityPlayer player, EnumHand hand )
    {
        ItemStack stack = player.getHeldItem( hand );
        Item item = stack.getItem();
        if( !(item instanceof ItemPocketComputer) ) return;

        new PocketComputerContainerType( hand ).open( player );
    }

    public static void openComputerGUI( EntityPlayer player, ServerComputer computer )
    {
        new ViewComputerContainerType( computer ).open( player );
    }

    public static void setup()
    {
        ContainerType.register( TileEntityContainerType::computer, ( packet, player ) ->
            new ContainerComputer( (TileComputer) packet.getTileEntity( player ) ) );
        ContainerType.register( TileEntityContainerType::turtle, ( packet, player ) -> {
            TileTurtle turtle = (TileTurtle) packet.getTileEntity( player );
            return new ContainerTurtle( player.inventory, turtle.getAccess(), turtle.getServerComputer() );
        } );
        ContainerType.register( TileEntityContainerType::diskDrive, ( packet, player ) ->
            new ContainerDiskDrive( player.inventory, (TileDiskDrive) packet.getTileEntity( player ) ) );
        ContainerType.register( TileEntityContainerType::printer, ( packet, player ) ->
            new ContainerPrinter( player.inventory, (TilePrinter) packet.getTileEntity( player ) ) );

        ContainerType.register( PocketComputerContainerType::new, ( packet, player ) -> new ContainerPocketComputer( player, packet.hand ) );
        ContainerType.register( PrintoutContainerType::new, ( packet, player ) -> new ContainerHeldItem( player, packet.hand ) );
        ContainerType.register( ViewComputerContainerType::new, ( packet, player ) -> new ContainerViewComputer( ComputerCraft.serverComputerRegistry.get( packet.instanceId ) ) );
    }
}
