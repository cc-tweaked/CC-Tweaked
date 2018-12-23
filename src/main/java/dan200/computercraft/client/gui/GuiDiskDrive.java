/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.gui;

import dan200.computercraft.shared.peripheral.diskdrive.ContainerDiskDrive;
import dan200.computercraft.shared.peripheral.diskdrive.TileDiskDrive;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

public class GuiDiskDrive extends GuiContainer
{
    private static final ResourceLocation background = new ResourceLocation( "computercraft", "textures/gui/diskdrive.png" );

    private TileDiskDrive m_diskDrive;

    public GuiDiskDrive( InventoryPlayer inventoryplayer, TileDiskDrive diskDrive )
    {
        super( new ContainerDiskDrive( inventoryplayer, diskDrive ) );
        m_diskDrive = diskDrive;
    }

    @Override
    protected void drawGuiContainerForegroundLayer( int par1, int par2 )
    {
        String title = m_diskDrive.getDisplayName().getUnformattedText();
        fontRenderer.drawString( title, (xSize - fontRenderer.getStringWidth( title )) / 2, 6, 0x404040 );
        fontRenderer.drawString( I18n.format( "container.inventory" ), 8, (ySize - 96) + 2, 0x404040 );
    }

    @Override
    protected void drawGuiContainerBackgroundLayer( float f, int i, int j )
    {
        GlStateManager.color( 1.0F, 1.0F, 1.0F, 1.0F );
        this.mc.getTextureManager().bindTexture( background );
        int l = (width - xSize) / 2;
        int i1 = (height - ySize) / 2;
        drawTexturedModalRect( l, i1, 0, 0, xSize, ySize );
    }

    @Override
    public void drawScreen( int mouseX, int mouseY, float partialTicks )
    {
        drawDefaultBackground();
        super.drawScreen( mouseX, mouseY, partialTicks );
        renderHoveredToolTip( mouseX, mouseY );
    }
}
