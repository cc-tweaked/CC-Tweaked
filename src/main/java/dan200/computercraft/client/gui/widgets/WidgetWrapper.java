/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.gui.widgets;

import net.minecraft.client.gui.Element;

public class WidgetWrapper implements Element {
    private final Element listener;
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    public WidgetWrapper(Element listener, int x, int y, int width, int height) {
        this.listener = listener;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        double dx = x - this.x, dy = y - this.y;
        return dx >= 0 && dx < this.width && dy >= 0 && dy < this.height && this.listener.mouseClicked(dx, dy, button);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        double dx = x - this.x, dy = y - this.y;
        return dx >= 0 && dx < this.width && dy >= 0 && dy < this.height && this.listener.mouseReleased(dx, dy, button);
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double deltaX, double deltaY) {
        double dx = x - this.x, dy = y - this.y;
        return dx >= 0 && dx < this.width && dy >= 0 && dy < this.height && this.listener.mouseDragged(dx, dy, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double delta) {
        double dx = x - this.x, dy = y - this.y;
        return dx >= 0 && dx < this.width && dy >= 0 && dy < this.height && this.listener.mouseScrolled(dx, dy, delta);
    }

    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        return this.listener.keyPressed(key, scancode, modifiers);
    }

    @Override
    public boolean keyReleased(int key, int scancode, int modifiers) {
        return this.listener.keyReleased(key, scancode, modifiers);
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        return this.listener.charTyped(character, modifiers);
    }

    @Override
    public boolean changeFocus(boolean b) {
        return this.listener.changeFocus(b);
    }

    @Override
    public boolean isMouseOver(double x, double y) {
        double dx = x - this.x, dy = y - this.y;
        return dx >= 0 && dx < this.width && dy >= 0 && dy < this.height;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }
}
