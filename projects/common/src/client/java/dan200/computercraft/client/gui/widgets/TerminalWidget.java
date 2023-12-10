// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.client.gui.widgets;

import com.mojang.blaze3d.vertex.Tesselator;
import dan200.computercraft.client.render.RenderTypes;
import dan200.computercraft.client.render.text.FixedWidthFontRenderer;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.core.util.StringUtil;
import dan200.computercraft.shared.computer.core.InputHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.BitSet;

import static dan200.computercraft.client.render.ComputerBorderRenderer.MARGIN;
import static dan200.computercraft.client.render.text.FixedWidthFontRenderer.FONT_HEIGHT;
import static dan200.computercraft.client.render.text.FixedWidthFontRenderer.FONT_WIDTH;

/**
 * A widget which renders a computer terminal and handles input events (keyboard, mouse, clipboard) and computer
 * shortcuts (terminate/shutdown/reboot).
 *
 * @see dan200.computercraft.client.gui.ClientInputHandler The input handler typically used with this class.
 */
public class TerminalWidget extends AbstractWidget {
    private static final Component DESCRIPTION = Component.translatable("gui.computercraft.terminal");

    private static final float TERMINATE_TIME = 0.5f;
    private static final float KEY_SUPPRESS_DELAY = 0.2f;

    private final Terminal terminal;
    private final InputHandler computer;

    // The positions of the actual terminal
    private final int innerX;
    private final int innerY;
    private final int innerWidth;
    private final int innerHeight;

    private float terminateTimer = -1;
    private float rebootTimer = -1;
    private float shutdownTimer = -1;

    private int lastMouseButton = -1;
    private int lastMouseX = -1;
    private int lastMouseY = -1;

    private final BitSet keysDown = new BitSet(256);

    public TerminalWidget(Terminal terminal, InputHandler computer, int x, int y) {
        super(x, y, terminal.getWidth() * FONT_WIDTH + MARGIN * 2, terminal.getHeight() * FONT_HEIGHT + MARGIN * 2, DESCRIPTION);

        this.terminal = terminal;
        this.computer = computer;

        innerX = x + MARGIN;
        innerY = y + MARGIN;
        innerWidth = terminal.getWidth() * FONT_WIDTH;
        innerHeight = terminal.getHeight() * FONT_HEIGHT;
    }

    @Override
    public boolean charTyped(char ch, int modifiers) {
        if (ch >= 32 && ch <= 126 || ch >= 160 && ch <= 255) {
            // Queue the char event for any printable chars in byte range
            computer.queueEvent("char", new Object[]{ Character.toString(ch) });
        }

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
            switch (key) {
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
            // Queue the "key" event and add to the down set
            var repeat = keysDown.get(key);
            keysDown.set(key);
            computer.keyDown(key, repeat);
        }

        return true;
    }

    private void paste() {
        var clipboard = StringUtil.normaliseClipboardString(Minecraft.getInstance().keyboardHandler.getClipboard());
        if (!clipboard.isEmpty()) computer.queueEvent("paste", new Object[]{ clipboard });
    }

    @Override
    public boolean keyReleased(int key, int scancode, int modifiers) {
        // Queue the "key_up" event and remove from the down set
        if (key >= 0 && keysDown.get(key)) {
            keysDown.set(key, false);
            computer.keyUp(key);
        }

        switch (key) {
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
        if (!hasMouseSupport() || button < 0 || button > 2) return false;

        var charX = (int) ((mouseX - innerX) / FONT_WIDTH);
        var charY = (int) ((mouseY - innerY) / FONT_HEIGHT);
        charX = Math.min(Math.max(charX, 0), terminal.getWidth() - 1);
        charY = Math.min(Math.max(charY, 0), terminal.getHeight() - 1);

        computer.mouseClick(button + 1, charX + 1, charY + 1);

        lastMouseButton = button;
        lastMouseX = charX;
        lastMouseY = charY;

        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!inTermRegion(mouseX, mouseY)) return false;
        if (!hasMouseSupport() || button < 0 || button > 2) return false;

        var charX = (int) ((mouseX - innerX) / FONT_WIDTH);
        var charY = (int) ((mouseY - innerY) / FONT_HEIGHT);
        charX = Math.min(Math.max(charX, 0), terminal.getWidth() - 1);
        charY = Math.min(Math.max(charY, 0), terminal.getHeight() - 1);

        if (lastMouseButton == button) {
            computer.mouseUp(lastMouseButton + 1, charX + 1, charY + 1);
            lastMouseButton = -1;
        }

        lastMouseX = charX;
        lastMouseY = charY;

        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double v2, double v3) {
        if (!inTermRegion(mouseX, mouseY)) return false;
        if (!hasMouseSupport() || button < 0 || button > 2) return false;

        var charX = (int) ((mouseX - innerX) / FONT_WIDTH);
        var charY = (int) ((mouseY - innerY) / FONT_HEIGHT);
        charX = Math.min(Math.max(charX, 0), terminal.getWidth() - 1);
        charY = Math.min(Math.max(charY, 0), terminal.getHeight() - 1);

        if (button == lastMouseButton && (charX != lastMouseX || charY != lastMouseY)) {
            computer.mouseDrag(button + 1, charX + 1, charY + 1);
            lastMouseX = charX;
            lastMouseY = charY;
        }

        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!inTermRegion(mouseX, mouseY)) return false;
        if (!hasMouseSupport() || delta == 0) return false;

        var charX = (int) ((mouseX - innerX) / FONT_WIDTH);
        var charY = (int) ((mouseY - innerY) / FONT_HEIGHT);
        charX = Math.min(Math.max(charX, 0), terminal.getWidth() - 1);
        charY = Math.min(Math.max(charY, 0), terminal.getHeight() - 1);

        computer.mouseScroll(delta < 0 ? 1 : -1, charX + 1, charY + 1);

        lastMouseX = charX;
        lastMouseY = charY;

        return true;
    }

    private boolean inTermRegion(double mouseX, double mouseY) {
        return active && visible && mouseX >= innerX && mouseY >= innerY && mouseX < innerX + innerWidth && mouseY < innerY + innerHeight;
    }

    private boolean hasMouseSupport() {
        return terminal.isColour();
    }

    public void update() {
        if (terminateTimer >= 0 && terminateTimer < TERMINATE_TIME && (terminateTimer += 0.05f) > TERMINATE_TIME) {
            computer.queueEvent("terminate");
        }

        if (shutdownTimer >= 0 && shutdownTimer < TERMINATE_TIME && (shutdownTimer += 0.05f) > TERMINATE_TIME) {
            computer.shutdown();
        }

        if (rebootTimer >= 0 && rebootTimer < TERMINATE_TIME && (rebootTimer += 0.05f) > TERMINATE_TIME) {
            computer.reboot();
        }
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);

        if (!focused) {
            // When blurring, we should make all keys go up
            for (var key = 0; key < keysDown.size(); key++) {
                if (keysDown.get(key)) computer.keyUp(key);
            }
            keysDown.clear();

            // When blurring, we should make the last mouse button go up
            if (lastMouseButton >= 0) {
                computer.mouseUp(lastMouseButton + 1, lastMouseX + 1, lastMouseY + 1);
                lastMouseButton = -1;
            }

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
