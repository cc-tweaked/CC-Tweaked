// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.client.gui;

import dan200.computercraft.core.terminal.TextBuffer;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.common.HeldItemMenu;
import dan200.computercraft.shared.media.items.PrintoutData;
import dan200.computercraft.shared.media.items.PrintoutItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
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

        var printout = container.getStack().getOrDefault(ModRegistry.DataComponents.PRINTOUT.get(), PrintoutData.EMPTY);
        this.text = new TextBuffer[printout.lines().size()];
        this.colours = new TextBuffer[printout.lines().size()];
        for (var i = 0; i < this.text.length; i++) {
            var line = printout.lines().get(i);
            this.text[i] = new TextBuffer(line.text());
            this.colours[i] = new TextBuffer(line.foreground());
        }

        page = 0;
        pages = Math.max(this.text.length / PrintoutData.LINES_PER_PAGE, 1);
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
    public boolean mouseScrolled(double x, double y, double deltaX, double deltaY) {
        if (super.mouseScrolled(x, y, deltaX, deltaY)) return true;
        if (deltaY < 0) {
            // Scroll up goes to the next page
            if (page < pages - 1) page++;
            return true;
        }

        if (deltaY > 0) {
            // Scroll down goes to the previous page
            if (page > 0) page--;
            return true;
        }

        return false;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        // Push the printout slightly forward, to avoid clipping into the background.
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 1);

        drawBorder(graphics.pose(), graphics.bufferSource(), leftPos, topPos, 0, page, pages, book, FULL_BRIGHT_LIGHTMAP);
        drawText(graphics.pose(), graphics.bufferSource(), leftPos + X_TEXT_MARGIN, topPos + Y_TEXT_MARGIN, PrintoutData.LINES_PER_PAGE * page, FULL_BRIGHT_LIGHTMAP, text, colours);

        graphics.pose().popPose();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Skip rendering labels.
    }
}
