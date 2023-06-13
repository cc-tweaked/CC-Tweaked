// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.client;

import dan200.computercraft.util.Colour;
import net.minecraft.client.renderer.RenderEngine;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.settings.GameSettings;
import org.lwjgl.opengl.GL11;

/**
 * A replacement for {@link dan200.computer.client.FixedWidthFontRenderer} which uses CC's 1.76+ font.
 */
public class FixedWidthFontRenderer {
    public static final int FONT_HEIGHT = 9;
    public static final int FONT_WIDTH = 6;
    public static final float WIDTH = 256.0f;

    public static final float BACKGROUND_START = (WIDTH - 6.0f) / WIDTH;
    public static final float BACKGROUND_END = (WIDTH - 4.0f) / WIDTH;

    private final int fontTextureName;

    public FixedWidthFontRenderer(GameSettings gameSettings, String s, RenderEngine renderEngine) {
        this.fontTextureName = renderEngine.getTexture("/assets/cctweaked/textures/gui/term_font.png");
    }

    public int getStringWidth(String s) {
        return s == null ? 0 : s.length() * FONT_WIDTH;
    }

    @Deprecated
    public void drawString(String s, int x, int y, String colours, int marginSize) {
        this.drawString(s, x, y, colours, (float) marginSize, false);
    }

    @Deprecated
    public void drawString(String text, int x, int y, String colours, float marginSize, boolean forceBackground) {
        drawString(text, x, y, colours, marginSize, forceBackground, false);
    }

    public void drawStringIsColour(String text, int x, int y, String colours, int marginSize, boolean isColour) {
        drawString(text, x, y, colours, marginSize, false, !isColour);
    }

    public void drawString(String text, int x, int y, String colours, float marginSize, boolean forceBackground, boolean greyscale) {
        if (text == null) return;
        boolean hasBackgrounds = colours.length() > text.length();

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.fontTextureName);

        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawing(GL11.GL_QUADS);
        if (hasBackgrounds) {
            drawBackground(tessellator, x, y, colours, text.length(), marginSize, marginSize, FONT_HEIGHT, forceBackground, greyscale);
        }
        drawString(tessellator, x, y, text, colours, greyscale);
        tessellator.draw();
    }

    private static void drawBackground(
        Tessellator emitter, float x, float y, String backgroundColour, int start,
        float leftMarginSize, float rightMarginSize, float height, boolean forceBackground, boolean greyscale
    ) {
        int length = backgroundColour.length() - start;
        if (leftMarginSize > 0) {
            drawQuad(emitter, x - leftMarginSize, y, leftMarginSize, height, backgroundColour.charAt(start), forceBackground, greyscale);
        }

        if (rightMarginSize > 0) {
            drawQuad(emitter, x + length * FONT_WIDTH, y, rightMarginSize, height, backgroundColour.charAt(backgroundColour.length() - 1), forceBackground, greyscale);
        }

        // Batch together runs of identical background cells.
        int blockStart = 0;
        char blockColour = '\0';
        for (int i = 0; i < length; i++) {
            char colourIndex = backgroundColour.charAt(i + start);
            if (colourIndex == blockColour) continue;

            if (blockColour != '\0') {
                drawQuad(emitter, x + blockStart * FONT_WIDTH, y, FONT_WIDTH * (i - blockStart), height, blockColour, forceBackground, greyscale);
            }

            blockColour = colourIndex;
            blockStart = i;
        }

        if (blockColour != '\0') {
            drawQuad(emitter, x + blockStart * FONT_WIDTH, y, FONT_WIDTH * (length - blockStart), height, blockColour, forceBackground, greyscale);
        }
    }

    private static void drawQuad(Tessellator emitter, float x, float y, float width, float height, char colourIndex, boolean forceBackground, boolean greyscale) {
        Colour colour = Colour.fromInt(getColour(colourIndex, Colour.BLACK));
        // Skip drawing black quads. Mostly important for printouts.
        if (colour == Colour.BLACK && !forceBackground) return;

        quad(
            emitter, x, y, x + width, y + height, 0, getHex(colour, greyscale),
            BACKGROUND_START, BACKGROUND_START, BACKGROUND_END, BACKGROUND_END
        );
    }


    public static void drawString(Tessellator emitter, float x, float y, String text, String textColour, boolean greyscale) {
        for (int i = 0; i < text.length(); i++) {
            int colour = getHex(Colour.fromInt(getColour(textColour.charAt(i), Colour.WHITE)), greyscale);

            int index = text.charAt(i);
            if (index > 255) index = '?';
            drawChar(emitter, x + i * FONT_WIDTH, y, index, colour);
        }
    }

    private static void drawChar(Tessellator emitter, float x, float y, int index, int colour) {
        // Short circuit to avoid the common case - the texture should be blank here after all.
        if (index == '\0' || index == ' ') return;

        int column = index % 16;
        int row = index / 16;

        int xStart = 1 + column * (FONT_WIDTH + 2);
        int yStart = 1 + row * (FONT_HEIGHT + 2);

        quad(
            emitter, x, y, x + FONT_WIDTH, y + FONT_HEIGHT, 0, colour,
            xStart / WIDTH, yStart / WIDTH, (xStart + FONT_WIDTH) / WIDTH, (yStart + FONT_HEIGHT) / WIDTH
        );
    }

    private static void quad(Tessellator emitter, float x1, float y1, float x2, float y2, float z, int colour, float u1, float v1, float u2, float v2) {
        emitter.setColorOpaque_I(colour);
        emitter.addVertexWithUV(x1, y1, z, u1, v1);
        emitter.addVertexWithUV(x1, y2, z, u1, v2);
        emitter.addVertexWithUV(x2, y2, z, u2, v2);
        emitter.addVertexWithUV(x2, y1, z, u2, v1);
    }

    private static int getHex(Colour colour, boolean greyscale) {
        if (greyscale) {
            int single = (int) ((colour.getR() + colour.getG() + colour.getB()) / 3 * 255) & 0xFF;
            return (single << 16) | (single << 8) | single;
        } else {
            return colour.getHex();
        }
    }

    private static int getColour(char c, Colour def) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'a' && c <= 'f') return c - 'a' + 10;
        if (c >= 'A' && c <= 'F') return c - 'A' + 10;
        return 15 - def.ordinal();
    }
}
