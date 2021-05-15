/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.gui;

import static dan200.computercraft.client.render.PrintoutRenderer.X_TEXT_MARGIN;
import static dan200.computercraft.client.render.PrintoutRenderer.Y_SIZE;
import static dan200.computercraft.client.render.PrintoutRenderer.Y_TEXT_MARGIN;
import static dan200.computercraft.client.render.PrintoutRenderer.drawBorder;
import static dan200.computercraft.client.render.PrintoutRenderer.drawText;

import javax.annotation.Nonnull;

import com.mojang.blaze3d.systems.RenderSystem;
import dan200.computercraft.core.terminal.TextBuffer;
import dan200.computercraft.shared.common.ContainerHeldItem;
import dan200.computercraft.shared.media.items.ItemPrintout;
import org.lwjgl.glfw.GLFW;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.math.Matrix4f;

public class GuiPrintout extends HandledScreen<ContainerHeldItem> {
    private final boolean m_book;
    private final int m_pages;
    private final TextBuffer[] m_text;
    private final TextBuffer[] m_colours;
    private int m_page;

    public GuiPrintout(ContainerHeldItem container, PlayerInventory player, Text title) {
        super(container, player, title);

        this.backgroundHeight = Y_SIZE;

        String[] text = ItemPrintout.getText(container.getStack());
        this.m_text = new TextBuffer[text.length];
        for (int i = 0; i < this.m_text.length; i++) {
            this.m_text[i] = new TextBuffer(text[i]);
        }

        String[] colours = ItemPrintout.getColours(container.getStack());
        this.m_colours = new TextBuffer[colours.length];
        for (int i = 0; i < this.m_colours.length; i++) {
            this.m_colours[i] = new TextBuffer(colours[i]);
        }

        this.m_page = 0;
        this.m_pages = Math.max(this.m_text.length / ItemPrintout.LINES_PER_PAGE, 1);
        this.m_book = ((ItemPrintout) container.getStack()
                                               .getItem()).getType() == ItemPrintout.Type.BOOK;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double delta) {
        if (super.mouseScrolled(x, y, delta)) {
            return true;
        }
        if (delta < 0) {
            // Scroll up goes to the next page
            if (this.m_page < this.m_pages - 1) {
                this.m_page++;
            }
            return true;
        }

        if (delta > 0) {
            // Scroll down goes to the previous page
            if (this.m_page > 0) {
                this.m_page--;
            }
            return true;
        }

        return false;
    }

    @Override
    public void render(@Nonnull MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        // We must take the background further back in order to not overlap with our printed pages.
        this.setZOffset(this.getZOffset() - 1);
        this.renderBackground(stack);
        this.setZOffset(this.getZOffset() + 1);

        super.render(stack, mouseX, mouseY, partialTicks);
    }

    @Override
    protected void drawForeground(@Nonnull MatrixStack transform, int mouseX, int mouseY) {
        // Skip rendering labels.
    }

    @Override
    protected void drawBackground(@Nonnull MatrixStack transform, float partialTicks, int mouseX, int mouseY) {
        // Draw the printout
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.enableDepthTest();

        VertexConsumerProvider.Immediate renderer = MinecraftClient.getInstance()
                                                                   .getBufferBuilders()
                                                                   .getEntityVertexConsumers();
        Matrix4f matrix = transform.peek()
                                   .getModel();
        drawBorder(matrix, renderer, this.x, this.y, this.getZOffset(), this.m_page, this.m_pages, this.m_book);
        drawText(matrix, renderer, this.x + X_TEXT_MARGIN, this.y + Y_TEXT_MARGIN, ItemPrintout.LINES_PER_PAGE * this.m_page, this.m_text, this.m_colours);
        renderer.draw();
    }

    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        if (super.keyPressed(key, scancode, modifiers)) {
            return true;
        }

        if (key == GLFW.GLFW_KEY_RIGHT) {
            if (this.m_page < this.m_pages - 1) {
                this.m_page++;
            }
            return true;
        }

        if (key == GLFW.GLFW_KEY_LEFT) {
            if (this.m_page > 0) {
                this.m_page--;
            }
            return true;
        }

        return false;
    }
}
