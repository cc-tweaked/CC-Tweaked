/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.gui;

import static dan200.computercraft.client.render.ComputerBorderRenderer.BORDER;
import static dan200.computercraft.client.render.ComputerBorderRenderer.MARGIN;

import javax.annotation.Nonnull;

import com.mojang.blaze3d.systems.RenderSystem;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.gui.widgets.WidgetTerminal;
import dan200.computercraft.client.gui.widgets.WidgetWrapper;
import dan200.computercraft.client.render.ComputerBorderRenderer;
import dan200.computercraft.shared.computer.blocks.TileComputer;
import dan200.computercraft.shared.computer.core.ClientComputer;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.IContainerComputer;
import dan200.computercraft.shared.computer.inventory.ContainerComputer;
import dan200.computercraft.shared.computer.inventory.ContainerComputerBase;
import dan200.computercraft.shared.computer.inventory.ContainerViewComputer;
import dan200.computercraft.shared.pocket.inventory.ContainerPocketComputer;
import org.lwjgl.glfw.GLFW;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public final class GuiComputer<T extends ScreenHandler & IContainerComputer> extends HandledScreen<T> {

    private final ComputerFamily family;
    private final ClientComputer computer;
    private final int termWidth;
    private final int termHeight;

    private WidgetTerminal terminal;
    private WidgetWrapper terminalWrapper;

    public GuiComputer(T container, PlayerInventory player, ComputerFamily family, ClientComputer computer, int termWidth, int termHeight) {
        super(container, player, new LiteralText(""));
        this.family = family;
        this.computer = computer;
        this.termWidth = termWidth;
        this.termHeight = termHeight;
        this.terminal = null;
    }

    private GuiComputer(T container, PlayerInventory player, ComputerFamily family, Text title, int termWidth, int termHeight) {
        super(container, player, title);
        this.family = family;
        this.computer = (ClientComputer) container.getComputer();
        this.termWidth = termWidth;
        this.termHeight = termHeight;
        this.terminal = null;
    }


    public static GuiComputer<ContainerComputer> create(int id, TileComputer computer, PlayerInventory player) {
        return create(new ContainerComputer(id, computer), player, computer.getDisplayName());
    }

    public static GuiComputer<ContainerComputer> create(ContainerComputer container, PlayerInventory inventory, Text component) {
        return new GuiComputer<>(container, inventory, container.getFamily(), component, ComputerCraft.computerTermWidth, ComputerCraft.computerTermHeight);
    }

    @Override
    protected void init() {
        this.client.keyboard.setRepeatEvents(true);

        int termPxWidth = this.termWidth * FixedWidthFontRenderer.FONT_WIDTH;
        int termPxHeight = this.termHeight * FixedWidthFontRenderer.FONT_HEIGHT;

        this.backgroundWidth = termPxWidth + MARGIN * 2 + BORDER * 2;
        this.backgroundHeight = termPxHeight + MARGIN * 2 + BORDER * 2;

        super.init();

        this.terminal = new WidgetTerminal(this.client, () -> this.computer, this.termWidth, this.termHeight, MARGIN, MARGIN, MARGIN, MARGIN);
        this.terminalWrapper = new WidgetWrapper(this.terminal, MARGIN + BORDER + this.x, MARGIN + BORDER + this.y, termPxWidth, termPxHeight);

        this.children.add(this.terminalWrapper);
        this.setFocused(this.terminalWrapper);
    }

    @Override
    public void removed() {
        super.removed();
        this.children.remove(this.terminal);
        this.terminal = null;
        this.client.keyboard.setRepeatEvents(false);
    }

    @Override
    public void tick() {
        super.tick();
        this.terminal.update();
    }

    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        // Forward the tab key to the terminal, rather than moving between controls.
        if (key == GLFW.GLFW_KEY_TAB && this.getFocused() != null && this.getFocused() == this.terminalWrapper) {
            return this.getFocused().keyPressed(key, scancode, modifiers);
        }

        return super.keyPressed(key, scancode, modifiers);
    }

    @Override
    public void drawBackground(@Nonnull MatrixStack stack, float partialTicks, int mouseX, int mouseY) {
        // Draw terminal
        this.terminal.draw(this.terminalWrapper.getX(), this.terminalWrapper.getY());

        // Draw a border around the terminal
        RenderSystem.color4f(1, 1, 1, 1);
        this.client.getTextureManager()
                   .bindTexture(ComputerBorderRenderer.getTexture(this.family));
        ComputerBorderRenderer.render(this.terminalWrapper.getX() - MARGIN, this.terminalWrapper.getY() - MARGIN,
                                      this.getZOffset(), this.terminalWrapper.getWidth() + MARGIN * 2, this.terminalWrapper.getHeight() + MARGIN * 2);
    }

    @Override
    public void render(@Nonnull MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        super.render(stack, mouseX, mouseY, partialTicks);
        this.drawMouseoverTooltip(stack, mouseX, mouseY);
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double deltaX, double deltaY) {
        return (this.getFocused() != null && this.getFocused().mouseDragged(x, y, button, deltaX, deltaY)) || super.mouseDragged(x, y, button, deltaX, deltaY);
    }

    @Override
    protected void drawForeground(@Nonnull MatrixStack transform, int mouseX, int mouseY) {
        // Skip rendering labels.
    }
}
