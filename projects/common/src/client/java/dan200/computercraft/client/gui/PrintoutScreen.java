// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.client.gui;

import com.mojang.blaze3d.vertex.Tesselator;
import dan200.computercraft.core.terminal.TextBuffer;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.media.PrintoutMenu;
import dan200.computercraft.shared.media.items.PrintoutItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;
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
        var text = PrintoutItem.getText(stack);
        var textBuffers = new TextBuffer[text.length];
        for (var i = 0; i < textBuffers.length; i++) textBuffers[i] = new TextBuffer(text[i]);

        var colours = PrintoutItem.getColours(stack);
        var colourBuffers = new TextBuffer[colours.length];
        for (var i = 0; i < colours.length; i++) colourBuffers[i] = new TextBuffer(colours[i]);

        var pages = Math.max(text.length / PrintoutItem.LINES_PER_PAGE, 1);
        var book = stack.is(ModRegistry.Items.PRINTED_BOOK.get());

        printout = new PrintoutInfo(pages, book, textBuffers, colourBuffers);
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
    public boolean mouseScrolled(double x, double y, double delta) {
        if (super.mouseScrolled(x, y, delta)) return true;
        if (delta < 0) {
            // Scroll up goes to the next page
            nextPage();
            return true;
        }

        if (delta > 0) {
            // Scroll down goes to the previous page
            previousPage();
            return true;
        }

        return false;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        // Draw the printout
        var renderer = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());

        drawBorder(graphics.pose(), renderer, leftPos, topPos, 0, page, printout.pages(), printout.book(), FULL_BRIGHT_LIGHTMAP);
        drawText(graphics.pose(), renderer, leftPos + X_TEXT_MARGIN, topPos + Y_TEXT_MARGIN, PrintoutItem.LINES_PER_PAGE * page, FULL_BRIGHT_LIGHTMAP, printout.text(), printout.colour());
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

    record PrintoutInfo(int pages, boolean book, TextBuffer[] text, TextBuffer[] colour) {
        public static final PrintoutInfo DEFAULT;

        static {
            var textLines = new TextBuffer[PrintoutItem.LINES_PER_PAGE];
            Arrays.fill(textLines, new TextBuffer(" ".repeat(PrintoutItem.LINE_MAX_LENGTH)));

            var colourLines = new TextBuffer[PrintoutItem.LINES_PER_PAGE];
            Arrays.fill(colourLines, new TextBuffer("f".repeat(PrintoutItem.LINE_MAX_LENGTH)));

            DEFAULT = new PrintoutInfo(1, false, textLines, colourLines);
        }
    }
}
