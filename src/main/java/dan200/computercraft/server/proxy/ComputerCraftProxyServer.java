/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.server.proxy;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.computer.blocks.TileComputer;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.IComputer;
import dan200.computercraft.shared.network.ComputerCraftPacket;
import dan200.computercraft.shared.peripheral.diskdrive.TileDiskDrive;
import dan200.computercraft.shared.peripheral.printer.TilePrinter;
import dan200.computercraft.shared.proxy.ComputerCraftProxyCommon;
import dan200.computercraft.shared.turtle.blocks.TileTurtle;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import java.io.File;

public class ComputerCraftProxyServer extends ComputerCraftProxyCommon
{
    public ComputerCraftProxyServer()
    {
    }
    
    // IComputerCraftProxy implementation
    
    @Override
    public void init()
    {
        super.init();
    }

    @Override
    public Object getTurtleGUI( InventoryPlayer inventory, TileTurtle turtle )
    {
        return null;
    }

    @Override
    public boolean isClient()
    {
        return false;
    }

    @Override
    public boolean getGlobalCursorBlink()
    {
        return false;
    }

    @Override
    public long getRenderFrame()
    {
        return 0;
    }

    @Override
    public Object getFixedWidthFontRenderer()
    {
        return null;
    }

    @Override
    public Object getDiskDriveGUI( InventoryPlayer inventory, TileDiskDrive drive )
    {
        return null;
    }
    
    @Override
    public Object getComputerGUI( TileComputer computer )
    {
        return null;
    }

    @Override
    public Object getPrinterGUI( InventoryPlayer inventory, TilePrinter printer )
    {
        return null;
    }

    @Override
    public Object getPrintoutGUI( EntityPlayer player, EnumHand hand )
    {
        return null;
    }

    @Override
    public Object getPocketComputerGUI( EntityPlayer player, EnumHand hand )
    {
        return null;
    }

    @Override
    public Object getComputerGUI( IComputer computer, int width, int height, ComputerFamily family )
    {
        return null;
    }

    @Override
    public File getWorldDir( World world )
    {
        return DimensionManager.getWorld( 0 ).getSaveHandler().getWorldDirectory();
    }
}
