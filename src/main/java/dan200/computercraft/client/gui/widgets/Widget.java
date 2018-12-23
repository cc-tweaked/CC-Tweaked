/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;

public abstract class Widget extends Gui
{
    private int m_xPosition;
    private int m_yPosition;
    private int m_width;
    private int m_height;

    protected Widget( int x, int y, int width, int height )
    {
        m_xPosition = x;
        m_yPosition = y;
        m_width = width;
        m_height = height;
    }

    public int getXPosition()
    {
        return m_xPosition;
    }

    public int getYPosition()
    {
        return m_yPosition;
    }

    public int getWidth()
    {
        return m_width;
    }

    public int getHeight()
    {
        return m_height;
    }

    public void update()
    {
    }

    public void draw( Minecraft mc, int xOrigin, int yOrigin, int mouseX, int mouseY )
    {
    }

    public void handleMouseInput( int mouseX, int mouseY )
    {
    }

    public boolean onKeyboardInput()
    {
        return false;
    }

    @Deprecated
    public void handleKeyboardInput()
    {
        onKeyboardInput();
    }

    public void mouseClicked( int mouseX, int mouseY, int mouseButton )
    {
    }

    public boolean onKeyTyped( char c, int k )
    {
        return false;
    }

    @Deprecated
    public void keyTyped( char c, int k )
    {
        onKeyTyped( c, k );
    }
}
