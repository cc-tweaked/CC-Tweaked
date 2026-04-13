// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.client.gui.widgets;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dan200.computercraft.client.gui.ClientComputerActions;
import dan200.computercraft.client.gui.ClientComputerInput;
import dan200.computercraft.client.gui.KeyConverter;
import dan200.computercraft.client.render.text.FixedWidthFontRenderer;
import dan200.computercraft.core.input.UserComputerInput;
import dan200.computercraft.core.terminal.Terminal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.LightCoordsUtil;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
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
    public boolean charTyped(CharacterEvent event) {
        computerInput.codepointTyped(event.codepoint());
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
            computerInput.keyDown(event.key());
        }

        return true;
    }

    private void paste() {
        computerInput.paste(Minecraft.getInstance().keyboardHandler.getClipboard());
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        computerInput.keyUp(event.key());

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

        var charX = (int) ((event.x() - innerX) / FONT_WIDTH);
        var charY = (int) ((event.y() - innerY) / FONT_HEIGHT);
        computerInput.mouseClick(event.button() + 1, charX + 1, charY + 1);

        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (!inTermRegion(event.x(), event.y())) return false;

        var charX = (int) ((event.x() - innerX) / FONT_WIDTH);
        var charY = (int) ((event.y() - innerY) / FONT_HEIGHT);
        computerInput.mouseUp(event.button() + 1, charX + 1, charY + 1);

        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double v2, double v3) {
        if (!inTermRegion(event.x(), event.y())) return false;

        var charX = (int) ((event.x() - innerX) / FONT_WIDTH);
        var charY = (int) ((event.y() - innerY) / FONT_HEIGHT);
        computerInput.mouseDrag(event.button() + 1, charX + 1, charY + 1);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (!inTermRegion(mouseX, mouseY)) return false;
        if (deltaY == 0) return false;

        var charX = (int) ((mouseX - innerX) / FONT_WIDTH);
        var charY = (int) ((mouseY - innerY) / FONT_HEIGHT);
        computerInput.mouseScroll(deltaY < 0 ? 1 : -1, charX + 1, charY + 1);

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
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        if (!visible) return;

        var scissor = graphics.scissorStack.peek();
        var terminalPose = new Matrix3x2f(graphics.pose());
        var terminalTextures = TextureSetup.singleTextureWithLightmap(
            graphics.minecraft.getTextureManager().getTexture(FixedWidthFontRenderer.FONT).getTextureView(),
            RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST)
        );

        graphics.guiRenderState.addGuiElement(new TerminalBackgroundRenderState(
            innerX, innerY, terminal, terminalPose, terminalTextures,
            maybeIntersect(scissor, new ScreenRectangle(
                innerX, innerY, terminal.getWidth() * FONT_WIDTH, terminal.getHeight() * FONT_HEIGHT).transformMaxBounds(graphics.pose())
            ),
            scissor
        ));

        graphics.guiRenderState.addGuiElement(new TerminalTextRenderState(
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
                buffer.addVertex(0, 0, 0).setColor(0x00ffffff).setUv(0, 0).setLight(LightCoordsUtil.FULL_BRIGHT);
            }
        }

        @Override
        public RenderPipeline pipeline() {
            return RenderPipelines.TEXT;
        }
    }
}
