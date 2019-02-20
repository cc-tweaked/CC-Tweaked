/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.network.container;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.computer.inventory.ContainerComputer;
import dan200.computercraft.shared.peripheral.diskdrive.ContainerDiskDrive;
import dan200.computercraft.shared.peripheral.printer.ContainerPrinter;
import dan200.computercraft.shared.turtle.inventory.ContainerTurtle;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;

/**
 * Opens a GUI on a specific ComputerCraft TileEntity
 *
 * @see dan200.computercraft.shared.peripheral.diskdrive.TileDiskDrive
 * @see dan200.computercraft.shared.peripheral.printer.TilePrinter
 * @see dan200.computercraft.shared.computer.blocks.TileComputer
 */
public class TileEntityContainerType<T extends Container> implements ContainerType<T>
{
    private static final ResourceLocation DISK_DRIVE = new ResourceLocation( ComputerCraft.MOD_ID, "disk_drive" );
    private static final ResourceLocation PRINTER = new ResourceLocation( ComputerCraft.MOD_ID, "printer" );
    private static final ResourceLocation COMPUTER = new ResourceLocation( ComputerCraft.MOD_ID, "computer" );
    private static final ResourceLocation TURTLE = new ResourceLocation( ComputerCraft.MOD_ID, "turtle" );

    public BlockPos pos;
    private final ResourceLocation id;

    private TileEntityContainerType( ResourceLocation id, BlockPos pos )
    {
        this.id = id;
        this.pos = pos;
    }

    private TileEntityContainerType( ResourceLocation id )
    {
        this.id = id;
    }

    @Nonnull
    @Override
    public ResourceLocation getId()
    {
        return id;
    }

    @Override
    public void toBytes( PacketBuffer buf )
    {
        buf.writeBlockPos( pos );
    }

    @Override
    public void fromBytes( PacketBuffer buf )
    {
        pos = buf.readBlockPos();
    }

    public TileEntity getTileEntity( EntityPlayer entity )
    {
        return entity.world.getTileEntity( pos );
    }

    public static TileEntityContainerType<ContainerDiskDrive> diskDrive()
    {
        return new TileEntityContainerType<>( DISK_DRIVE );
    }

    public static TileEntityContainerType<ContainerDiskDrive> diskDrive( BlockPos pos )
    {
        return new TileEntityContainerType<>( DISK_DRIVE, pos );
    }

    public static TileEntityContainerType<ContainerPrinter> printer()
    {
        return new TileEntityContainerType<>( PRINTER );
    }

    public static TileEntityContainerType<ContainerPrinter> printer( BlockPos pos )
    {
        return new TileEntityContainerType<>( PRINTER, pos );
    }

    public static TileEntityContainerType<ContainerComputer> computer()
    {
        return new TileEntityContainerType<>( COMPUTER );
    }

    public static TileEntityContainerType<ContainerComputer> computer( BlockPos pos )
    {
        return new TileEntityContainerType<>( COMPUTER, pos );
    }

    public static TileEntityContainerType<ContainerTurtle> turtle()
    {
        return new TileEntityContainerType<>( TURTLE );
    }

    public static TileEntityContainerType<ContainerTurtle> turtle( BlockPos pos )
    {
        return new TileEntityContainerType<>( TURTLE, pos );
    }
}
