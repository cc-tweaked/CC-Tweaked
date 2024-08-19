// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.client.gui;

import dan200.computercraft.core.terminal.TextBuffer;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.media.PrintoutMenu;
import dan200.computercraft.shared.media.items.PrintoutData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;

import static dan200.computercraft.client.render.PrintoutRenderer.*;
import static dan200.computercraft.client.render.RenderTypes.FULL_BRIGHT_LIGHTMAP;

/**
 * The GUI for printed pages and books.
 *
 * @see dan200.computercraft.client.render.PrintoutRenderer
 */
public final class PrintoutScreen extends AbstractContainerScreen<PrintoutMenu> implements ContainerListener {
    private PrintoutInfo printout = PrintoutInfo.DEFAULT;
    private int page = 0;

    public PrintoutScreen(PrintoutMenu container, Inventory player, Component title) {
        super(container, player, title);
        imageHeight = Y_SIZE;
    }

    private void setPrintout(ItemStack stack) {
        page = 0;
        printout = PrintoutInfo.of(PrintoutData.getOrEmpty(stack), stack.is(ModRegistry.Items.PRINTED_BOOK.get()));
    }

    @Override
    protected void init() {
        super.init();
        menu.addSlotListener(this);
    }

    @Override
    public void removed() {
        menu.removeSlotListener(this);
    }

    @Override
    public void slotChanged(AbstractContainerMenu menu, int slot, ItemStack stack) {
        if (slot == 0) setPrintout(stack);
    }

    @Override
    public void dataChanged(AbstractContainerMenu menu, int slot, int data) {
        if (slot == PrintoutMenu.DATA_CURRENT_PAGE) page = data;
    }

    private void setPage(int page) {
        this.page = page;

        var gameMode = Objects.requireNonNull(Objects.requireNonNull(minecraft).gameMode);
        gameMode.handleInventoryButtonClick(menu.containerId, PrintoutMenu.PAGE_BUTTON_OFFSET + page);
    }

    private void previousPage() {
        if (page > 0) setPage(page - 1);
    }

    private void nextPage() {
        if (page < printout.pages() - 1) setPage(page + 1);
    }

    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        if (key == GLFW.GLFW_KEY_RIGHT) {
            nextPage();
            return true;
        }

        if (key == GLFW.GLFW_KEY_LEFT) {
            previousPage();
            return true;
        }

        return super.keyPressed(key, scancode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double deltaX, double deltaY) {
        if (super.mouseScrolled(x, y, deltaX, deltaY)) return true;
        if (deltaY < 0) {
            // Scroll up goes to the next page
            nextPage();
            return true;
        }

        if (deltaY > 0) {
            // Scroll down goes to the previous page
            previousPage();
            return true;
        }

        return false;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        // Push the printout slightly forward, to avoid clipping into the background.
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 1);

        drawBorder(graphics.pose(), graphics.bufferSource(), leftPos, topPos, 0, page, printout.pages(), printout.book(), FULL_BRIGHT_LIGHTMAP);
        drawText(graphics.pose(), graphics.bufferSource(), leftPos + X_TEXT_MARGIN, topPos + Y_TEXT_MARGIN, PrintoutData.LINES_PER_PAGE * page, FULL_BRIGHT_LIGHTMAP, printout.text(), printout.colour());

        graphics.pose().popPose();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Skip rendering labels.
    }

    record PrintoutInfo(int pages, boolean book, TextBuffer[] text, TextBuffer[] colour) {
        public static final PrintoutInfo DEFAULT = of(PrintoutData.EMPTY, false);

        public static PrintoutInfo of(PrintoutData printout, boolean book) {
            var text = new TextBuffer[printout.lines().size()];
            var colours = new TextBuffer[printout.lines().size()];
            for (var i = 0; i < text.length; i++) {
                var line = printout.lines().get(i);
                text[i] = new TextBuffer(line.text());
                colours[i] = new TextBuffer(line.foreground());
            }

            var pages = Math.max(text.length / PrintoutData.LINES_PER_PAGE, 1);
            return new PrintoutInfo(pages, book, text, colours);
        }
    }
}
