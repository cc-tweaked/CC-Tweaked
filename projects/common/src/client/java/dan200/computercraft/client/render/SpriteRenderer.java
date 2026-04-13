// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;


/**
 * A {@link GuiGraphicsExtractor}-equivalent that renders to a {@link VertexConsumer}. This is suitable for rendering
 * outside of a GUI, such as part of an entity renderer.
 * <p>
 * This batches all render calls together, though requires that all {@link TextureAtlasSprite}s are on the same sprite
 * sheet.
 */
public class SpriteRenderer {
    private final PoseStack transform;
    private final SubmitNodeCollector submit;
    private final int light;
    private final int z;
    private final int colour;

    public SpriteRenderer(PoseStack transform, SubmitNodeCollector submit, int z, int light, int colour) {
        this.transform = transform;
        this.submit = submit;
        this.z = z;
        this.light = light;
        this.colour = colour;
    }

    public void blit(TextureAtlasSprite sprite, int x0, int y0, int width, int height, int spriteX, int spriteY, int spriteWidth, int spriteHeight) {
        if (width == 0 || height == 0) return;

        var x1 = x0 + width;
        var y1 = y0 + height;
        var u0 = sprite.getU((float) spriteX / spriteWidth);
        var u1 = sprite.getU((float) (spriteX + width) / spriteWidth);
        var v0 = sprite.getV((float) spriteY / spriteHeight);
        var v1 = sprite.getV((float) (spriteY + height) / spriteHeight);

        submit.submitCustomGeometry(transform, RenderTypes.text(sprite.atlasLocation()), (t, vertices) -> {
            vertices.addVertex(t, x0, y1, z).setColor(colour).setUv(u0, v1).setLight(light);
            vertices.addVertex(t, x1, y1, z).setColor(colour).setUv(u1, v1).setLight(light);
            vertices.addVertex(t, x1, y0, z).setColor(colour).setUv(u1, v0).setLight(light);
            vertices.addVertex(t, x0, y0, z).setColor(colour).setUv(u0, v0).setLight(light);
        });
    }

    public void blitTiled(
        TextureAtlasSprite sprite,
        int x, int y, int width, int height,
        int tileX, int tileY, int tileWidth, int tileHeight, int spriteWidth, int spriteHeight
    ) {
        if (width <= 0 || height <= 0) return;
        if (tileWidth <= 0 || tileHeight <= 0) {
            throw new IllegalArgumentException("Tiled sprite texture size must be positive, got " + tileWidth + "x" + tileHeight);
        }

        for (var xOffset = 0; xOffset < width; xOffset += tileWidth) {
            var sliceWidth = Math.min(tileWidth, width - xOffset);
            for (var yOffset = 0; yOffset < height; yOffset += tileHeight) {
                var sliceHeight = Math.min(tileHeight, height - yOffset);
                blit(sprite, x + xOffset, y + yOffset, sliceWidth, sliceHeight, tileX, tileY, spriteWidth, spriteHeight);
            }
        }
    }
}
