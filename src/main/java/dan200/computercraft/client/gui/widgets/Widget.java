/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;

public abstract class Widget extends Gui
{
    private WidgetContainer m_parent;
    private boolean m_visible;
    private int m_xPosition;
    private int m_yPosition;
    private int m_width;
    private int m_height;

    protected Widget( int x, int y, int width, int height )
    {
        m_parent = null;
        m_visible = true;
        m_xPosition = x;
        m_yPosition = y;
        m_width = width;
        m_height = height;
    }

    public WidgetContainer getRoot()
    {
        if( m_parent != null )
        {
            return m_parent.getRoot();
        }
        else if( this instanceof WidgetContainer )
        {
            return (WidgetContainer)this;
        }
        return null;
    }

    public WidgetContainer getParent()
    {
        return m_parent;
    }

    public void setParent( WidgetContainer parent )
    {
        m_parent = parent;
    }

    public boolean isObscured()
    {
        if( m_parent != null )
        {
            Widget parentModalWidget = m_parent.getModalWidget();
            if( parentModalWidget == null )
            {
                return m_parent.isObscured();
            }
            else
            {
                return (parentModalWidget != this);
            }
        }
        return false;
    }

    public boolean isVisible()
    {
        return m_visible && (m_parent == null || m_parent.isVisible());
    }

    public void setVisible( boolean visible )
    {
        m_visible = visible;
    }

    public int getXPosition()
    {
        return m_xPosition;
    }

    public int getYPosition()
    {
        return m_yPosition;
    }

    public int getAbsoluteXPosition()
    {
        return m_xPosition + (m_parent != null ? m_parent.getAbsoluteXPosition() : 0);
    }

    public int getAbsoluteYPosition()
    {
        return m_yPosition + (m_parent != null ? m_parent.getAbsoluteYPosition() : 0);
    }

    public int getWidth()
    {
        return m_width;
    }

    public int getHeight()
    {
        return m_height;
    }

    public void setPosition( int x, int y )
    {
        m_xPosition = x;
        m_yPosition = y;
    }

    public void resize( int width, int height )
    {
        m_width = width;
        m_height = height;
    }

    public void update()
    {
    }

    public void draw( Minecraft mc, int xOrigin, int yOrigin, int mouseX, int mouseY )
    {
    }

    public void drawForeground( Minecraft mc, int xOrigin, int yOrigin, int mouseX, int mouseY )
    {
    }

    public void modifyMousePosition( MousePos pos )
    {
    }

    public void handleMouseInput( int mouseX, int mouseY )
    {
    }

    public void handleKeyboardInput()
    {
    }

    public void mouseClicked( int mouseX, int mouseY, int mouseButton )
    {
    }

    public void keyTyped( char c, int k )
    {
    }

    public boolean suppressItemTooltips( Minecraft mc, int xOrigin, int yOrigin, int mouseX, int mouseY )
    {
        return false;
    }

    public boolean suppressKeyPress( char c, int k )
    {
        return false;
    }

    protected void drawFullImage( int x, int y, int w, int h )
    {
        Tessellator tessellator = Tessellator.getInstance();
        tessellator.getBuffer().begin( GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX );
        tessellator.getBuffer().pos( x + 0, y + h, this.zLevel ).tex( 0.0, 1.0 ).endVertex();
        tessellator.getBuffer().pos( x + w, y + h, this.zLevel ).tex( 1.0, 1.0 ).endVertex();
        tessellator.getBuffer().pos( x + w, y + 0, this.zLevel ).tex( 1.0, 0.0 ).endVertex();
        tessellator.getBuffer().pos( x + 0, y + 0, this.zLevel ).tex( 0.0, 0.0 ).endVertex();
        tessellator.draw();
    }

    protected void drawT3( int x, int y, int w, int h, int u, int v, int uw, int vh )
    {
        int partW = uw / 3;

        // Draw first bit
        super.drawTexturedModalRect( x, y, u, v, partW, vh );

        // Draw middle bits
        int middleBits = Math.max( (w - 2 * partW) / partW, 0 );
        for( int j=0; j<middleBits; ++j )
        {
            super.drawTexturedModalRect( x + (j + 1) * partW, y, u + partW, v, partW, vh );
        }

        // Draw end bit
        int endW = w - (middleBits + 1) * partW;
        super.drawTexturedModalRect( x + w - endW, y, u + uw - endW, v, endW, vh );
    }

    protected void drawT9( int x, int y, int w, int h, int u, int v, int uw, int vh )
    {
        int partH = vh / 3;

        // Draw first row
        drawT3( x, y, w, partH, u, v, uw, partH );

        // Draw middle rows
        int middleBits = Math.max( (h - 2 * partH) / partH, 0 );
        for( int j=0; j<middleBits; ++j )
        {
            drawT3( x, y + ( j + 1 ) * partH, w, partH, u, v + partH, uw, partH );
        }

        // Draw end row
        int endH = h - (middleBits + 1) * partH;
        drawT3( x, y + h - endH, w, endH, u, v + vh - endH, uw, endH );
    }

    protected void drawInsetBorder( int x, int y, int w, int h )
    {
        // Draw border
        try
        {
            drawVerticalLine( x, y - 1, y + h - 1, 0xff363636 );
            drawVerticalLine( x + w - 1, y, y + h, 0xffffffff );
            drawHorizontalLine( x, x + w - 2, y, 0xff363636 );
            drawHorizontalLine( x + 1, x + w - 1, y + h - 1, 0xffffffff );
            drawHorizontalLine( x, x, y + h - 1, 0xff8a8a8a );
            drawHorizontalLine( x + w - 1, x + w - 1, y, 0xff8a8a8a );
        }
        finally
        {
            GlStateManager.color( 1.0f, 1.0f, 1.0f, 1.0f );
        }
    }

