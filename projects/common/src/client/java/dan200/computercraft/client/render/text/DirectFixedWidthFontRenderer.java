// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.client.render.text;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dan200.computercraft.core.terminal.Palette;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.core.terminal.TextBuffer;
import dan200.computercraft.core.util.Colour;
import net.minecraft.util.ARGB;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

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
 *   <li>The buffer <strong>MUST</strong> be allocated with {@link MemoryUtil}, and not through any other means.</li>
 * </ul>
 * <p>
 * Note this is almost an exact copy of {@link FixedWidthFontRenderer}. While the code duplication is unfortunate,
 * it is measurably faster than introducing polymorphism into {@link FixedWidthFontRenderer}.
 * <p>
 * <strong>IMPORTANT: </strong> When making changes to this class, please check if you need to make the same changes to
 * {@link FixedWidthFontRenderer}.
 */
public final class DirectFixedWidthFontRenderer {
    private static final boolean IS_LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;

    private DirectFixedWidthFontRenderer() {
    }

    private static void drawChar(QuadEmitter emitter, float x, float y, int index, int colour) {
        // Short circuit to avoid the common case - the texture should be blank here after all.
        if (index == '\0' || index == ' ') return;

        var column = index % 16;
        var row = index / 16;

        var xStart = 1 + column * (FONT_WIDTH + 2);
        var yStart = 1 + row * (FONT_HEIGHT + 2);

        quad(
            emitter, x, y, x + FONT_WIDTH, y + FONT_HEIGHT, colour,
            xStart / WIDTH, yStart / WIDTH, (xStart + FONT_WIDTH) / WIDTH, (yStart + FONT_HEIGHT) / WIDTH
        );
    }

    private static void drawQuad(QuadEmitter emitter, float x, float y, float width, float height, Palette palette, char colourIndex) {
        var colour = palette.getRenderColours(getColour(colourIndex, Colour.BLACK));
        quad(emitter, x, y, x + width, y + height, colour, BACKGROUND_START, BACKGROUND_START, BACKGROUND_END, BACKGROUND_END);
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

    private static void quad(QuadEmitter buffer, float x1, float y1, float x2, float y2, int colour, float u1, float v1, float u2, float v2) {
        var colourAbgr = ARGB.toABGR(colour);
        // Pack colour so it is equivalent to abgr:BBBB. This matches the logic in BufferBuilder.
        var nativeColour = IS_LITTLE_ENDIAN ? colourAbgr : Integer.reverseBytes(colourAbgr);

        buffer.vertexCount += 4;
        buffer.quad(x1, y1, x2, y2, 0, nativeColour, u1, v1, u2, v2);
    }

    /**
     * An abstraction for emitting quads to a buffer.
     */
    public abstract static class QuadEmitter {
        private int vertexCount;

        public abstract ByteBuffer byteBuffer();

        public abstract VertexFormat format();

        /**
         * Emit a quad to this buffer.
         *
         * @param x1           The first X coordinate of the quad.
         * @param y1           The first Y coordinate of the quad.
         * @param x2           The second X coordinate of the quad.
         * @param y2           The second Y coordinate of the quad.
         * @param z            The z coordinate of the quad.
         * @param nativeColour The colour of the quad, in ABGR or RGBA, according to endianness.
         * @param u1           The first U coordinate of the quad.
         * @param v1           The first V coordinate of the quad.
         * @param u2           The second U coordinate of the quad.
         * @param v2           The second V coordinate of the quad.
         */
        protected abstract void quad(float x1, float y1, float x2, float y2, float z, int nativeColour, float u1, float v1, float u2, float v2);

        public int vertexCount() {
            return vertexCount;
        }
    }

    public static final class ByteBufferEmitter extends QuadEmitter {
        private final ByteBuffer buffer;

        public ByteBufferEmitter(ByteBuffer buffer) {
            this.buffer = buffer;
        }

        @Override
        public ByteBuffer byteBuffer() {
            return buffer;
        }

        @Override
        public VertexFormat format() {
            return TERMINAL_TEXT.format();
        }

        @Override
        public void quad(float x1, float y1, float x2, float y2, float z, int nativeColour, float u1, float v1, float u2, float v2) {
            DirectFixedWidthFontRenderer.quad(buffer, x1, y1, x2, y2, z, nativeColour, u1, v1, u2, v2);
        }
    }

    private static void quad(ByteBuffer buffer, float x1, float y1, float x2, float y2, float z, int nativeColour, float u1, float v1, float u2, float v2) {
        // Emit a single quad to our buffer. This uses Unsafe (well, LWJGL's MemoryUtil) to directly blit bytes to the
        // underlying buffer. This allows us to have a single bounds check up-front, rather than one for every write.
        // This provides significant performance gains, at the cost of well, using Unsafe.
        // Each vertex is 28 bytes, giving 112 bytes in total. Vertices are of the form (xyz:FFF)(uv1:FF)(uv2:SS)(abgr:BBBB),
        // which matches the POSITION_COLOR_TEX_LIGHTMAP vertex format.
        var position = buffer.position();
        var addr = MemoryUtil.memAddress(buffer);

        // We're doing terrible unsafe hacks below, so let's be really sure that what we're doing is reasonable.
        // Require the pointer to be aligned to a 32-bit boundary.
        if ((addr & 3) != 0) throw new IllegalStateException("Memory is not aligned");
        if (TERMINAL_TEXT.format().getVertexSize() != 28) {
            throw new IllegalStateException("Incorrect vertex size");
        }

        memPutFloat(addr + 0, x1);
        memPutFloat(addr + 4, y1);
        memPutFloat(addr + 8, z);
        memPutFloat(addr + 12, u1);
        memPutFloat(addr + 16, v1);
        memPutShort(addr + 20, (short) 0xF0);
        memPutShort(addr + 22, (short) 0xF0);
        memPutInt(addr + 24, nativeColour);

        memPutFloat(addr + 28, x1);
        memPutFloat(addr + 32, y2);
        memPutFloat(addr + 36, z);
        memPutFloat(addr + 40, u1);
        memPutFloat(addr + 44, v2);
        memPutShort(addr + 48, (short) 0xF0);
        memPutShort(addr + 50, (short) 0xF0);
        memPutInt(addr + 52, nativeColour);

        memPutFloat(addr + 56, x2);
        memPutFloat(addr + 60, y2);
        memPutFloat(addr + 64, z);
        memPutFloat(addr + 68, u2);
        memPutFloat(addr + 72, v2);
        memPutShort(addr + 76, (short) 0xF0);
        memPutShort(addr + 78, (short) 0xF0);
        memPutInt(addr + 80, nativeColour);

        memPutFloat(addr + 84, x2);
        memPutFloat(addr + 88, y1);
        memPutFloat(addr + 92, z);
        memPutFloat(addr + 96, u2);
        memPutFloat(addr + 100, v1);
        memPutShort(addr + 104, (short) 0xF0);
        memPutShort(addr + 106, (short) 0xF0);
        memPutInt(addr + 108, nativeColour);

        // Finally increment the position.
        buffer.position(position + 112);

        // Well done for getting to the end of this method. I recommend you take a break and go look at cute puppies.
    }
}
