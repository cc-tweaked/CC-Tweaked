// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dan200.computercraft.client.render.text.FixedWidthFontRenderer;
import dan200.computercraft.core.terminal.Palette;
import dan200.computercraft.core.terminal.TextBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;

import static dan200.computercraft.client.render.text.FixedWidthFontRenderer.FONT_HEIGHT;
import static dan200.computercraft.shared.media.items.PrintoutItem.LINES_PER_PAGE;

/**
 * Renders printed pages or books, either for a GUI ({@link dan200.computercraft.client.gui.PrintoutScreen}) or
 * {@linkplain PrintoutItemRenderer in-hand/item frame printouts}.
 */
public final class PrintoutRenderer {
    private static final float BG_SIZE = 256.0f;

    /**
     * Width of a page.
     */
    public static final int X_SIZE = 172;

    /**
     * Height of a page.
     */
    public static final int Y_SIZE = 209;

    /**
     * Padding between the left and right of a page and the text.
     */
    public static final int X_TEXT_MARGIN = 13;

    /**
     * Padding between the top and bottom of a page and the text.
     */
    public static final int Y_TEXT_MARGIN = 11;

    /**
     * Width of the extra page texture.
     */
    private static final int X_FOLD_SIZE = 12;

    /**
     * Size of the leather cover.
     */
    public static final int COVER_SIZE = 12;

    private static final int COVER_Y = Y_SIZE;
    private static final int COVER_X = X_SIZE + 4 * X_FOLD_SIZE;

    private PrintoutRenderer() {
    }

    public static void drawText(PoseStack transform, MultiBufferSource bufferSource, int x, int y, int start, int light, TextBuffer[] text, TextBuffer[] colours) {
        var buffer = bufferSource.getBuffer(RenderTypes.PRINTOUT_TEXT);
        var emitter = FixedWidthFontRenderer.toVertexConsumer(transform, buffer);
        for (var line = 0; line < LINES_PER_PAGE && line < text.length; line++) {
            FixedWidthFontRenderer.drawString(emitter,
                x, y + line * FONT_HEIGHT, text[start + line], colours[start + line],
                Palette.DEFAULT, light
            );
        }
    }

    public static void drawText(PoseStack transform, MultiBufferSource bufferSource, int x, int y, int start, int light, String[] text, String[] colours) {
        var buffer = bufferSource.getBuffer(RenderTypes.PRINTOUT_TEXT);
        var emitter = FixedWidthFontRenderer.toVertexConsumer(transform, buffer);
        for (var line = 0; line < LINES_PER_PAGE && line < text.length; line++) {
            FixedWidthFontRenderer.drawString(emitter,
                x, y + line * FONT_HEIGHT,
                new TextBuffer(text[start + line]), new TextBuffer(colours[start + line]),
                Palette.DEFAULT, light
            );
        }
    }

    public static void drawBorder(PoseStack transform, MultiBufferSource bufferSource, float x, float y, float z, int page, int pages, boolean isBook, int light) {
        var matrix = transform.last().pose();
        var leftPages = page;
        var rightPages = pages - page - 1;

        var buffer = bufferSource.getBuffer(RenderTypes.PRINTOUT_BACKGROUND);

        if (isBook) {
            // Border
            var offset = offsetAt(pages);
            var left = x - 4 - offset;
            var right = x + X_SIZE + offset - 4;

            // Left and right border
            drawTexture(matrix, buffer, left - 4, y - 8, z - 0.02f, COVER_X, 0, COVER_SIZE, Y_SIZE + COVER_SIZE * 2, light);
            drawTexture(matrix, buffer, right, y - 8, z - 0.02f, COVER_X + COVER_SIZE, 0, COVER_SIZE, Y_SIZE + COVER_SIZE * 2, light);

            // Draw centre panel (just stretched texture, sorry).
            drawTexture(matrix, buffer,
                x - offset, y, z - 0.02f, X_SIZE + offset * 2, Y_SIZE,
                COVER_X + COVER_SIZE / 2.0f, COVER_SIZE, COVER_SIZE, Y_SIZE,
                light
            );

            var borderX = left;
            while (borderX < right) {
                double thisWidth = Math.min(right - borderX, X_SIZE);
                drawTexture(matrix, buffer, borderX, y - 8, z - 0.02f, 0, COVER_Y, (float) thisWidth, COVER_SIZE, light);
                drawTexture(matrix, buffer, borderX, y + Y_SIZE - 4, z - 0.02f, 0, COVER_Y + COVER_SIZE, (float) thisWidth, COVER_SIZE, light);
                borderX = (float) (borderX + thisWidth);
            }
        }

        // Current page background: Z-offset is interleaved between the "zeroth" left/right page and the first
        // left/right page, so that the "bold" border can be drawn over the edge where appropriate.
        drawTexture(matrix, buffer, x, y, z - 1e-3f * 0.5f, X_FOLD_SIZE * 2, 0, X_SIZE, Y_SIZE, light);

        // Left pages
        for (var n = 0; n <= leftPages; n++) {
            drawTexture(matrix, buffer,
                x - offsetAt(n), y, z - 1e-3f * n,
                // Use the left "bold" fold for the outermost page
                n == leftPages ? 0 : X_FOLD_SIZE, 0,
                X_FOLD_SIZE, Y_SIZE, light
            );
        }

        // Right pages
        for (var n = 0; n <= rightPages; n++) {
            drawTexture(matrix, buffer,
                x + (X_SIZE - X_FOLD_SIZE) + offsetAt(n), y, z - 1e-3f * n,
                // Two folds, then the main page. Use the right "bold" fold for the outermost page.
                X_FOLD_SIZE * 2 + X_SIZE + (n == rightPages ? X_FOLD_SIZE : 0), 0,
                X_FOLD_SIZE, Y_SIZE, light
            );
        }
    }

    private static void drawTexture(Matrix4f matrix, VertexConsumer buffer, float x, float y, float z, float u, float v, float width, float height, int light) {
        vertex(buffer, matrix, x, y + height, z, u / BG_SIZE, (v + height) / BG_SIZE, light);
        vertex(buffer, matrix, x + width, y + height, z, (u + width) / BG_SIZE, (v + height) / BG_SIZE, light);
        vertex(buffer, matrix, x + width, y, z, (u + width) / BG_SIZE, v / BG_SIZE, light);
        vertex(buffer, matrix, x, y, z, u / BG_SIZE, v / BG_SIZE, light);
    }

    private static void drawTexture(Matrix4f matrix, VertexConsumer buffer, float x, float y, float z, float width, float height, float u, float v, float tWidth, float tHeight, int light) {
        vertex(buffer, matrix, x, y + height, z, u / BG_SIZE, (v + tHeight) / BG_SIZE, light);
        vertex(buffer, matrix, x + width, y + height, z, (u + tWidth) / BG_SIZE, (v + tHeight) / BG_SIZE, light);
        vertex(buffer, matrix, x + width, y, z, (u + tWidth) / BG_SIZE, v / BG_SIZE, light);
        vertex(buffer, matrix, x, y, z, u / BG_SIZE, v / BG_SIZE, light);
    }

    private static void vertex(VertexConsumer buffer, Matrix4f matrix, float x, float y, float z, float u, float v, int light) {
        buffer.vertex(matrix, x, y, z).color(255, 255, 255, 255).uv(u, v).uv2(light).endVertex();
    }

    public static float offsetAt(int page) {
        return (float) (32 * (1 - Math.pow(1.2, -page)));
    }
}
