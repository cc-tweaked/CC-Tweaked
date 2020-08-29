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
import dan200.computercraft.shared.computer.blocks.TileComputer;
import dan200.computercraft.shared.computer.core.ClientComputer;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.inventory.ContainerComputer;
import org.lwjgl.glfw.GLFW;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;

public class GuiComputer<T extends ScreenHandler> extends HandledScreen<T> {
    public static final Identifier BACKGROUND_NORMAL = new Identifier(ComputerCraft.MOD_ID, "textures/gui/corners_normal.png");
    public static final Identifier BACKGROUND_ADVANCED = new Identifier(ComputerCraft.MOD_ID, "textures/gui/corners_advanced.png");
    public static final Identifier BACKGROUND_COMMAND = new Identifier(ComputerCraft.MOD_ID, "textures/gui/corners_command.png");
    public static final Identifier BACKGROUND_COLOUR = new Identifier(ComputerCraft.MOD_ID, "textures/gui/corners_colour.png");

    private final ComputerFamily m_family;
    private final ClientComputer m_computer;
    private final int m_termWidth;
    private final int m_termHeight;

    private WidgetTerminal terminal;
    private WidgetWrapper terminalWrapper;


    public GuiComputer(T container, PlayerInventory player, ComputerFamily family, ClientComputer computer, int termWidth, int termHeight) {
        super(container, player, new LiteralText(""));

        this.m_family = family;
        this.m_computer = computer;
        this.m_termWidth = termWidth;
        this.m_termHeight = termHeight;
        this.terminal = null;
    }

    public static GuiComputer<ContainerComputer> create(int id, TileComputer computer, PlayerInventory player) {
        return new GuiComputer<>(new ContainerComputer(id, computer),
                                 player,
                                 computer.getFamily(),
                                 computer.createClientComputer(),
                                 ComputerCraft.terminalWidth_computer,
                                 ComputerCraft.terminalHeight_computer);
    }

    @Override
    protected void init() {
        MinecraftClient.getInstance().keyboard.enableRepeatEvents(true);

        int termPxWidth = this.m_termWidth * FixedWidthFontRenderer.FONT_WIDTH;
        int termPxHeight = this.m_termHeight * FixedWidthFontRenderer.FONT_HEIGHT;

        this.backgroundWidth = termPxWidth + 4 + 24;
        this.backgroundHeight = termPxHeight + 4 + 24;

        super.init();

        this.terminal = new WidgetTerminal(minecraft, () -> this.m_computer, this.m_termWidth, this.m_termHeight, 2, 2, 2, 2);
        this.terminalWrapper = new WidgetWrapper(this.terminal, 2 + 12 + this.x, 2 + 12 + this.y, termPxWidth, termPxHeight);

        this.children.add(this.terminalWrapper);
        this.setFocused(this.terminalWrapper);
    }

    @Override
    public void render(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(stack, 0);
        super.render(stack, mouseX, mouseY, partialTicks);
        this.drawMouseoverTooltip(stack, mouseX, mouseY);
    }

    @Override
    public void drawBackground(MatrixStack stack, float partialTicks, int mouseX, int mouseY) {
        // Work out where to draw
        int startX = this.terminalWrapper.getX() - 2;
        int startY = this.terminalWrapper.getY() - 2;
        int endX = startX + this.terminalWrapper.getWidth() + 4;
        int endY = startY + this.terminalWrapper.getHeight() + 4;

        // Draw terminal
        this.terminal.draw(this.terminalWrapper.getX(), this.terminalWrapper.getY());

        // Draw a border around the terminal
        GlStateManager.color4f(1.0f, 1.0f, 1.0f, 1.0f);
        switch (this.m_family) {
        case Normal:
        default:
            minecraft.getTextureManager()
                     .bindTextureInner(BACKGROUND_NORMAL);
            break;
        case Advanced:
            minecraft.getTextureManager()
                     .bindTextureInner(BACKGROUND_ADVANCED);
            break;
        case Command:
            minecraft.getTextureManager()
                     .bindTextureInner(BACKGROUND_COMMAND);
            break;
        }

        this.drawTexture(stack, startX - 12, startY - 12, 12, 28, 12, 12);
        this.drawTexture(stack, startX - 12, endY, 12, 40, 12, 12);
        this.drawTexture(stack, endX, startY - 12, 24, 28, 12, 12);
        this.drawTexture(stack, endX, endY, 24, 40, 12, 12);

        this.drawTexture(stack, startX, startY - 12, 0, 0, endX - startX, 12);
        this.drawTexture(stack, startX, endY, 0, 12, endX - startX, 12);

        this.drawTexture(stack, startX - 12, startY, 0, 28, 12, endY - startY);
        this.drawTexture(stack, endX, startY, 36, 28, 12, endY - startY);
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double deltaX, double deltaY) {
        // Make sure drag events are propagated to children
        return (this.getFocused() != null && this.getFocused().mouseDragged(x, y, button, deltaX, deltaY)) || super.mouseDragged(x, y, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        // Make sure release events are propagated to children
        return (this.getFocused() != null && this.getFocused().mouseReleased(x, y, button)) || super.mouseReleased(x, y, button);
    }

    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        // When pressing tab, send it to the computer first
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
}
