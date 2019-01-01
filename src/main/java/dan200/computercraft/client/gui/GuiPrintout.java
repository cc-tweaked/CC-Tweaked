/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.gui;

import dan200.computercraft.core.terminal.TextBuffer;
import dan200.computercraft.shared.media.inventory.ContainerHeldItem;
import dan200.computercraft.shared.media.items.ItemPrintout;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Mouse;

import java.io.IOException;

import static dan200.computercraft.client.render.PrintoutRenderer.*;

public class GuiPrintout extends GuiContainer
{
    private final boolean m_book;
    private final int m_pages;
    private final TextBuffer[] m_text;
    private final TextBuffer[] m_colours;
    private int m_page;

    public GuiPrintout( ContainerHeldItem container )
    {
        super( container );

        String[] text = ItemPrintout.getText( container.getStack() );
        m_text = new TextBuffer[text.length];
        for( int i = 0; i < m_text.length; i++ ) m_text[i] = new TextBuffer( text[i] );

        String[] colours = ItemPrintout.getColours( container.getStack() );
        m_colours = new TextBuffer[colours.length];
        for( int i = 0; i < m_colours.length; i++ ) m_colours[i] = new TextBuffer( colours[i] );

        m_page = 0;
        m_pages = Math.max( m_text.length / ItemPrintout.LINES_PER_PAGE, 1 );
        m_book = ItemPrintout.getType( container.getStack() ) == ItemPrintout.Type.Book;
    }

    @Override
    public boolean doesGuiPauseGame()
    {
        return false;
    }

    @Override
    protected void keyTyped( char c, int k ) throws IOException
    {
        super.keyTyped( c, k );

        if( k == 205 )
        {
            // Right
            if( m_page < m_pages - 1 ) m_page++;
        }
        else if( k == 203 )
        {
            // Left
            if( m_page > 0 ) m_page--;
        }
    }

    @Override
    public void handleMouseInput() throws IOException
    {
        super.handleMouseInput();

        int mouseWheelChange = Mouse.getEventDWheel();
        if( mouseWheelChange < 0 )
        {
            // Up
            if( m_page < m_pages - 1 ) m_page++;
        }
        else if( mouseWheelChange > 0 )
        {
            // Down
            if( m_page > 0 ) m_page--;
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer( int par1, int par2 )
    {
    }

    @Override
    protected void drawGuiContainerBackgroundLayer( float var1, int var2, int var3 )
    {
    }

    @Override
    public void drawScreen( int mouseX, int mouseY, float f )
    {
        // Draw background
        zLevel = zLevel - 1;
        drawDefaultBackground();
        zLevel = zLevel + 1;

        // Draw the printout
        GlStateManager.color( 1.0f, 1.0f, 1.0f, 1.0f );

        int startY = (height - Y_SIZE) / 2;
        int startX = (width - X_SIZE) / 2;

        drawBorder( startX, startY, zLevel, m_page, m_pages, m_book );
        drawText( startX + X_TEXT_MARGIN, startY + Y_TEXT_MARGIN, ItemPrintout.LINES_PER_PAGE * m_page, m_text, m_colours );
    }
}
