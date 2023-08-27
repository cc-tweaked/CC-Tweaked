// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.client.render;

import dan200.computercraft.client.gui.GuiSprites;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import static dan200.computercraft.client.render.SpriteRenderer.u;
import static dan200.computercraft.client.render.SpriteRenderer.v;

/**
 * Renders the borders of computers, either for a GUI ({@link dan200.computercraft.client.gui.ComputerScreen}) or
 * {@linkplain PocketItemRenderer in-hand pocket computers}.
 */
public final class ComputerBorderRenderer {
    /**
     * The margin between the terminal and its border.
     */
    public static final int MARGIN = 2;

    /**
     * The width of the terminal border.
     */
    public static final int BORDER = 12;

    public static final int LIGHT_HEIGHT = 8;

    private static final int TEX_SIZE = 36;

    private ComputerBorderRenderer() {
    }

    public static void render(SpriteRenderer renderer, GuiSprites.ComputerTextures textures, int x, int y, int width, int height, boolean withLight) {
        var endX = x + width;
        var endY = y + height;

        var border = GuiSprites.get(textures.border());

        // Top bar
        blitBorder(renderer, border, x - BORDER, y - BORDER, 0, 0, BORDER, BORDER);
        blitBorder(renderer, border, x, y - BORDER, BORDER, 0, width, BORDER);
        blitBorder(renderer, border, endX, y - BORDER, BORDER * 2, 0, BORDER, BORDER);

        // Vertical bars
        blitBorder(renderer, border, x - BORDER, y, 0, BORDER, BORDER, height);
        blitBorder(renderer, border, endX, y, BORDER * 2, BORDER, BORDER, height);

        // Bottom bar. We allow for drawing a stretched version, which allows for additional elements (such as the
        // pocket computer's lights).
        if (withLight) {
            var pocketBottomTexture = textures.pocketBottom();
            if (pocketBottomTexture == null) throw new NullPointerException(textures + " has no pocket texture");
            var pocketBottom = GuiSprites.get(pocketBottomTexture);

            renderer.blitHorizontalSliced(
                pocketBottom, x - BORDER, endY, width + BORDER * 2, BORDER + LIGHT_HEIGHT,
                BORDER, BORDER, BORDER * 3
            );
        } else {
            blitBorder(renderer, border, x - BORDER, endY, 0, BORDER * 2, BORDER, BORDER);
            blitBorder(renderer, border, x, endY, BORDER, BORDER * 2, width, BORDER);
            blitBorder(renderer, border, endX, endY, BORDER * 2, BORDER * 2, BORDER, BORDER);
        }
    }

    private static void blitBorder(SpriteRenderer renderer, TextureAtlasSprite sprite, int x, int y, int u, int v, int width, int height) {
        renderer.blit(
            x, y, width, height,
            u(sprite, u, TEX_SIZE), v(sprite, v, TEX_SIZE),
            u(sprite, u + BORDER, TEX_SIZE), v(sprite, v + BORDER, TEX_SIZE)
        );
    }
}
