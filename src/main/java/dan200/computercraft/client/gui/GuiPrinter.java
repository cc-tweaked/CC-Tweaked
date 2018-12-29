/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.gui;

import dan200.computercraft.shared.peripheral.printer.ContainerPrinter;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;

public class GuiPrinter extends GuiContainer
{
    private static final ResourceLocation background = new ResourceLocation( "computercraft", "textures/gui/printer.png" );

    private final ContainerPrinter m_container;

    public GuiPrinter( ContainerPrinter container )
    {
        super( container );
        m_container = container;
    }

    @Override
    protected void drawGuiContainerForegroundLayer( int par1, int par2 )
    {
        String title = m_container.getPrinter().getDisplayName().getUnformattedText();
        fontRenderer.drawString( title, (xSize - fontRenderer.getStringWidth( title )) / 2, 6, 0x404040 );
        fontRenderer.drawString( I18n.format( "container.inventory" ), 8, (ySize - 96) + 2, 0x404040 );
    }

    @Override
    protected void drawGuiContainerBackgroundLayer( float f, int i, int j )
    {
        GlStateManager.color( 1.0F, 1.0F, 1.0F, 1.0F );
        this.mc.getTextureManager().bindTexture( background );
        int startX = (width - xSize) / 2;
        int startY = (height - ySize) / 2;
        drawTexturedModalRect( startX, startY, 0, 0, xSize, ySize );

        boolean printing = m_container.isPrinting();
        if( printing )
        {
            drawTexturedModalRect( startX + 34, startY + 21, 176, 0, 25, 45 );
        }
    }

    @Override
    public void drawScreen( int mouseX, int mouseY, float partialTicks )
    {
        drawDefaultBackground();
        super.drawScreen( mouseX, mouseY, partialTicks );
        renderHoveredToolTip( mouseX, mouseY );
    }
}
