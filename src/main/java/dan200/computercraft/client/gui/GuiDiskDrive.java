/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.gui;

import dan200.computercraft.shared.peripheral.diskdrive.ContainerDiskDrive;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;

public class GuiDiskDrive extends GuiContainer
{
    private static final ResourceLocation BACKGROUND = new ResourceLocation( "computercraft", "textures/gui/disk_drive.png" );

    private final ContainerDiskDrive m_container;

    public GuiDiskDrive( ContainerDiskDrive container )
    {
        super( container );
        m_container = container;
    }

    @Override
    protected void drawGuiContainerForegroundLayer( int mouseX, int mouseY )
    {
        String title = m_container.getDiskDrive().getDisplayName().getString();
        fontRenderer.drawString( title, (xSize - fontRenderer.getStringWidth( title )) / 2.0f, 6, 0x404040 );
        fontRenderer.drawString( I18n.format( "container.inventory" ), 8, ySize - 96 + 2, 0x404040 );
    }

    @Override
    protected void drawGuiContainerBackgroundLayer( float partialTicks, int mouseX, int mouseY )
    {
        GlStateManager.color4f( 1.0F, 1.0F, 1.0F, 1.0F );
        mc.getTextureManager().bindTexture( BACKGROUND );
        drawTexturedModalRect( guiLeft, guiTop, 0, 0, xSize, ySize );
    }

    @Override
    public void render( int mouseX, int mouseY, float partialTicks )
    {
        drawDefaultBackground();
        super.render( mouseX, mouseY, partialTicks );
        renderHoveredToolTip( mouseX, mouseY );
    }
}
