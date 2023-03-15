// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.client.render.text;

import com.mojang.blaze3d.platform.MemoryTracker;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dan200.computercraft.client.render.RenderTypes;
import dan200.computercraft.core.terminal.Palette;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.core.terminal.TextBuffer;
import dan200.computercraft.core.util.Colour;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

import static dan200.computercraft.client.render.text.FixedWidthFontRenderer.*;
import static org.lwjgl.system.MemoryUtil.*;

/**
 * An optimised copy of {@link FixedWidthFontRenderer} emitter emits directly to a {@link QuadEmitter} rather than
 * emitting to {@link VertexConsumer}. This allows us to emit vertices very quickly, when using the VBO renderer.
 * <p>
 * There are some limitations here:
 * <ul>
 *   <li>No transformation matrix (not needed for VBOs).</li>
 *   <li>Only works with {@link DefaultVertexFormat#POSITION_COLOR_TEX_LIGHTMAP}.</li>
 *   <li>The buffer <strong>MUST</strong> be allocated with {@link MemoryTracker}, and not through any other means.</li>
 * </ul>
 * <p>
 * Note this is almost an exact copy of {@link FixedWidthFontRenderer}. While the code duplication is unfortunate,
 * it is measurably faster than introducing polymorphism into {@link FixedWidthFontRenderer}.
 *
 * <strong>IMPORTANT: </strong> When making changes to this class, please check if you need to make the same changes to
 * {@link FixedWidthFontRenderer}.
 */
public final class DirectFixedWidthFontRenderer {
    private DirectFixedWidthFontRenderer() {
    }

    private static void drawChar(QuadEmitter emitter, float x, float y, int index, byte[] colour) {
        // Short circuit to avoid the common case - the texture should be blank here after all.
        if (index == '\0' || index == ' ') return;

        var column = index % 16;
        var row = index / 16;

        var xStart = 1 + column * (FONT_WIDTH + 2);
        var yStart = 1 + row * (FONT_HEIGHT + 2);

        quad(
            emitter, x, y, x + FONT_WIDTH, y + FONT_HEIGHT, 0, colour,
            xStart / WIDTH, yStart / WIDTH, (xStart + FONT_WIDTH) / WIDTH, (yStart + FONT_HEIGHT) / WIDTH
        );
    }

    private static void drawQuad(QuadEmitter emitter, float x, float y, float width, float height, Palette palette, char colourIndex) {
        var colour = palette.getRenderColours(getColour(colourIndex, Colour.BLACK));
        quad(emitter, x, y, x + width, y + height, 0f, colour, BACKGROUND_START, BACKGROUND_START, BACKGROUND_END, BACKGROUND_END);
    }

    private static void drawBackground(
        QuadEmitter emitter, float x, float y, TextBuffer backgroundColour, Palette palette,
        float leftMarginSize, float rightMarginSize, float height
    ) {
        if (leftMarginSize > 0) {
            drawQuad(emitter, x - leftMarginSize, y, leftMarginSize, height, palette, backgroundColour.charAt(0));
        }

        if (rightMarginSize > 0) {
            drawQuad(emitter, x + backgroundColour.length() * FONT_WIDTH, y, rightMarginSize, height, palette, backgroundColour.charAt(backgroundColour.length() - 1));
        }

        // Batch together runs of identical background cells.
        var blockStart = 0;
        var blockColour = '\0';
        for (var i = 0; i < backgroundColour.length(); i++) {
            var colourIndex = backgroundColour.charAt(i);
            if (colourIndex == blockColour) continue;

            if (blockColour != '\0') {
                drawQuad(emitter, x + blockStart * FONT_WIDTH, y, FONT_WIDTH * (i - blockStart), height, palette, blockColour);
            }

            blockColour = colourIndex;
            blockStart = i;
        }

        if (blockColour != '\0') {
            drawQuad(emitter, x + blockStart * FONT_WIDTH, y, FONT_WIDTH * (backgroundColour.length() - blockStart), height, palette, blockColour);
        }
    }

    public static void drawString(QuadEmitter emitter, float x, float y, TextBuffer text, TextBuffer textColour, Palette palette) {
        for (var i = 0; i < text.length(); i++) {
            var colour = palette.getRenderColours(getColour(textColour.charAt(i), Colour.BLACK));

            int index = text.charAt(i);
            if (index > 255) index = '?';
            drawChar(emitter, x + i * FONT_WIDTH, y, index, colour);
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
                palette
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
            leftMarginSize, rightMarginSize, topMarginSize
        );

        drawBackground(
            emitter, x, y + height * FONT_HEIGHT, terminal.getBackgroundColourLine(height - 1), palette,
            leftMarginSize, rightMarginSize, bottomMarginSize
        );

        // The main text
        for (var i = 0; i < height; i++) {
            var rowY = y + FONT_HEIGHT * i;
            drawBackground(
                emitter, x, rowY, terminal.getBackgroundColourLine(i), palette,
                leftMarginSize, rightMarginSize, FONT_HEIGHT
            );
        }
    }

