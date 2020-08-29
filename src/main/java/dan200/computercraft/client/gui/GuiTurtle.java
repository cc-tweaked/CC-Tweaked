/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.gui.widgets.WidgetTerminal;
import dan200.computercraft.client.gui.widgets.WidgetWrapper;
import dan200.computercraft.shared.computer.core.ClientComputer;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.turtle.blocks.TileTurtle;
import dan200.computercraft.shared.turtle.inventory.ContainerTurtle;
import org.lwjgl.glfw.GLFW;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.Identifier;

public class GuiTurtle extends HandledScreen<ContainerTurtle> {
    private static final Identifier BACKGROUND_NORMAL = new Identifier("computercraft", "textures/gui/turtle_normal.png");
    private static final Identifier BACKGROUND_ADVANCED = new Identifier("computercraft", "textures/gui/turtle_advanced.png");
    private final ComputerFamily m_family;
    private final ClientComputer m_computer;
    private ContainerTurtle m_container;
    private WidgetTerminal terminal;
    private WidgetWrapper terminalWrapper;

    public GuiTurtle(TileTurtle turtle, ContainerTurtle container, PlayerInventory player) {
        super(container, player, turtle.getDisplayName());

        this.m_container = container;
        this.m_family = turtle.getFamily();
        this.m_computer = turtle.getClientComputer();

        this.backgroundWidth = 254;
        this.backgroundHeight = 217;
    }

    @Override
    protected void init() {
        super.init();
        minecraft.keyboard.enableRepeatEvents(true);

        int termPxWidth = ComputerCraft.terminalWidth_turtle * FixedWidthFontRenderer.FONT_WIDTH;
        int termPxHeight = ComputerCraft.terminalHeight_turtle * FixedWidthFontRenderer.FONT_HEIGHT;

        this.terminal = new WidgetTerminal(minecraft, () -> this.m_computer, ComputerCraft.terminalWidth_turtle, ComputerCraft.terminalHeight_turtle, 2, 2, 2, 2);
        this.terminalWrapper = new WidgetWrapper(this.terminal, 2 + 8 + this.x, 2 + 8 + this.y, termPxWidth, termPxHeight);

        this.children.add(this.terminalWrapper);
        this.setFocused(this.terminalWrapper);
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double deltaX, double deltaY) {
        return (this.getFocused() != null && this.getFocused().mouseDragged(x, y, button, deltaX, deltaY)) || super.mouseDragged(x, y, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        return (this.getFocused() != null && this.getFocused().mouseReleased(x, y, button)) || super.mouseReleased(x, y, button);
    }

    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        return (key == GLFW.GLFW_KEY_TAB && this.getFocused() == this.terminalWrapper && this.terminalWrapper.keyPressed(key,
                                                                                                                         scancode,
                                                                                                                         modifiers)) || super.keyPressed(key,
                                                                                                                                          scancode,
                                                                                                                                          modifiers);
    }

    @Override
    public void removed() {
        super.removed();
        this.children.remove(this.terminal);
        this.terminal = null;
        minecraft.keyboard.enableRepeatEvents(false);
    }

    @Override
    public void tick() {
        super.tick();
        this.terminal.update();
    }

    @Override
    protected void drawBackground(float partialTicks, int mouseX, int mouseY) {
        // Draw term
        boolean advanced = this.m_family == ComputerFamily.Advanced;
        this.terminal.draw(this.terminalWrapper.getX(), this.terminalWrapper.getY());

        // Draw border/inventory
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        minecraft.getTextureManager()
                 .bindTextureInner(advanced ? BACKGROUND_ADVANCED : BACKGROUND_NORMAL);
        blit(this.x, this.y, 0, 0, this.backgroundWidth, this.backgroundHeight);

        this.drawSelectionSlot(advanced);
    }

    private void drawSelectionSlot(boolean advanced) {
        // Draw selection slot
        int slot = this.m_container.getSelectedSlot();
        if (slot >= 0) {
            GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            int slotX = slot % 4;
            int slotY = slot / 4;
            minecraft.getTextureManager()
                     .bindTextureInner(advanced ? BACKGROUND_ADVANCED : BACKGROUND_NORMAL);
            blit(this.x + this.m_container.m_turtleInvStartX - 2 + slotX * 18, this.y + this.m_container.m_playerInvStartY - 2 + slotY * 18, 0, 217, 24, 24);
        }
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        renderBackground();
        super.render(mouseX, mouseY, partialTicks);
        this.drawMouseoverTooltip(mouseX, mouseY);
    }
}