    protected void drawFlatBorder( int x, int y, int w, int h, int colour )
    {
        // Draw border
        colour = colour | 0xff000000;
        try
        {
            drawVerticalLine( x, y - 1, y + h - 1, colour );
            drawVerticalLine( x + w - 1, y, y + h, colour );
            drawHorizontalLine( x, x + w - 2, y, colour );
            drawHorizontalLine( x + 1, x + w - 1, y + h - 1, colour );
            drawHorizontalLine( x, x, y + h - 1, colour );
            drawHorizontalLine( x + w - 1, x + w - 1, y, colour );
        }
        finally
        {
            GlStateManager.color( 1.0f, 1.0f, 1.0f, 1.0f );
        }
    }

    protected void drawTooltip( String line, int x, int y )
    {
        drawTooltip( new String[] { line }, x, y );
    }

    protected void drawTooltip( String[] lines, int x, int y )
    {
        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer fontRenderer = mc.fontRenderer;

        int width = 0;
        for( String line : lines )
        {
            width = Math.max( fontRenderer.getStringWidth( line ), width );
        }
        int startX = x + 12;
        int startY = y - 12;
        if( startX + width + 4 > mc.currentScreen.width )
        {
            startX -= width + 24;
            if( startX < 24 )
            {
                startX = 24;
            }
        }

        float oldZLevel = this.zLevel;
        try
        {
            this.zLevel = 300.0F;

            int height = 10 * lines.length - 2;
            int j1 = -267386864;
            this.drawGradientRect( startX - 3, startY - 4, startX + width + 3, startY - 3, j1, j1 );
            this.drawGradientRect( startX - 3, startY + height + 3, startX + width + 3, startY + height + 4, j1, j1 );
            this.drawGradientRect( startX - 3, startY - 3, startX + width + 3, startY + height + 3, j1, j1 );
            this.drawGradientRect( startX - 4, startY - 3, startX - 3, startY + height + 3, j1, j1 );

            this.drawGradientRect( startX + width + 3, startY - 3, startX + width + 4, startY + height + 3, j1, j1 );
            int k1 = 1347420415;
            int l1 = ( k1 & 16711422 ) >> 1 | k1 & -16777216;
            this.drawGradientRect( startX - 3, startY - 3 + 1, startX - 3 + 1, startY + height + 3 - 1, k1, l1 );
            this.drawGradientRect( startX + width + 2, startY - 3 + 1, startX + width + 3, startY + height + 3 - 1, k1, l1 );
            this.drawGradientRect( startX - 3, startY - 3, startX + width + 3, startY - 3 + 1, k1, k1 );
            this.drawGradientRect( startX - 3, startY + height + 2, startX + width + 3, startY + height + 3, l1, l1 );

            GlStateManager.disableDepth();
            try
            {
                for( int i = 0; i < lines.length; ++i )
                {
                    String line = lines[ i ];
                    fontRenderer.drawStringWithShadow( line, startX, startY + i * 10, 0xffffffff );
                }
            }
            finally
            {
                GlStateManager.color( 1.0f, 1.0f, 1.0f, 1.0f );
                GlStateManager.enableDepth();
            }
        }
        finally
        {
            this.zLevel = oldZLevel;
        }
    }

    protected void drawItemStack( int x, int y, @Nonnull ItemStack stack )
    {
        if( !stack.isEmpty() )
        {
            GlStateManager.color( 1.0f, 1.0f, 1.0f, 1.0f );
            GlStateManager.enableLighting();
            GlStateManager.enableRescaleNormal();
            RenderHelper.enableGUIStandardItemLighting();
            OpenGlHelper.setLightmapTextureCoords( OpenGlHelper.lightmapTexUnit, 240.0f, 240.0f );
            try
            {
                Minecraft mc = Minecraft.getMinecraft();
                RenderItem renderItem = mc.getRenderItem();
                if( renderItem != null )
                {
                    renderItem.renderItemAndEffectIntoGUI( stack, x, y );
                    renderItem.renderItemOverlayIntoGUI( mc.fontRenderer, stack, x, y, null );
                }
            }
            finally
            {
                RenderHelper.disableStandardItemLighting();
                GlStateManager.disableRescaleNormal();
                GlStateManager.disableLighting();
                GlStateManager.enableBlend();
                GlStateManager.blendFunc( GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA );
                GlStateManager.color( 1.0f, 1.0f, 1.0f, 1.0f );
            }
        }
    }

    protected void drawString( String s, int x, int y, int color )
    {
        Minecraft mc = Minecraft.getMinecraft();
        try
        {
            mc.fontRenderer.drawString( s, x, y, color );
        }
        finally
        {
            GlStateManager.color( 1.0f, 1.0f, 1.0f, 1.0f );
        }
    }

    protected int getStringWidth( String s )
    {
        Minecraft mc = Minecraft.getMinecraft();
        return mc.fontRenderer.getStringWidth( s );
    }

    protected void playClickSound()
    {
        Minecraft mc = Minecraft.getMinecraft();
        mc.getSoundHandler().playSound( PositionedSoundRecord.getMasterRecord( SoundEvents.UI_BUTTON_CLICK, 1.0F ) );
    }
}
