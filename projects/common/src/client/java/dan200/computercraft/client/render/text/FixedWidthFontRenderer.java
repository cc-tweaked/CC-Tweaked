// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.client.render.text;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dan200.computercraft.client.FrameInfo;
import dan200.computercraft.core.terminal.Palette;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.core.terminal.TextBuffer;
import dan200.computercraft.core.util.Colour;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static dan200.computercraft.client.render.RenderTypes.FULL_BRIGHT_LIGHTMAP;

/**
 * Handles rendering fixed width text and computer terminals.
 * <p>
 * This class has several modes of usage:
 * <ul>
 * <li>{@link #drawString}: Drawing basic text without a terminal (such as for printouts). Unlike the other methods,
 * this accepts a lightmap coordinate as, unlike terminals, printed pages render fullbright.</li>
 * <li>{@link #drawTerminal}: Draw a terminal with a cursor. This is used by the various computer GUIs to render the
 * whole term.</li>
 * </ul>
 *
 * <strong>IMPORTANT: </strong> When making changes to this class, please check if you need to make the same changes to
 * {@link DirectFixedWidthFontRenderer}.
 */
public final class FixedWidthFontRenderer {
    public static final ResourceLocation FONT = new ResourceLocation("computercraft", "textures/gui/term_font.png");

    public static final int FONT_HEIGHT = 9;
    public static final int FONT_WIDTH = 6;
    static final float WIDTH = 256.0f;

    static final float BACKGROUND_START = (WIDTH - 6.0f) / WIDTH;
    static final float BACKGROUND_END = (WIDTH - 4.0f) / WIDTH;

    private static final int BLACK = FastColor.ARGB32.color(255, byteColour(Colour.BLACK.getR()), byteColour(Colour.BLACK.getR()), byteColour(Colour.BLACK.getR()));
    private static final float Z_OFFSET = 1e-3f;

    private FixedWidthFontRenderer() {
    }

    private static byte byteColour(float c) {
        return (byte) (int) (c * 255);
    }

    public static float toGreyscale(double[] rgb) {
        return (float) ((rgb[0] + rgb[1] + rgb[2]) / 3);
    }

    public static int getColour(char c, Colour def) {
        return 15 - Terminal.getColour(c, def);
    }

    private static void drawChar(QuadEmitter emitter, float x, float y, int index, int colour, int light) {
        // Short circuit to avoid the common case - the texture should be blank here after all.
        if (index == '\0' || index == ' ') return;

        var column = index % 16;
        var row = index / 16;

        var xStart = 1 + column * (FONT_WIDTH + 2);
        var yStart = 1 + row * (FONT_HEIGHT + 2);

        quad(
            emitter, x, y, x + FONT_WIDTH, y + FONT_HEIGHT, 0, colour,
            xStart / WIDTH, yStart / WIDTH, (xStart + FONT_WIDTH) / WIDTH, (yStart + FONT_HEIGHT) / WIDTH, light
        );
    }

    public static void drawQuad(QuadEmitter emitter, float x, float y, float z, float width, float height, int colour, int light) {
        quad(emitter, x, y, x + width, y + height, z, colour, BACKGROUND_START, BACKGROUND_START, BACKGROUND_END, BACKGROUND_END, light);
    }

    private static void drawQuad(QuadEmitter emitter, float x, float y, float width, float height, Palette palette, char colourIndex, int light) {
        var colour = palette.getRenderColours(getColour(colourIndex, Colour.BLACK));
        drawQuad(emitter, x, y, 0, width, height, colour, light);
    }

    private static void drawBackground(
        QuadEmitter emitter, float x, float y, TextBuffer backgroundColour, Palette palette,
        float leftMarginSize, float rightMarginSize, float height, int light
    ) {
        if (leftMarginSize > 0) {
            drawQuad(emitter, x - leftMarginSize, y, leftMarginSize, height, palette, backgroundColour.charAt(0), light);
        }

        if (rightMarginSize > 0) {
            drawQuad(emitter, x + backgroundColour.length() * FONT_WIDTH, y, rightMarginSize, height, palette, backgroundColour.charAt(backgroundColour.length() - 1), light);
        }

        // Batch together runs of identical background cells.
        var blockStart = 0;
        var blockColour = '\0';
        for (var i = 0; i < backgroundColour.length(); i++) {
            var colourIndex = backgroundColour.charAt(i);
            if (colourIndex == blockColour) continue;

            if (blockColour != '\0') {
                drawQuad(emitter, x + blockStart * FONT_WIDTH, y, FONT_WIDTH * (i - blockStart), height, palette, blockColour, light);
            }

            blockColour = colourIndex;
            blockStart = i;
        }

        if (blockColour != '\0') {
            drawQuad(emitter, x + blockStart * FONT_WIDTH, y, FONT_WIDTH * (backgroundColour.length() - blockStart), height, palette, blockColour, light);
        }
    }

