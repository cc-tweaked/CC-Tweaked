/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.gui.widgets;

import static dan200.computercraft.client.gui.FixedWidthFontRenderer.BACKGROUND;

import java.util.BitSet;
import java.util.function.Supplier;

import com.mojang.blaze3d.platform.GlStateManager;
import dan200.computercraft.client.FrameInfo;
import dan200.computercraft.client.gui.FixedWidthFontRenderer;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.core.terminal.TextBuffer;
import dan200.computercraft.shared.computer.core.ClientComputer;
import dan200.computercraft.shared.computer.core.IComputer;
import dan200.computercraft.shared.util.Colour;
import dan200.computercraft.shared.util.Palette;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;

@Environment(EnvType.CLIENT)
public class WidgetTerminal implements Element {
    private static final float TERMINATE_TIME = 0.5f;

    private final MinecraftClient minecraft;

    private final Supplier<ClientComputer> computer;
    private final int termWidth;
    private final int termHeight;
    private final int leftMargin;
    private final int rightMargin;
    private final int topMargin;
    private final int bottomMargin;
    private final BitSet keysDown = new BitSet(256);
    private boolean focused;
    private float terminateTimer = -1;
    private float rebootTimer = -1;
    private float shutdownTimer = -1;
    private int lastMouseButton = -1;
    private int lastMouseX = -1;
    private int lastMouseY = -1;

