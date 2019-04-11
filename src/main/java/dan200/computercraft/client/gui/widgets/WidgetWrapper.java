/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.gui.widgets;

import net.minecraft.client.gui.Element;

public class WidgetWrapper implements Element
{
    private final Element listener;
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    public WidgetWrapper( Element listener, int x, int y, int width, int height )
    {
        this.listener = listener;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public void mouseMoved( double x, double y )
    {
        double dx = x - this.x, dy = y - this.y;
        if( dx >= 0 && dx < width && dy >= 0 && dy < height ) listener.mouseMoved( dx, dy );
    }

    @Override
    public boolean changeFocus( boolean reverse )
    {
        return listener.changeFocus( reverse );
    }

    @Override
    public boolean mouseClicked( double x, double y, int button )
    {
        double dx = x - this.x, dy = y - this.y;
        return dx >= 0 && dx < width && dy >= 0 && dy < height && listener.mouseClicked( dx, dy, button );
    }

    @Override
    public boolean mouseReleased( double x, double y, int button )
    {
        double dx = x - this.x, dy = y - this.y;
        return dx >= 0 && dx < width && dy >= 0 && dy < height && listener.mouseReleased( dx, dy, button );
    }

    @Override
    public boolean mouseDragged( double x, double y, int button, double deltaX, double deltaY )
    {
        double dx = x - this.x, dy = y - this.y;
        return dx >= 0 && dx < width && dy >= 0 && dy < height && listener.mouseDragged( dx, dy, button, deltaX, deltaY );
    }

    @Override
    public boolean mouseScrolled( double x, double y, double delta )
    {
        double dx = x - this.x, dy = y - this.y;
        return dx >= 0 && dx < width && dy >= 0 && dy < height && listener.mouseScrolled( dx, dy, delta );
    }

    @Override
    public boolean keyPressed( int key, int scancode, int modifiers )
    {
        return listener.keyPressed( key, scancode, modifiers );
    }

    @Override
    public boolean keyReleased( int key, int scancode, int modifiers )
    {
        return listener.keyReleased( key, scancode, modifiers );
    }

    @Override
    public boolean charTyped( char character, int modifiers )
    {
        return listener.charTyped( character, modifiers );
    }

    public int getX()
    {
        return x;
    }

    public int getY()
    {
        return y;
    }

    public int getWidth()
    {
        return width;
    }

    public int getHeight()
    {
        return height;
    }

    @Override
    public boolean isMouseOver( double x, double y )
    {
        double dx = x - this.x, dy = y - this.y;
        return dx >= 0 && dx < width && dy >= 0 && dy < height;
    }
}
