// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import dan200.computercraft.client.render.PrintoutRenderer;
import dan200.computercraft.core.terminal.TextBuffer;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.media.PrintoutMenu;
import dan200.computercraft.shared.media.items.PrintoutData;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;

import static dan200.computercraft.client.render.PrintoutRenderer.*;

/**
 * The GUI for printed pages and books.
 *
 * @see PrintoutRenderer
 */
public final class PrintoutScreen extends AbstractContainerScreen<PrintoutMenu> implements ContainerListener {
    private PrintoutInfo printout = PrintoutInfo.DEFAULT;
    private int page = 0;

    public PrintoutScreen(PrintoutMenu container, Inventory player, Component title) {
        super(container, player, title, DEFAULT_IMAGE_WIDTH, Y_SIZE);
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
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_RIGHT) {
            nextPage();
            return true;
        }

        if (event.key() == GLFW.GLFW_KEY_LEFT) {
            previousPage();
            return true;
        }

        return super.keyPressed(event);
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
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        super.extractBackground(graphics, mouseX, mouseY, partialTicks);
        // Push the printout slightly forward, to avoid clipping into the background.
        graphics.guiRenderState.addPicturesInPictureState(new PrintoutRenderState(
            leftPos - COVER_SIZE - 32, leftPos + X_SIZE + COVER_SIZE + 32,
            topPos - COVER_SIZE, topPos + Y_SIZE + COVER_SIZE,
            printout, page, new Matrix3x2f(graphics.pose()), graphics.scissorStack.peek()
        ));
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // Skip rendering labels.
    }

    @SuppressWarnings("ArrayRecordComponent")
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

    public record PrintoutRenderState(
        int x0, int x1, int y0, int y1, PrintoutInfo printout, int page, Matrix3x2f pose,
        @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds
    ) implements PictureInPictureRenderState {
        private PrintoutRenderState(
            int x0, int x1, int y0, int y1, PrintoutInfo printout, int page, Matrix3x2f pose, @Nullable ScreenRectangle scissorArea
        ) {
            this(x0, x1, y0, y1, printout, page, pose, scissorArea, PictureInPictureRenderState.getBounds(x0, x1, y0, y1, scissorArea));
        }

        @Override
        public float scale() {
            return 1.0f;
        }
    }

    /**
     * PIP renderer for printouts.
     * <p>
     * We prefer using a PIP (rather than a {@link GuiElementRenderState}), as {@link PrintoutRenderer} renders with
     * multiple z-levels.
     */
    public static final class PrintoutPictureRenderer extends PictureInPictureRenderer<PrintoutRenderState> {
        public PrintoutPictureRenderer(MultiBufferSource.BufferSource bufferSource) {
            super(bufferSource);
        }

        @Override
        protected void renderToTexture(PrintoutRenderState state, PoseStack pose) {
            pose.pushPose();
            pose.translate(-0.5f * X_SIZE, -(Y_SIZE + COVER_SIZE), 0);
            pose.scale(1.0f, 1.0f, -1.0f);

            var buffer = bufferSource.getBuffer(PrintoutRenderer.BACKGROUND);
            drawBorder(pose.last().pose(), buffer, 0, 0, 0, state.page(), state.printout().pages(), state.printout().book(), LightCoordsUtil.FULL_BRIGHT);

            drawText(
                pose, bufferSource, X_TEXT_MARGIN, Y_TEXT_MARGIN, PrintoutData.LINES_PER_PAGE * state.page(), LightCoordsUtil.FULL_BRIGHT,
                state.printout().text(), state.printout().colour()
            );
            pose.popPose();
        }

        @Override
        public Class<PrintoutRenderState> getRenderStateClass() {
            return PrintoutRenderState.class;
        }

        @Override
        protected String getTextureLabel() {
            return "Printout";
        }
    }
}
