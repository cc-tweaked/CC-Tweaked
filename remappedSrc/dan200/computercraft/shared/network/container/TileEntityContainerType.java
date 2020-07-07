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
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;

/**
 * Opens a GUI on a specific ComputerCraft BlockEntity
 *
 * @see dan200.computercraft.shared.peripheral.diskdrive.TileDiskDrive
 * @see dan200.computercraft.shared.peripheral.printer.TilePrinter
 * @see dan200.computercraft.shared.computer.blocks.TileComputer
 */
public final class TileEntityContainerType<T extends ScreenHandler> implements ContainerType<T>
{
    private static final Identifier DISK_DRIVE = new Identifier( ComputerCraft.MOD_ID, "disk_drive" );
    private static final Identifier PRINTER = new Identifier( ComputerCraft.MOD_ID, "printer" );
    private static final Identifier COMPUTER = new Identifier( ComputerCraft.MOD_ID, "computer" );
    private static final Identifier TURTLE = new Identifier( ComputerCraft.MOD_ID, "turtle" );

    public BlockPos pos;
    private final Identifier id;

    private TileEntityContainerType( Identifier id, BlockPos pos )
    {
        this.id = id;
        this.pos = pos;
    }

    private TileEntityContainerType( Identifier id )
    {
        this.id = id;
    }

    @Nonnull
    @Override
    public Identifier getId()
    {
        return id;
    }

    @Override
    public void toBytes( PacketByteBuf buf )
    {
        buf.writeBlockPos( pos );
    }

    @Override
    public void fromBytes( PacketByteBuf buf )
    {
        pos = buf.readBlockPos();
    }

    public BlockEntity getTileEntity( PlayerEntity entity )
    {
        return entity.world.getBlockEntity( pos );
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
