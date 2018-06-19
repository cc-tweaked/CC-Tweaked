/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.gui;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.core.terminal.TextBuffer;
import dan200.computercraft.shared.media.inventory.ContainerHeldItem;
import dan200.computercraft.shared.media.items.ItemPrintout;
import dan200.computercraft.shared.util.Palette;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;

import java.io.IOException;

public class GuiPrintout extends GuiContainer
{
    public static final ResourceLocation BACKGROUND = new ResourceLocation( "computercraft", "textures/gui/printout.png" );
    public static final int X_SIZE = 172;
    public static final int Y_SIZE = 209;
    
    private final boolean m_book;
    private final int m_pages;
    private final TextBuffer[] m_text;
    private final TextBuffer[] m_colours;
    private int m_page;

    public GuiPrintout( ContainerHeldItem container )
    {
        super( container );
        m_book = (ItemPrintout.getType( container.getStack() ) == ItemPrintout.Type.Book);

        String[] text = ItemPrintout.getText( container.getStack() );
        m_text = new TextBuffer[ text.length ];
        for( int i=0; i<m_text.length; ++i )
        {
            m_text[i] = new TextBuffer( text[i] );
        }
        String[] colours = ItemPrintout.getColours( container.getStack() );
        m_colours = new TextBuffer[ colours.length ];
        for( int i=0; i<m_colours.length; ++i )
        {
            m_colours[i] = new TextBuffer( colours[i] );
        }

        m_pages = Math.max( m_text.length / ItemPrintout.LINES_PER_PAGE, 1 );
        m_page = 0;
    }

    @Override
    public void initGui()
    {
        super.initGui();
    }

    @Override
    public void onGuiClosed()
    {
        super.onGuiClosed();
    }

    @Override
    public boolean doesGuiPauseGame()
    {
        return false;
    }

    @Override
    public void updateScreen()
    {
        super.updateScreen();
    }

    @Override
    protected void keyTyped(char c, int k) throws IOException
    {
        super.keyTyped( c, k );

        if( k == 205 )
        {
            // Right
            if( m_page < m_pages - 1 )
            {
                m_page = m_page + 1;
            }
        }
        else if( k == 203 )
        {
             // Left
            if( m_page > 0 )
            {
                m_page = m_page - 1;
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException
    {
        super.handleMouseInput();

        int mouseWheelChange = Mouse.getEventDWheel();
        if (mouseWheelChange < 0)
        {
            // Up
            if( m_page < m_pages - 1 )
            {
                m_page = m_page + 1;
            }
        }
        else if (mouseWheelChange > 0) 
        {
            // Down
            if( m_page > 0 )
            {
                m_page = m_page - 1;
            }
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
    public void drawScreen(int mouseX, int mouseY, float f)
    {
        // Draw background
        drawDefaultBackground();
        
        // Draw the printout
        GlStateManager.color( 1.0f, 1.0f, 1.0f, 1.0f );
        this.mc.getTextureManager().bindTexture( BACKGROUND );
        
        int startY = (height - Y_SIZE) / 2;
        //int startX = (width - X_SIZE) / 2 - (m_page * 8);
        int startX = (width - (X_SIZE + (m_pages - 1)*8)) / 2;
        
        if( m_book )
        {
            // Border
            drawTexturedModalRect( startX - 8, startY - 8, X_SIZE + 48, 0, 12, Y_SIZE + 24);
            drawTexturedModalRect( startX + X_SIZE + (m_pages - 1)*8 - 4, startY - 8, X_SIZE + 48 + 12, 0, 12, Y_SIZE + 24);
            
            drawTexturedModalRect( startX, startY - 8, 0, Y_SIZE, X_SIZE, 12);
            drawTexturedModalRect( startX, startY + Y_SIZE - 4, 0, Y_SIZE + 12, X_SIZE, 12);
            for( int n=1; n<m_pages; ++n )
            {
                drawTexturedModalRect( startX + X_SIZE + (n-1)*8, startY - 8, 0, Y_SIZE, 8, 12);
                drawTexturedModalRect( startX + X_SIZE + (n-1)*8, startY + Y_SIZE - 4, 0, Y_SIZE + 12, 8, 12);
            }
        }
            
        // Left half
        if( m_page == 0 )
        {            
            drawTexturedModalRect( startX, startY, 24, 0, X_SIZE / 2, Y_SIZE );
            drawTexturedModalRect( startX, startY, 0, 0, 12, Y_SIZE );
        }
        else
        {
            drawTexturedModalRect( startX, startY, 0, 0, 12, Y_SIZE );
            for( int n=1; n<m_page; ++n )
            {
                drawTexturedModalRect( startX + n*8, startY, 12, 0, 12, Y_SIZE );                
            }
            drawTexturedModalRect( startX + m_page*8, startY, 24, 0, X_SIZE / 2, Y_SIZE );
        }
        
        // Right half
        if( m_page == (m_pages - 1) )
        {
            drawTexturedModalRect( startX + m_page*8 + X_SIZE /2, startY, 24 + X_SIZE / 2, 0, X_SIZE / 2, Y_SIZE );
            drawTexturedModalRect( startX + m_page*8 + (X_SIZE - 12), startY, 24 + X_SIZE + 12, 0, 12, Y_SIZE );
        }
        else 
        {
            drawTexturedModalRect( startX + (m_pages - 1)*8 + (X_SIZE - 12), startY, 24 + X_SIZE + 12, 0, 12, Y_SIZE );
            for( int n=m_pages-2; n>=m_page; --n )
            {
                drawTexturedModalRect( startX + n*8 + (X_SIZE - 12), startY, 24 + X_SIZE, 0, 12, Y_SIZE );
            }
            drawTexturedModalRect( startX + m_page*8 + X_SIZE /2, startY, 24 + X_SIZE / 2, 0, X_SIZE / 2, Y_SIZE );
        }

        // Draw the text
        FixedWidthFontRenderer fontRenderer = (FixedWidthFontRenderer)ComputerCraft.getFixedWidthFontRenderer();
        int x = startX + m_page * 8 + 13;
        int y = startY + 11;
        for( int line=0; line<ItemPrintout.LINES_PER_PAGE; ++line )
        {
            int lineIdx = ItemPrintout.LINES_PER_PAGE * m_page + line;
            if( lineIdx >= 0 && lineIdx < m_text.length )
            {
                fontRenderer.drawString( m_text[lineIdx], x, y, m_colours[lineIdx], null, 0, 0, false, Palette.DEFAULT );
            }
            y = y + FixedWidthFontRenderer.FONT_HEIGHT;
        }
    }
}