    public WidgetTerminal(MinecraftClient minecraft, Supplier<ClientComputer> computer, int termWidth, int termHeight, int leftMargin, int rightMargin,
                          int topMargin, int bottomMargin) {
        this.minecraft = minecraft;
        this.computer = computer;
        this.termWidth = termWidth;
        this.termHeight = termHeight;
        this.leftMargin = leftMargin;
        this.rightMargin = rightMargin;
        this.topMargin = topMargin;
        this.bottomMargin = bottomMargin;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        ClientComputer computer = this.computer.get();
        if (computer == null || !computer.isColour() || button < 0 || button > 2) {
            return false;
        }

        Terminal term = computer.getTerminal();
        if (term != null) {
            int charX = (int) (mouseX / FixedWidthFontRenderer.FONT_WIDTH);
            int charY = (int) (mouseY / FixedWidthFontRenderer.FONT_HEIGHT);
            charX = Math.min(Math.max(charX, 0), term.getWidth() - 1);
            charY = Math.min(Math.max(charY, 0), term.getHeight() - 1);

            computer.mouseClick(button + 1, charX + 1, charY + 1);

            this.lastMouseButton = button;
            this.lastMouseX = charX;
            this.lastMouseY = charY;
        }

        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        ClientComputer computer = this.computer.get();
        if (computer == null || !computer.isColour() || button < 0 || button > 2) {
            return false;
        }

        Terminal term = computer.getTerminal();
        if (term != null) {
            int charX = (int) (mouseX / FixedWidthFontRenderer.FONT_WIDTH);
            int charY = (int) (mouseY / FixedWidthFontRenderer.FONT_HEIGHT);
            charX = Math.min(Math.max(charX, 0), term.getWidth() - 1);
            charY = Math.min(Math.max(charY, 0), term.getHeight() - 1);

            if (this.lastMouseButton == button) {
                computer.mouseUp(this.lastMouseButton + 1, charX + 1, charY + 1);
                this.lastMouseButton = -1;
            }

            this.lastMouseX = charX;
            this.lastMouseY = charY;
        }

        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double v2, double v3) {
        ClientComputer computer = this.computer.get();
        if (computer == null || !computer.isColour() || button < 0 || button > 2) {
            return false;
        }

        Terminal term = computer.getTerminal();
        if (term != null) {
            int charX = (int) (mouseX / FixedWidthFontRenderer.FONT_WIDTH);
            int charY = (int) (mouseY / FixedWidthFontRenderer.FONT_HEIGHT);
            charX = Math.min(Math.max(charX, 0), term.getWidth() - 1);
            charY = Math.min(Math.max(charY, 0), term.getHeight() - 1);

            computer.mouseDrag(button + 1, charX + 1, charY + 1);

            this.lastMouseX = charX;
            this.lastMouseY = charY;
            this.lastMouseButton = button;
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        ClientComputer computer = this.computer.get();
        if (computer == null || !computer.isColour() || delta == 0) {
            return false;
        }

        Terminal term = computer.getTerminal();
        if (term != null) {
            int charX = (int) (mouseX / FixedWidthFontRenderer.FONT_WIDTH);
            int charY = (int) (mouseY / FixedWidthFontRenderer.FONT_HEIGHT);
            charX = Math.min(Math.max(charX, 0), term.getWidth() - 1);
            charY = Math.min(Math.max(charY, 0), term.getHeight() - 1);

            computer.mouseScroll(delta < 0 ? 1 : -1, charX + 1, charY + 1);

            this.lastMouseX = charX;
            this.lastMouseY = charY;
        }

        return true;
    }

    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            return false;
        }
        if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            switch (key) {
            case GLFW.GLFW_KEY_T:
                if (this.terminateTimer < 0) {
                    this.terminateTimer = 0;
                }
                return true;
            case GLFW.GLFW_KEY_S:
                if (this.shutdownTimer < 0) {
                    this.shutdownTimer = 0;
                }
                return true;
            case GLFW.GLFW_KEY_R:
                if (this.rebootTimer < 0) {
                    this.rebootTimer = 0;
                }
                return true;

            case GLFW.GLFW_KEY_V:
                // Ctrl+V for paste
                String clipboard = this.minecraft.keyboard.getClipboard();
                if (clipboard != null) {
                    // Clip to the first occurrence of \r or \n
                    int newLineIndex1 = clipboard.indexOf("\r");
                    int newLineIndex2 = clipboard.indexOf("\n");
                    if (newLineIndex1 >= 0 && newLineIndex2 >= 0) {
                        clipboard = clipboard.substring(0, Math.min(newLineIndex1, newLineIndex2));
                    } else if (newLineIndex1 >= 0) {
                        clipboard = clipboard.substring(0, newLineIndex1);
                    } else if (newLineIndex2 >= 0) {
                        clipboard = clipboard.substring(0, newLineIndex2);
                    }

                    // Filter the string
                    clipboard = SharedConstants.stripInvalidChars(clipboard);
                    if (!clipboard.isEmpty()) {
                        // Clip to 512 characters and queue the event
                        if (clipboard.length() > 512) {
                            clipboard = clipboard.substring(0, 512);
                        }
                        this.queueEvent("paste", clipboard);
                    }

                    return true;
                }
            }
        }

        if (key >= 0 && this.terminateTimer < 0 && this.rebootTimer < 0 && this.shutdownTimer < 0) {
            // Queue the "key" event and add to the down set
            boolean repeat = this.keysDown.get(key);
            this.keysDown.set(key);
            IComputer computer = this.computer.get();
            if (computer != null) {
                computer.keyDown(key, repeat);
            }
        }

        return true;
    }

    @Override
    public boolean keyReleased(int key, int scancode, int modifiers) {
        // Queue the "key_up" event and remove from the down set
        if (key >= 0 && this.keysDown.get(key)) {
            this.keysDown.set(key, false);
            IComputer computer = this.computer.get();
            if (computer != null) {
                computer.keyUp(key);
            }
        }

        switch (key) {
        case GLFW.GLFW_KEY_T:
            this.terminateTimer = -1;
            break;
        case GLFW.GLFW_KEY_R:
            this.rebootTimer = -1;
            break;
        case GLFW.GLFW_KEY_S:
            this.shutdownTimer = -1;
            break;
        case GLFW.GLFW_KEY_LEFT_CONTROL:
        case GLFW.GLFW_KEY_RIGHT_CONTROL:
            this.terminateTimer = this.rebootTimer = this.shutdownTimer = -1;
            break;
        }

        return true;
    }

    @Override
    public boolean charTyped(char ch, int modifiers) {
        if (ch >= 32 && ch <= 126 || ch >= 160 && ch <= 255) // printable chars in byte range
        {
            // Queue the "char" event
            this.queueEvent("char", Character.toString(ch));
        }

        return true;
    }

    @Override
    public boolean changeFocus(boolean reverse) {
        if (this.focused) {
            // When blurring, we should make all keys go up
            for (int key = 0; key < this.keysDown.size(); key++) {
                if (this.keysDown.get(key)) {
                    this.queueEvent("key_up", key);
                }
            }
            this.keysDown.clear();

            // When blurring, we should make the last mouse button go up
            if (this.lastMouseButton > 0) {
                IComputer computer = this.computer.get();
                if (computer != null) {
                    computer.mouseUp(this.lastMouseButton + 1, this.lastMouseX + 1, this.lastMouseY + 1);
                }
                this.lastMouseButton = -1;
            }

            this.shutdownTimer = this.terminateTimer = this.rebootTimer = -1;
        }

        this.focused = !this.focused;
        return true;
    }

    private void queueEvent(String event, Object... args) {
        ClientComputer computer = this.computer.get();
        if (computer != null) {
            computer.queueEvent(event, args);
        }
    }

    public void update() {
        if (this.terminateTimer >= 0 && this.terminateTimer < TERMINATE_TIME && (this.terminateTimer += 0.05f) > TERMINATE_TIME) {
            this.queueEvent("terminate");
        }

        if (this.shutdownTimer >= 0 && this.shutdownTimer < TERMINATE_TIME && (this.shutdownTimer += 0.05f) > TERMINATE_TIME) {
            ClientComputer computer = this.computer.get();
            if (computer != null) {
                computer.shutdown();
            }
        }

        if (this.rebootTimer >= 0 && this.rebootTimer < TERMINATE_TIME && (this.rebootTimer += 0.05f) > TERMINATE_TIME) {
            ClientComputer computer = this.computer.get();
            if (computer != null) {
                computer.reboot();
            }
        }
    }

    private void queueEvent(String event) {
        ClientComputer computer = this.computer.get();
        if (computer != null) {
            computer.queueEvent(event);
        }
    }

    public void draw(int originX, int originY) {
        synchronized (this.computer) {
            // Draw the screen contents
            ClientComputer computer = this.computer.get();
            Terminal terminal = computer != null ? computer.getTerminal() : null;
            if (terminal != null) {
                // Draw the terminal
                boolean greyscale = !computer.isColour();
                Palette palette = terminal.getPalette();

                // Get the data from the terminal first
                // Unfortunately we have to keep the lock for the whole of drawing, so the text doesn't change under us.
                FixedWidthFontRenderer fontRenderer = FixedWidthFontRenderer.instance();
                boolean tblink = terminal.getCursorBlink() && FrameInfo.getGlobalCursorBlink();
                int tw = terminal.getWidth();
                int th = terminal.getHeight();
                int tx = terminal.getCursorX();
                int ty = terminal.getCursorY();

                // Draw margins
                TextBuffer emptyLine = new TextBuffer(' ', tw);
                if (this.topMargin > 0) {
                    fontRenderer.drawString(emptyLine,
                                            originX,
                                            originY - this.topMargin,
                                            terminal.getTextColourLine(0),
                                            terminal.getBackgroundColourLine(0), this.leftMargin, this.rightMargin,
                                            greyscale,
                                            palette);
                }

                if (this.bottomMargin > 0) {
                    fontRenderer.drawString(emptyLine,
                                            originX,
                                            originY + this.bottomMargin + (th - 1) * FixedWidthFontRenderer.FONT_HEIGHT,
                                            terminal.getTextColourLine(th - 1),
                                            terminal.getBackgroundColourLine(th - 1), this.leftMargin, this.rightMargin,
                                            greyscale,
                                            palette);
                }

                // Draw lines
                int y = originY;
                for (int line = 0; line < th; line++) {
                    TextBuffer text = terminal.getLine(line);
                    TextBuffer colour = terminal.getTextColourLine(line);
                    TextBuffer backgroundColour = terminal.getBackgroundColourLine(line);
                    fontRenderer.drawString(text, originX, y, colour, backgroundColour, this.leftMargin, this.rightMargin, greyscale, palette);
                    y += FixedWidthFontRenderer.FONT_HEIGHT;
                }

                if (tblink && tx >= 0 && ty >= 0 && tx < tw && ty < th) {
                    TextBuffer cursor = new TextBuffer('_', 1);
                    TextBuffer cursorColour = new TextBuffer("0123456789abcdef".charAt(terminal.getTextColour()), 1);

                    fontRenderer.drawString(cursor,
                                            originX + FixedWidthFontRenderer.FONT_WIDTH * tx,
                                            originY + FixedWidthFontRenderer.FONT_HEIGHT * ty,
                                            cursorColour,
                                            null,
                                            0,
                                            0,
                                            greyscale,
                                            palette);
                }
            } else {
                // Draw a black background
                Colour black = Colour.Black;
                GlStateManager.color4f(black.getR(), black.getG(), black.getB(), 1.0f);
                try {
                    int x = originX - this.leftMargin;
                    int y = originY - this.rightMargin;
                    int width = this.termWidth * FixedWidthFontRenderer.FONT_WIDTH + this.leftMargin + this.rightMargin;
                    int height = this.termHeight * FixedWidthFontRenderer.FONT_HEIGHT + this.topMargin + this.bottomMargin;

                    this.minecraft.getTextureManager()
                                  .bindTextureInner(BACKGROUND);

                    Tessellator tesslector = Tessellator.getInstance();
                    BufferBuilder buffer = tesslector.getBuffer();
                    buffer.begin(GL11.GL_QUADS, VertexFormats.POSITION_TEXTURE);
                    buffer.vertex(x, y + height, 0)
                          .texture(0 / 256.0f, height / 256.0f)
                          .next();
                    buffer.vertex(x + width, y + height, 0)
                          .texture(width / 256.0f, height / 256.f)
                          .next();
                    buffer.vertex(x + width, y, 0)
                          .texture(width / 256.0f, 0 / 256.0f)
                          .next();
                    buffer.vertex(x, y, 0)
                          .texture(0 / 256.0f, 0 / 256.0f)
                          .next();
                    tesslector.draw();
                } finally {
                    GlStateManager.color4f(1.0f, 1.0f, 1.0f, 1.0f);
                }
            }
        }
    }
}
