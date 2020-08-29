/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.gui;

import java.util.Arrays;

import com.mojang.blaze3d.platform.GlStateManager;
import dan200.computercraft.core.terminal.TextBuffer;
import dan200.computercraft.shared.util.Palette;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;

public final class FixedWidthFontRenderer {
    public static final Identifier BACKGROUND = new Identifier("computercraft", "textures/gui/term_background.png");
    public static final int FONT_HEIGHT = 9;
    public static final int FONT_WIDTH = 6;
    private static final Identifier FONT = new Identifier("computercraft", "textures/gui/term_font.png");
    private static FixedWidthFontRenderer instance;
    private final TextureManager m_textureManager;

    private FixedWidthFontRenderer() {
        this.m_textureManager = MinecraftClient.getInstance()
                                               .getTextureManager();
    }

    public static FixedWidthFontRenderer instance() {
        if (instance != null) {
            return instance;
        }
        return instance = new FixedWidthFontRenderer();
    }

    public void drawString(TextBuffer s, int x, int y, TextBuffer textColour, TextBuffer backgroundColour, double leftMarginSize, double rightMarginSize,
                           boolean greyScale, Palette p) {
        // Draw background
        if (backgroundColour != null) {
            // Bind the background texture
            this.m_textureManager.bindTextureInner(BACKGROUND);

            // Draw the quads
            this.drawStringBackgroundPart(x, y, backgroundColour, leftMarginSize, rightMarginSize, greyScale, p);
        }

        // Draw text
        if (s != null && textColour != null) {
            // Bind the font texture
            this.bindFont();

            // Draw the quads
            this.drawStringTextPart(x, y, s, textColour, greyScale, p);
        }
    }

    public void drawStringBackgroundPart(int x, int y, TextBuffer backgroundColour, double leftMarginSize, double rightMarginSize, boolean greyScale,
                                         Palette p) {
        // Draw the quads
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder renderer = tessellator.getBuffer();
        renderer.begin(GL11.GL_TRIANGLES, VertexFormats.POSITION_COLOR);
        if (leftMarginSize > 0.0) {
            int colour1 = "0123456789abcdef".indexOf(backgroundColour.charAt(0));
            if (colour1 < 0 || (greyScale && !this.isGreyScale(colour1))) {
                colour1 = 15;
            }
            this.drawQuad(renderer, x - leftMarginSize, y, colour1, leftMarginSize, p, greyScale);
        }
        if (rightMarginSize > 0.0) {
            int colour2 = "0123456789abcdef".indexOf(backgroundColour.charAt(backgroundColour.length() - 1));
            if (colour2 < 0 || (greyScale && !this.isGreyScale(colour2))) {
                colour2 = 15;
            }
            this.drawQuad(renderer, x + backgroundColour.length() * FONT_WIDTH, y, colour2, rightMarginSize, p, greyScale);
        }
        for (int i = 0; i < backgroundColour.length(); i++) {
            int colour = "0123456789abcdef".indexOf(backgroundColour.charAt(i));
            if (colour < 0 || (greyScale && !this.isGreyScale(colour))) {
                colour = 15;
            }
            this.drawQuad(renderer, x + i * FONT_WIDTH, y, colour, FONT_WIDTH, p, greyScale);
        }
        GlStateManager.disableTexture();
        tessellator.draw();
        GlStateManager.enableTexture();
    }

    public void bindFont() {
        this.m_textureManager.bindTextureInner(FONT);
        GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
    }

    public void drawStringTextPart(int x, int y, TextBuffer s, TextBuffer textColour, boolean greyScale, Palette p) {
        // Draw the quads
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder renderer = tessellator.getBuffer();
        renderer.begin(GL11.GL_TRIANGLES, VertexFormats.POSITION_TEXTURE_COLOR);
        for (int i = 0; i < s.length(); i++) {
            // Switch colour
            int colour = "0123456789abcdef".indexOf(textColour.charAt(i));
            if (colour < 0 || (greyScale && !this.isGreyScale(colour))) {
                colour = 0;
            }

            // Draw char
            int index = s.charAt(i);
            if (index < 0 || index > 255) {
                index = '?';
            }
            this.drawChar(renderer, x + i * FONT_WIDTH, y, index, colour, p, greyScale);
        }
        tessellator.draw();
    }

    private boolean isGreyScale(int colour) {
        return colour == 0 || colour == 15 || colour == 7 || colour == 8;
    }

    private void drawQuad(BufferBuilder renderer, double x, double y, int color, double width, Palette p, boolean greyscale) {
        double[] colour = p.getColour(15 - color);
        if (greyscale) {
            greyscaleify(colour);
        }
        float r = (float) colour[0];
        float g = (float) colour[1];
        float b = (float) colour[2];

        renderer.vertex(x, y, 0.0)
                .color(r, g, b, 1.0f)
                .next();
        renderer.vertex(x, y + FONT_HEIGHT, 0.0)
                .color(r, g, b, 1.0f)
                .next();
        renderer.vertex(x + width, y, 0.0)
                .color(r, g, b, 1.0f)
                .next();
        renderer.vertex(x + width, y, 0.0)
                .color(r, g, b, 1.0f)
                .next();
        renderer.vertex(x, y + FONT_HEIGHT, 0.0)
                .color(r, g, b, 1.0f)
                .next();
        renderer.vertex(x + width, y + FONT_HEIGHT, 0.0)
                .color(r, g, b, 1.0f)
                .next();
    }

    private void drawChar(BufferBuilder renderer, double x, double y, int index, int color, Palette p, boolean greyscale) {
        int column = index % 16;
        int row = index / 16;

        double[] colour = p.getColour(15 - color);
        if (greyscale) {
            greyscaleify(colour);
        }
        float r = (float) colour[0];
        float g = (float) colour[1];
        float b = (float) colour[2];

        int xStart = 1 + column * (FONT_WIDTH + 2);
        int yStart = 1 + row * (FONT_HEIGHT + 2);

        renderer.vertex(x, y, 0.0)
                .texture(xStart / 256.0, yStart / 256.0)
                .color(r, g, b, 1.0f)
                .next();
        renderer.vertex(x, y + FONT_HEIGHT, 0.0)
                .texture(xStart / 256.0, (yStart + FONT_HEIGHT) / 256.0)
                .color(r, g, b, 1.0f)
                .next();
        renderer.vertex(x + FONT_WIDTH, y, 0.0)
                .texture((xStart + FONT_WIDTH) / 256.0, yStart / 256.0)
                .color(r, g, b, 1.0f)
                .next();
        renderer.vertex(x + FONT_WIDTH, y, 0.0)
                .texture((xStart + FONT_WIDTH) / 256.0, yStart / 256.0)
                .color(r, g, b, 1.0f)
                .next();
        renderer.vertex(x, y + FONT_HEIGHT, 0.0)
                .texture(xStart / 256.0, (yStart + FONT_HEIGHT) / 256.0)
                .color(r, g, b, 1.0f)
                .next();
        renderer.vertex(x + FONT_WIDTH, y + FONT_HEIGHT, 0.0)
                .texture((xStart + FONT_WIDTH) / 256.0, (yStart + FONT_HEIGHT) / 256.0)
                .color(r, g, b, 1.0f)
                .next();
    }

    private static void greyscaleify(double[] rgb) {
        Arrays.fill(rgb, (rgb[0] + rgb[1] + rgb[2]) / 3.0f);
    }

    public int getStringWidth(String s) {
        if (s == null) {
            return 0;
        }
        return s.length() * FONT_WIDTH;
    }
}