    public static void drawCursor(QuadEmitter emitter, float x, float y, Terminal terminal) {
        if (isCursorVisible(terminal)) {
            var colour = terminal.getPalette().getRenderColours(15 - terminal.getTextColour());
            drawChar(emitter, x + terminal.getCursorX() * FONT_WIDTH, y + terminal.getCursorY() * FONT_HEIGHT, '_', colour);
        }
    }

    public static int getVertexCount(Terminal terminal) {
        return (terminal.getHeight() + 2) * (terminal.getWidth() + 2) * 2;
    }

    private static void quad(QuadEmitter buffer, float x1, float y1, float x2, float y2, float z, byte[] rgba, float u1, float v1, float u2, float v2) {
        buffer.quad(x1, y1, x2, y2, z, rgba, u1, v1, u2, v2);
    }

    public interface QuadEmitter {
        VertexFormat format();

        ByteBuffer buffer();

        void quad(float x1, float y1, float x2, float y2, float z, byte[] rgba, float u1, float v1, float u2, float v2);
    }

    public record ByteBufferEmitter(ByteBuffer buffer) implements QuadEmitter {
        @Override
        public VertexFormat format() {
            return RenderTypes.TERMINAL.format();
        }

        @Override
        public void quad(float x1, float y1, float x2, float y2, float z, byte[] rgba, float u1, float v1, float u2, float v2) {
            DirectFixedWidthFontRenderer.quad(buffer, x1, y1, x2, y2, z, rgba, u1, v1, u2, v2);
        }
    }

    static void quad(ByteBuffer buffer, float x1, float y1, float x2, float y2, float z, byte[] rgba, float u1, float v1, float u2, float v2) {
        // Emit a single quad to our buffer. This uses Unsafe (well, LWJGL's MemoryUtil) to directly blit bytes to the
        // underlying buffer. This allows us to have a single bounds check up-front, rather than one for every write.
        // This provides significant performance gains, at the cost of well, using Unsafe.
        // Each vertex is 28 bytes, giving 112 bytes in total. Vertices are of the form (xyz:FFF)(rgba:BBBB)(uv1:FF)(uv2:SS),
        // which matches the POSITION_COLOR_TEX_LIGHTMAP vertex format.

        var position = buffer.position();
        var addr = MemoryUtil.memAddress(buffer);

        // We're doing terrible unsafe hacks below, so let's be really sure that what we're doing is reasonable.
        if (position < 0 || 112 > buffer.limit() - position) throw new IndexOutOfBoundsException();
        // Require the pointer to be aligned to a 32-bit boundary.
        if ((addr & 3) != 0) throw new IllegalStateException("Memory is not aligned");
        // Also assert the length of the array. This appears to help elide bounds checks on the array in some circumstances.
        if (rgba.length != 4) throw new IllegalStateException();

        memPutFloat(addr + 0, x1);
        memPutFloat(addr + 4, y1);
        memPutFloat(addr + 8, z);
        memPutByte(addr + 12, rgba[0]);
        memPutByte(addr + 13, rgba[1]);
        memPutByte(addr + 14, rgba[2]);
        memPutByte(addr + 15, (byte) 255);
        memPutFloat(addr + 16, u1);
        memPutFloat(addr + 20, v1);
        memPutShort(addr + 24, (short) 0xF0);
        memPutShort(addr + 26, (short) 0xF0);

        memPutFloat(addr + 28, x1);
        memPutFloat(addr + 32, y2);
        memPutFloat(addr + 36, z);
        memPutByte(addr + 40, rgba[0]);
        memPutByte(addr + 41, rgba[1]);
        memPutByte(addr + 42, rgba[2]);
        memPutByte(addr + 43, (byte) 255);
        memPutFloat(addr + 44, u1);
        memPutFloat(addr + 48, v2);
        memPutShort(addr + 52, (short) 0xF0);
        memPutShort(addr + 54, (short) 0xF0);

        memPutFloat(addr + 56, x2);
        memPutFloat(addr + 60, y2);
        memPutFloat(addr + 64, z);
        memPutByte(addr + 68, rgba[0]);
        memPutByte(addr + 69, rgba[1]);
        memPutByte(addr + 70, rgba[2]);
        memPutByte(addr + 71, (byte) 255);
        memPutFloat(addr + 72, u2);
        memPutFloat(addr + 76, v2);
        memPutShort(addr + 80, (short) 0xF0);
        memPutShort(addr + 82, (short) 0xF0);

        memPutFloat(addr + 84, x2);
        memPutFloat(addr + 88, y1);
        memPutFloat(addr + 92, z);
        memPutByte(addr + 96, rgba[0]);
        memPutByte(addr + 97, rgba[1]);
        memPutByte(addr + 98, rgba[2]);
        memPutByte(addr + 99, (byte) 255);
        memPutFloat(addr + 100, u2);
        memPutFloat(addr + 104, v1);
        memPutShort(addr + 108, (short) 0xF0);
        memPutShort(addr + 110, (short) 0xF0);

        // Finally increment the position.
        buffer.position(position + 112);

        // Well done for getting to the end of this method. I recommend you take a break and go look at cute puppies.
    }
}
