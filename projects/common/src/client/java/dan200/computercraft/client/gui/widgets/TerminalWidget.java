// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.client.gui.widgets;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dan200.computercraft.client.gui.KeyConverter;
import dan200.computercraft.client.render.text.FixedWidthFontRenderer;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.core.util.StringUtil;
import dan200.computercraft.shared.computer.core.InputHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
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
    public boolean charTyped(CharacterEvent event) {
        var terminalChar = StringUtil.unicodeToTerminal(event.codepoint());
        if (StringUtil.isTypableChar(terminalChar)) computer.charTyped((byte) terminalChar);
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) return false;
        if (event.isPaste()) {
            paste();
            return true;
        }

        if ((event.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0) {
            switch (KeyConverter.physicalToActual(event.key(), event.scancode())) {
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

        if (event.key() >= 0 && terminateTimer < KEY_SUPPRESS_DELAY && rebootTimer < KEY_SUPPRESS_DELAY && shutdownTimer < KEY_SUPPRESS_DELAY) {
            // Queue the "key" event and add to the down set
            var repeat = keysDown.get(event.key());
            keysDown.set(event.key());
            computer.keyDown(event.key(), repeat);
        }

        return true;
    }

    private void paste() {
        var clipboard = StringUtil.getClipboardString(Minecraft.getInstance().keyboardHandler.getClipboard());
        if (clipboard.remaining() > 0) computer.paste(clipboard);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        // Queue the "key_up" event and remove from the down set
        if (event.key() >= 0 && keysDown.get(event.key())) {
            keysDown.set(event.key(), false);
            computer.keyUp(event.key());
        }

        switch (KeyConverter.physicalToActual(event.key(), event.scancode())) {
            case GLFW.GLFW_KEY_T -> terminateTimer = -1;
            case GLFW.GLFW_KEY_R -> rebootTimer = -1;
            case GLFW.GLFW_KEY_S -> shutdownTimer = -1;
            case GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL ->
                terminateTimer = rebootTimer = shutdownTimer = -1;
        }

        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!inTermRegion(event.x(), event.y())) return false;
        if (!hasMouseSupport() || event.button() < 0 || event.button() > 2) return false;

        var charX = (int) ((event.x() - innerX) / FONT_WIDTH);
        var charY = (int) ((event.y() - innerY) / FONT_HEIGHT);
        charX = Math.min(Math.max(charX, 0), terminal.getWidth() - 1);
        charY = Math.min(Math.max(charY, 0), terminal.getHeight() - 1);

        computer.mouseClick(event.button() + 1, charX + 1, charY + 1);

        lastMouseButton = event.button();
        lastMouseX = charX;
        lastMouseY = charY;

        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (!inTermRegion(event.x(), event.y())) return false;
        if (!hasMouseSupport() || event.button() < 0 || event.button() > 2) return false;

        var charX = (int) ((event.x() - innerX) / FONT_WIDTH);
        var charY = (int) ((event.y() - innerY) / FONT_HEIGHT);
        charX = Math.min(Math.max(charX, 0), terminal.getWidth() - 1);
        charY = Math.min(Math.max(charY, 0), terminal.getHeight() - 1);

        if (lastMouseButton == event.button()) {
            computer.mouseUp(lastMouseButton + 1, charX + 1, charY + 1);
            lastMouseButton = -1;
        }

        lastMouseX = charX;
        lastMouseY = charY;

        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double v2, double v3) {
        if (!inTermRegion(event.x(), event.y())) return false;
        if (!hasMouseSupport() || event.button() < 0 || event.button() > 2) return false;

        var charX = (int) ((event.x() - innerX) / FONT_WIDTH);
        var charY = (int) ((event.y() - innerY) / FONT_HEIGHT);
        charX = Math.min(Math.max(charX, 0), terminal.getWidth() - 1);
        charY = Math.min(Math.max(charY, 0), terminal.getHeight() - 1);

        if (event.button() == lastMouseButton && (charX != lastMouseX || charY != lastMouseY)) {
            computer.mouseDrag(event.button() + 1, charX + 1, charY + 1);
            lastMouseX = charX;
            lastMouseY = charY;
        }

        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (!inTermRegion(mouseX, mouseY)) return false;
        if (!hasMouseSupport() || deltaY == 0) return false;

        var charX = (int) ((mouseX - innerX) / FONT_WIDTH);
        var charY = (int) ((mouseY - innerY) / FONT_HEIGHT);
        charX = Math.min(Math.max(charX, 0), terminal.getWidth() - 1);
        charY = Math.min(Math.max(charY, 0), terminal.getHeight() - 1);

        computer.mouseScroll(deltaY < 0 ? 1 : -1, charX + 1, charY + 1);

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
            computer.terminate();
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

        var scissor = graphics.scissorStack.peek();
        var terminalPose = new Matrix3x2f(graphics.pose());
        var terminalTextures = TextureSetup.singleTextureWithLightmap(
            graphics.minecraft.getTextureManager().getTexture(FixedWidthFontRenderer.FONT).getTextureView(),
            RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST)
        );

        graphics.guiRenderState.submitGuiElement(new TerminalBackgroundRenderState(
            innerX, innerY, terminal, terminalPose, terminalTextures,
            maybeIntersect(scissor, new ScreenRectangle(
                innerX, innerY, terminal.getWidth() * FONT_WIDTH, terminal.getHeight() * FONT_HEIGHT).transformMaxBounds(graphics.pose())
            ),
            scissor
        ));

        graphics.guiRenderState.submitGuiElement(new TerminalTextRenderState(
            innerX, innerY, terminal, terminalPose, terminalTextures,
            maybeIntersect(scissor, new ScreenRectangle(getX(), getY(), getWidth(), getHeight()).transformMaxBounds(graphics.pose())),
            scissor
        ));
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

    private static @Nullable ScreenRectangle maybeIntersect(@Nullable ScreenRectangle scissor, ScreenRectangle bounds) {
        return scissor == null ? bounds : bounds.intersection(scissor);
    }

    private record TerminalBackgroundRenderState(
        int x, int y, Terminal terminal,
        Matrix3x2f pose,
        TextureSetup textureSetup,
        @Nullable ScreenRectangle bounds,
        @Nullable ScreenRectangle scissorArea
    ) implements GuiElementRenderState {
        @Override
        public void buildVertices(VertexConsumer buffer) {
            FixedWidthFontRenderer.drawTerminalBackground(new Matrix4f().mul(pose), buffer, x, y, terminal, MARGIN, MARGIN, MARGIN, MARGIN);
        }

        @Override
        public RenderPipeline pipeline() {
            return RenderPipelines.TEXT;
        }
    }

    private record TerminalTextRenderState(
        int x, int y, Terminal terminal, Matrix3x2f pose, TextureSetup textureSetup,
        @Nullable ScreenRectangle bounds, @Nullable ScreenRectangle scissorArea
    ) implements GuiElementRenderState {
        @Override
        public void buildVertices(VertexConsumer buffer) {
            var transform = new Matrix4f().mul(pose);
            FixedWidthFontRenderer.drawTerminalForeground(transform, buffer, x, y, terminal);
            FixedWidthFontRenderer.drawCursor(transform, buffer, x, y, terminal);

            // The GUI renderer requires that the buffer is non-empty. Add a zero-size vertex so we always have something.
            for (var i = 0; i < 4; i++) {
                buffer.addVertex(0, 0, 0).setColor(0x00ffffff).setUv(0, 0).setLight(LightTexture.FULL_BRIGHT);
            }
        }

        @Override
        public RenderPipeline pipeline() {
            return RenderPipelines.TEXT;
        }
    }
}
