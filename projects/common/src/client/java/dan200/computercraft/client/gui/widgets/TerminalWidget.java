// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.client.gui.widgets;

import com.mojang.blaze3d.vertex.Tesselator;
import dan200.computercraft.client.gui.ClientComputerActions;
import dan200.computercraft.client.gui.ClientComputerInput;
import dan200.computercraft.client.gui.KeyConverter;
import dan200.computercraft.client.render.RenderTypes;
import dan200.computercraft.client.render.text.FixedWidthFontRenderer;
import dan200.computercraft.core.input.UserComputerInput;
import dan200.computercraft.core.terminal.Terminal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import static dan200.computercraft.client.render.ComputerBorderRenderer.MARGIN;
import static dan200.computercraft.client.render.text.FixedWidthFontRenderer.FONT_HEIGHT;
import static dan200.computercraft.client.render.text.FixedWidthFontRenderer.FONT_WIDTH;

/**
 * A widget which renders a computer terminal and handles input events (keyboard, mouse, clipboard) and computer
 * shortcuts (terminate/shutdown/reboot).
 *
 * @see ClientComputerInput The input handler typically used with this class.
 */
public class TerminalWidget extends AbstractWidget {
    private static final Component DESCRIPTION = Component.translatable("gui.computercraft.terminal");

    private static final float TERMINATE_TIME = 0.5f;
    private static final float KEY_SUPPRESS_DELAY = 0.2f;

    private final Terminal terminal;
    private final UserComputerInput computerInput;
    private final ClientComputerActions computerActions;

    // The positions of the actual terminal
    private final int innerX;
    private final int innerY;
    private final int innerWidth;
    private final int innerHeight;

    private float terminateTimer = -1;
    private float rebootTimer = -1;
    private float shutdownTimer = -1;

    public TerminalWidget(Terminal terminal, UserComputerInput computerInput, ClientComputerActions computerActions, int x, int y) {
        super(x, y, terminal.getWidth() * FONT_WIDTH + MARGIN * 2, terminal.getHeight() * FONT_HEIGHT + MARGIN * 2, DESCRIPTION);

        this.terminal = terminal;
        this.computerInput = computerInput;
        this.computerActions = computerActions;

        innerX = x + MARGIN;
        innerY = y + MARGIN;
        innerWidth = terminal.getWidth() * FONT_WIDTH;
        innerHeight = terminal.getHeight() * FONT_HEIGHT;
    }

    @Override
    public boolean charTyped(char ch, int modifiers) {
        computerInput.codepointTyped(ch);
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        if (key == GLFW.GLFW_KEY_ESCAPE) return false;
        if (Screen.isPaste(key)) {
            paste();
            return true;
        }

        if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            switch (KeyConverter.physicalToActual(key, scancode)) {
                case GLFW.GLFW_KEY_T -> {
                    if (terminateTimer < 0) terminateTimer = 0;
                }
                case GLFW.GLFW_KEY_S -> {
                    if (shutdownTimer < 0) shutdownTimer = 0;
                }
                case GLFW.GLFW_KEY_R -> {
                    if (rebootTimer < 0) rebootTimer = 0;
                }
            }
        }

        if (key >= 0 && terminateTimer < KEY_SUPPRESS_DELAY && rebootTimer < KEY_SUPPRESS_DELAY && shutdownTimer < KEY_SUPPRESS_DELAY) {
            computerInput.keyDown(key);
        }

        return true;
    }

    private void paste() {
        computerInput.paste(Minecraft.getInstance().keyboardHandler.getClipboard());
    }

    @Override
    public boolean keyReleased(int key, int scancode, int modifiers) {
        computerInput.keyUp(key);

        switch (KeyConverter.physicalToActual(key, scancode)) {
            case GLFW.GLFW_KEY_T -> terminateTimer = -1;
            case GLFW.GLFW_KEY_R -> rebootTimer = -1;
            case GLFW.GLFW_KEY_S -> shutdownTimer = -1;
            case GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL ->
                terminateTimer = rebootTimer = shutdownTimer = -1;
        }

        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!inTermRegion(mouseX, mouseY)) return false;

        var charX = (int) ((mouseX - innerX) / FONT_WIDTH);
        var charY = (int) ((mouseY - innerY) / FONT_HEIGHT);
        computerInput.mouseClick(button + 1, charX + 1, charY + 1);

        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!inTermRegion(mouseX, mouseY)) return false;

        var charX = (int) ((mouseX - innerX) / FONT_WIDTH);
        var charY = (int) ((mouseY - innerY) / FONT_HEIGHT);
        computerInput.mouseUp(button + 1, charX + 1, charY + 1);

        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double v2, double v3) {
        if (!inTermRegion(mouseX, mouseY)) return false;

        var charX = (int) ((mouseX - innerX) / FONT_WIDTH);
        var charY = (int) ((mouseY - innerY) / FONT_HEIGHT);
        computerInput.mouseDrag(button + 1, charX + 1, charY + 1);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!inTermRegion(mouseX, mouseY)) return false;
        if (delta == 0) return false;

        var charX = (int) ((mouseX - innerX) / FONT_WIDTH);
        var charY = (int) ((mouseY - innerY) / FONT_HEIGHT);
        computerInput.mouseScroll(delta < 0 ? 1 : -1, charX + 1, charY + 1);

        return true;
    }

    private boolean inTermRegion(double mouseX, double mouseY) {
        return active && visible && mouseX >= innerX && mouseY >= innerY && mouseX < innerX + innerWidth && mouseY < innerY + innerHeight;
    }

    public void update() {
        if (terminateTimer >= 0 && terminateTimer < TERMINATE_TIME && (terminateTimer += 0.05f) > TERMINATE_TIME) {
            computerActions.terminate();
        }

        if (shutdownTimer >= 0 && shutdownTimer < TERMINATE_TIME && (shutdownTimer += 0.05f) > TERMINATE_TIME) {
            computerActions.shutdown();
        }

        if (rebootTimer >= 0 && rebootTimer < TERMINATE_TIME && (rebootTimer += 0.05f) > TERMINATE_TIME) {
            computerActions.reboot();
        }
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);

        if (!focused) {
            computerInput.releaseInputs();
            shutdownTimer = terminateTimer = rebootTimer = -1;
        }
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (!visible) return;

        var bufferSource = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        var emitter = FixedWidthFontRenderer.toVertexConsumer(graphics.pose(), bufferSource.getBuffer(RenderTypes.TERMINAL));

        FixedWidthFontRenderer.drawTerminal(
            emitter,
            (float) innerX, (float) innerY, terminal, (float) MARGIN, (float) MARGIN, (float) MARGIN, (float) MARGIN
        );

        bufferSource.endBatch();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
    }

    public static int getWidth(int termWidth) {
        return termWidth * FONT_WIDTH + MARGIN * 2;
    }

    public static int getHeight(int termHeight) {
        return termHeight * FONT_HEIGHT + MARGIN * 2;
    }
}
