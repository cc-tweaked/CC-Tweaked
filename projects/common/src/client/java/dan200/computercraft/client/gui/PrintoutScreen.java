// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.client.gui;

import com.mojang.blaze3d.vertex.Tesselator;
import dan200.computercraft.core.terminal.TextBuffer;
import dan200.computercraft.shared.common.HeldItemMenu;
import dan200.computercraft.shared.media.items.PrintoutItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

import static dan200.computercraft.client.render.PrintoutRenderer.*;
import static dan200.computercraft.client.render.RenderTypes.FULL_BRIGHT_LIGHTMAP;

/**
 * The GUI for printed pages and books.
 *
 * @see dan200.computercraft.client.render.PrintoutRenderer
 */
public class PrintoutScreen extends AbstractContainerScreen<HeldItemMenu> {
    private final boolean book;
    private final int pages;
    private final TextBuffer[] text;
    private final TextBuffer[] colours;
    private int page;

    public PrintoutScreen(HeldItemMenu container, Inventory player, Component title) {
        super(container, player, title);

        imageHeight = Y_SIZE;

        var text = PrintoutItem.getText(container.getStack());
        this.text = new TextBuffer[text.length];
        for (var i = 0; i < this.text.length; i++) this.text[i] = new TextBuffer(text[i]);

        var colours = PrintoutItem.getColours(container.getStack());
        this.colours = new TextBuffer[colours.length];
        for (var i = 0; i < this.colours.length; i++) this.colours[i] = new TextBuffer(colours[i]);

        page = 0;
        pages = Math.max(this.text.length / PrintoutItem.LINES_PER_PAGE, 1);
        book = ((PrintoutItem) container.getStack().getItem()).getType() == PrintoutItem.Type.BOOK;
    }

    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        if (key == GLFW.GLFW_KEY_RIGHT) {
            if (page < pages - 1) page++;
            return true;
        }

        if (key == GLFW.GLFW_KEY_LEFT) {
            if (page > 0) page--;
            return true;
        }

        return super.keyPressed(key, scancode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double delta) {
        if (super.mouseScrolled(x, y, delta)) return true;
        if (delta < 0) {
            // Scroll up goes to the next page
            if (page < pages - 1) page++;
            return true;
        }

        if (delta > 0) {
            // Scroll down goes to the previous page
            if (page > 0) page--;
            return true;
        }

        return false;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        // Draw the printout
        var renderer = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        drawBorder(graphics.pose(), renderer, leftPos, topPos, 0, page, pages, book, FULL_BRIGHT_LIGHTMAP);
        drawText(graphics.pose(), renderer, leftPos + X_TEXT_MARGIN, topPos + Y_TEXT_MARGIN, PrintoutItem.LINES_PER_PAGE * page, FULL_BRIGHT_LIGHTMAP, text, colours);
        renderer.endBatch();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // We must take the background further back in order to not overlap with our printed pages.
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, -1);
        renderBackground(graphics);
        graphics.pose().popPose();

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Skip rendering labels.
    }
}