    public static void drawString(QuadEmitter emitter, float x, float y, TextBuffer text, TextBuffer textColour, Palette palette, int light) {
        for (var i = 0; i < text.length(); i++) {
            var colour = palette.getRenderColours(getColour(textColour.charAt(i), Colour.BLACK));

            int index = text.charAt(i);
            if (index > 255) index = '?';
            drawChar(emitter, x + i * FONT_WIDTH, y, index, colour, light);
        }

    }

    public static void drawTerminalForeground(QuadEmitter emitter, float x, float y, Terminal terminal) {
        var palette = terminal.getPalette();
        var height = terminal.getHeight();

        // The main text
        for (var i = 0; i < height; i++) {
            var rowY = y + FONT_HEIGHT * i;
            drawString(
                emitter, x, rowY, terminal.getLine(i), terminal.getTextColourLine(i),
                palette, FULL_BRIGHT_LIGHTMAP
            );
        }
    }

    public static void drawTerminalBackground(
        QuadEmitter emitter, float x, float y, Terminal terminal,
        float topMarginSize, float bottomMarginSize, float leftMarginSize, float rightMarginSize
    ) {
        var palette = terminal.getPalette();
        var height = terminal.getHeight();

        // Top and bottom margins
        drawBackground(
            emitter, x, y - topMarginSize, terminal.getBackgroundColourLine(0), palette,
            leftMarginSize, rightMarginSize, topMarginSize, FULL_BRIGHT_LIGHTMAP
        );

        drawBackground(
            emitter, x, y + height * FONT_HEIGHT, terminal.getBackgroundColourLine(height - 1), palette,
            leftMarginSize, rightMarginSize, bottomMarginSize, FULL_BRIGHT_LIGHTMAP
        );

        // The main text
        for (var i = 0; i < height; i++) {
            var rowY = y + FONT_HEIGHT * i;
            drawBackground(
                emitter, x, rowY, terminal.getBackgroundColourLine(i), palette,
                leftMarginSize, rightMarginSize, FONT_HEIGHT, FULL_BRIGHT_LIGHTMAP
            );
        }
    }

    public static boolean isCursorVisible(Terminal terminal) {
        if (!terminal.getCursorBlink()) return false;

        var cursorX = terminal.getCursorX();
        var cursorY = terminal.getCursorY();
        return cursorX >= 0 && cursorX < terminal.getWidth() && cursorY >= 0 && cursorY < terminal.getHeight();
    }

    public static void drawCursor(QuadEmitter emitter, float x, float y, Terminal terminal) {
        if (isCursorVisible(terminal) && FrameInfo.getGlobalCursorBlink()) {
            var colour = terminal.getPalette().getRenderColours(15 - terminal.getTextColour());
            drawChar(emitter, x + terminal.getCursorX() * FONT_WIDTH, y + terminal.getCursorY() * FONT_HEIGHT, '_', colour, FULL_BRIGHT_LIGHTMAP);
        }
    }

    public static void drawTerminal(
        QuadEmitter emitter, float x, float y, Terminal terminal,
        float topMarginSize, float bottomMarginSize, float leftMarginSize, float rightMarginSize
    ) {
        drawTerminalBackground(
            emitter, x, y, terminal,
            topMarginSize, bottomMarginSize, leftMarginSize, rightMarginSize
        );

        // Render the foreground with a slight offset. By calling .translate() on the matrix itself, we're translating
        // in screen space, rather than in model/view space.
        // It's definitely not perfect, but better than z fighting!
        var transformBackup = new Matrix4f(emitter.poseMatrix());
        emitter.poseMatrix().translate(new Vector3f(0, 0, Z_OFFSET));

        drawTerminalForeground(emitter, x, y, terminal);
        drawCursor(emitter, x, y, terminal);

        emitter.poseMatrix().set(transformBackup);
    }

    public static void drawEmptyTerminal(QuadEmitter emitter, float x, float y, float width, float height) {
        drawQuad(emitter, x, y, 0, width, height, BLACK, FULL_BRIGHT_LIGHTMAP);
    }

    public record QuadEmitter(Matrix4f poseMatrix, VertexConsumer consumer) {
    }

    public static QuadEmitter toVertexConsumer(PoseStack transform, VertexConsumer consumer) {
        return new QuadEmitter(transform.last().pose(), consumer);
    }

    private static void quad(QuadEmitter c, float x1, float y1, float x2, float y2, float z, int colour, float u1, float v1, float u2, float v2, int light) {
        var poseMatrix = c.poseMatrix();
        var consumer = c.consumer();
        int r = FastColor.ARGB32.red(colour), g = FastColor.ARGB32.green(colour), b = FastColor.ARGB32.blue(colour), a = FastColor.ARGB32.alpha(colour);

        consumer.vertex(poseMatrix, x1, y1, z).color(r, g, b, a).uv(u1, v1).uv2(light).endVertex();
        consumer.vertex(poseMatrix, x1, y2, z).color(r, g, b, a).uv(u1, v2).uv2(light).endVertex();
        consumer.vertex(poseMatrix, x2, y2, z).color(r, g, b, a).uv(u2, v2).uv2(light).endVertex();
        consumer.vertex(poseMatrix, x2, y1, z).color(r, g, b, a).uv(u2, v1).uv2(light).endVertex();
    }
}
