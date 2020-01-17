/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.gui.*;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.computer.blocks.TileComputer;
import dan200.computercraft.shared.computer.core.ClientComputer;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.inventory.ContainerComputer;
import dan200.computercraft.shared.computer.inventory.ContainerViewComputer;
import dan200.computercraft.shared.media.inventory.ContainerHeldItem;
import dan200.computercraft.shared.media.items.ItemPrintout;
import dan200.computercraft.shared.peripheral.diskdrive.ContainerDiskDrive;
import dan200.computercraft.shared.peripheral.diskdrive.TileDiskDrive;
import dan200.computercraft.shared.peripheral.printer.ContainerPrinter;
import dan200.computercraft.shared.peripheral.printer.TilePrinter;
import dan200.computercraft.shared.pocket.inventory.ContainerPocketComputer;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import dan200.computercraft.shared.turtle.blocks.TileTurtle;
import dan200.computercraft.shared.turtle.inventory.ContainerTurtle;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public final class Containers implements IGuiHandler
{
    public static final Containers INSTANCE = new Containers();

    private static final int DISK_DRIVE = 100;
    private static final int COMPUTER = 101;
    private static final int PRINTER = 102;
    private static final int TURTLE = 103;
    private static final int PRINTOUT = 105;
    private static final int POCKET_COMPUTER = 106;
    private static final int VIEW_COMPUTER = 110;

    private Containers()
    {
    }

    public static void openDiskDriveGUI( EntityPlayer player, TileDiskDrive drive )
    {
        BlockPos pos = drive.getPos();
        player.openGui( ComputerCraft.instance, DISK_DRIVE, player.getEntityWorld(), pos.getX(), pos.getY(), pos.getZ() );
    }

    public static void openComputerGUI( EntityPlayer player, TileComputer computer )
    {
        BlockPos pos = computer.getPos();
        player.openGui( ComputerCraft.instance, COMPUTER, player.getEntityWorld(), pos.getX(), pos.getY(), pos.getZ() );
    }

    public static void openPrinterGUI( EntityPlayer player, TilePrinter printer )
    {
        BlockPos pos = printer.getPos();
        player.openGui( ComputerCraft.instance, PRINTER, player.getEntityWorld(), pos.getX(), pos.getY(), pos.getZ() );
    }

    public static void openTurtleGUI( EntityPlayer player, TileTurtle turtle )
    {
        BlockPos pos = turtle.getPos();
        player.openGui( ComputerCraft.instance, TURTLE, player.getEntityWorld(), pos.getX(), pos.getY(), pos.getZ() );
    }

    public static void openPrintoutGUI( EntityPlayer player, EnumHand hand )
    {
        player.openGui( ComputerCraft.instance, PRINTOUT, player.getEntityWorld(), hand.ordinal(), 0, 0 );
    }

    public static void openPocketComputerGUI( EntityPlayer player, EnumHand hand )
    {
        player.openGui( ComputerCraft.instance, POCKET_COMPUTER, player.getEntityWorld(), hand.ordinal(), 0, 0 );
    }

    public static void openComputerGUI( EntityPlayer player, ServerComputer computer )
    {
        ComputerFamily family = computer.getFamily();
        int width = 0, height = 0;
        Terminal terminal = computer.getTerminal();
        if( terminal != null )
        {
            width = terminal.getWidth();
            height = terminal.getHeight();
        }

        // Pack useful terminal information into the various coordinate bits.
        // These are extracted in ComputerCraftProxyCommon.getClientGuiElement
        player.openGui( ComputerCraft.instance, VIEW_COMPUTER, player.getEntityWorld(),
            computer.getInstanceID(), family.ordinal(), (width & 0xFFFF) << 16 | (height & 0xFFFF)
        );
    }

    @Override
    public Object getServerGuiElement( int id, EntityPlayer player, World world, int x, int y, int z )
    {
        BlockPos pos = new BlockPos( x, y, z );
        switch( id )
        {
            case DISK_DRIVE:
            {
                TileEntity tile = world.getTileEntity( pos );
                if( tile instanceof TileDiskDrive )
                {
                    TileDiskDrive drive = (TileDiskDrive) tile;
                    return new ContainerDiskDrive( player.inventory, drive );
                }
                break;
            }
            case COMPUTER:
            {
                TileEntity tile = world.getTileEntity( pos );
                if( tile instanceof TileComputer )
                {
                    TileComputer computer = (TileComputer) tile;
                    return new ContainerComputer( computer );
                }
                break;
            }
            case PRINTER:
            {
                TileEntity tile = world.getTileEntity( pos );
                if( tile instanceof TilePrinter )
                {
                    TilePrinter printer = (TilePrinter) tile;
                    return new ContainerPrinter( player.inventory, printer );
                }
                break;
            }
            case TURTLE:
            {
                TileEntity tile = world.getTileEntity( pos );
                if( tile instanceof TileTurtle )
                {
                    TileTurtle turtle = (TileTurtle) tile;
                    return new ContainerTurtle( player.inventory, turtle.getAccess(), turtle.getServerComputer() );
                }
                break;
            }
            case PRINTOUT:
                return new ContainerHeldItem( player, x == 0 ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND );
            case POCKET_COMPUTER:
                return new ContainerPocketComputer( player, x == 0 ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND );
            case VIEW_COMPUTER:
            {
                ServerComputer computer = ComputerCraft.serverComputerRegistry.get( x );
                return computer == null ? null : new ContainerViewComputer( computer );
            }
        }
        return null;
    }

    @Override
    @SideOnly( Side.CLIENT )
    public Object getClientGuiElement( int id, EntityPlayer player, World world, int x, int y, int z )
    {
        BlockPos pos = new BlockPos( x, y, z );
        switch( id )
        {
            case DISK_DRIVE:
            {
                TileEntity tile = world.getTileEntity( pos );
                return tile instanceof TileDiskDrive ? new GuiDiskDrive( new ContainerDiskDrive( player.inventory, (TileDiskDrive) tile ) ) : null;
            }
            case COMPUTER:
            {
                TileEntity tile = world.getTileEntity( pos );
                return tile instanceof TileComputer ? new GuiComputer( (TileComputer) tile ) : null;
            }
            case PRINTER:
            {
                TileEntity tile = world.getTileEntity( pos );
                return tile instanceof TilePrinter ? new GuiPrinter( new ContainerPrinter( player.inventory, (TilePrinter) tile ) ) : null;
            }
            case TURTLE:
            {
                TileEntity tile = world.getTileEntity( pos );
                if( tile instanceof TileTurtle )
                {
                    TileTurtle turtle = (TileTurtle) tile;
                    return new GuiTurtle( turtle, new ContainerTurtle( player.inventory, turtle.getAccess() ) );
                }
                return null;
            }
            case PRINTOUT:
            {
                ContainerHeldItem container = new ContainerHeldItem( player, x == 0 ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND );
                return container.getStack().getItem() instanceof ItemPrintout ? new GuiPrintout( container ) : null;
            }
            case POCKET_COMPUTER:
            {
                ContainerPocketComputer container = new ContainerPocketComputer( player, x == 0 ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND );
                return container.getStack().getItem() instanceof ItemPocketComputer ? new GuiPocketComputer( container ) : null;
            }
            case VIEW_COMPUTER:
            {
                ClientComputer computer = ComputerCraft.clientComputerRegistry.get( x );

                // We extract some terminal information from the various coordinate flags.
                // See ComputerCraft.openComputerGUI for how they are packed.
                ComputerFamily family = ComputerFamily.values()[y];
                int width = (z >> 16) & 0xFFFF, height = z & 0xFF;

                if( computer == null )
                {
                    computer = new ClientComputer( x );
                    ComputerCraft.clientComputerRegistry.add( x, computer );
                }
                else if( computer.getTerminal() != null )
                {
                    width = computer.getTerminal().getWidth();
                    height = computer.getTerminal().getHeight();
                }

                ContainerViewComputer container = new ContainerViewComputer( computer );
                return new GuiComputer( container, family, computer, width, height );
            }
            default:
                return null;
        }
    }
}
