// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.joml.Matrix4f;

/**
 * A {@link GuiGraphics}-equivalent which is suitable for both rendering in to a GUI and in-world (as part of an entity
 * renderer).
 * <p>
 * This batches all render calls together, though requires that all {@link TextureAtlasSprite}s are on the same sprite
 * sheet.
 */
public class SpriteRenderer {
    private final Matrix4f transform;
    private final VertexConsumer builder;
    private final int light;
    private final int z;
    private final int r, g, b;

    public SpriteRenderer(Matrix4f transform, VertexConsumer builder, int z, int light, int r, int g, int b) {
        this.transform = transform;
        this.builder = builder;
        this.z = z;
        this.light = light;
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public static SpriteRenderer createForGui(GuiGraphics graphics, RenderType renderType) {
        return new SpriteRenderer(
            graphics.pose().last().pose(), graphics.bufferSource().getBuffer(renderType),
            0, RenderTypes.FULL_BRIGHT_LIGHTMAP, 255, 255, 255
        );
    }

    /**
     * Render a single sprite.
     *
     * @param sprite The texture to draw.
     * @param x      The x position of the rectangle we'll draw.
     * @param y      The x position of the rectangle we'll draw.
     * @param width  The width of the rectangle we'll draw.
     * @param height The height of the rectangle we'll draw.
     */
    public void blit(TextureAtlasSprite sprite, int x, int y, int width, int height) {
        blit(x, y, width, height, sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1());
    }

    /**
     * Render a horizontal 3-sliced texture (i.e. split into left, middle and right). Unlike {@link GuiGraphics#blitNineSliced},
     * the middle texture is stretched rather than repeated.
     *
     * @param sprite       The texture to draw.
     * @param x            The x position of the rectangle we'll draw.
     * @param y            The x position of the rectangle we'll draw.
     * @param width        The width of the rectangle we'll draw.
     * @param height       The height of the rectangle we'll draw.
     * @param leftBorder   The width of the left border.
     * @param rightBorder  The width of the right border.
     * @param textureWidth The width of the whole texture.
     */
    public void blitHorizontalSliced(TextureAtlasSprite sprite, int x, int y, int width, int height, int leftBorder, int rightBorder, int textureWidth) {
        // TODO(1.20.2)/TODO(1.21.0): Drive this from mcmeta files, like vanilla does.
        if (width < leftBorder + rightBorder) throw new IllegalArgumentException("width is less than two borders");

        var centerStart = SpriteRenderer.u(sprite, leftBorder, textureWidth);
        var centerEnd = SpriteRenderer.u(sprite, textureWidth - rightBorder, textureWidth);

        blit(x, y, leftBorder, height, sprite.getU0(), sprite.getV0(), centerStart, sprite.getV1());
        blit(x + leftBorder, y, width - leftBorder - rightBorder, height, centerStart, sprite.getV0(), centerEnd, sprite.getV1());
        blit(x + width - rightBorder, y, rightBorder, height, centerEnd, sprite.getV0(), sprite.getU1(), sprite.getV1());
    }

    /**
     * Render a vertical 3-sliced texture (i.e. split into top, middle and bottom). Unlike {@link GuiGraphics#blitNineSliced},
     * the middle texture is stretched rather than repeated.
     *
     * @param sprite        The texture to draw.
     * @param x             The x position of the rectangle we'll draw.
     * @param y             The x position of the rectangle we'll draw.
     * @param width         The width of the rectangle we'll draw.
     * @param height        The height of the rectangle we'll draw.
     * @param topBorder     The height of the top border.
     * @param bottomBorder  The height of the bottom border.
     * @param textureHeight The height of the whole texture.
     */
    public void blitVerticalSliced(TextureAtlasSprite sprite, int x, int y, int width, int height, int topBorder, int bottomBorder, int textureHeight) {
        // TODO(1.20.2)/TODO(1.21.0): Drive this from mcmeta files, like vanilla does.
        if (width < topBorder + bottomBorder) throw new IllegalArgumentException("height is less than two borders");

        var centerStart = SpriteRenderer.v(sprite, topBorder, textureHeight);
        var centerEnd = SpriteRenderer.v(sprite, textureHeight - bottomBorder, textureHeight);

        blit(x, y, width, topBorder, sprite.getU0(), sprite.getV0(), sprite.getU1(), centerStart);
        blit(x, y + topBorder, width, height - topBorder - bottomBorder, sprite.getU0(), centerStart, sprite.getU1(), centerEnd);
        blit(x, y + height - bottomBorder, width, bottomBorder, sprite.getU0(), centerEnd, sprite.getU1(), sprite.getV1());
    }

    /**
     * The low-level blit function, used to render a portion of the sprite sheet. Unlike other functions, this takes uvs rather than a single sprite.
     *
     * @param x      The x position of the rectangle we'll draw.
     * @param y      The x position of the rectangle we'll draw.
     * @param width  The width of the rectangle we'll draw.
     * @param height The height of the rectangle we'll draw.
     * @param u0     The first U coordinate.
     * @param v0     The first V coordinate.
     * @param u1     The second U coordinate.
     * @param v1     The second V coordinate.
     */
    public void blit(
        int x, int y, int width, int height, float u0, float v0, float u1, float v1) {
        builder.vertex(transform, x, y + height, z).color(r, g, b, 255).uv(u0, v1).uv2(light).endVertex();
        builder.vertex(transform, x + width, y + height, z).color(r, g, b, 255).uv(u1, v1).uv2(light).endVertex();
        builder.vertex(transform, x + width, y, z).color(r, g, b, 255).uv(u1, v0).uv2(light).endVertex();
        builder.vertex(transform, x, y, z).color(r, g, b, 255).uv(u0, v0).uv2(light).endVertex();
    }

    public static float u(TextureAtlasSprite sprite, int x, int width) {
        return sprite.getU((double) x / width * 16);
    }

    public static float v(TextureAtlasSprite sprite, int y, int height) {
        return sprite.getV((double) y / height * 16);
    }
}
