// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dan200.computercraft.client.gui.GuiSprites;
import dan200.computercraft.client.pocket.ClientPocketComputers;
import dan200.computercraft.client.render.text.FixedWidthFontRenderer;
import dan200.computercraft.core.util.Colour;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.config.Config;
import dan200.computercraft.shared.pocket.items.PocketComputerItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.gui.GuiMetadataSection;
import net.minecraft.client.resources.metadata.gui.GuiSpriteScaling;
import net.minecraft.data.AtlasIds;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;

import static dan200.computercraft.client.render.ComputerBorderRenderer.BORDER;
import static dan200.computercraft.client.render.ComputerBorderRenderer.MARGIN;
import static dan200.computercraft.client.render.text.FixedWidthFontRenderer.FONT_HEIGHT;
import static dan200.computercraft.client.render.text.FixedWidthFontRenderer.FONT_WIDTH;

/**
 * Emulates map rendering for pocket computers.
 */
public final class PocketItemRenderer extends ItemMapLikeRenderer {
    public static final PocketItemRenderer INSTANCE = new PocketItemRenderer();

    /**
     * The height of the pocket computer's light.
     */
    private static final int LIGHT_HEIGHT = 8;

    private PocketItemRenderer() {
    }

    @Override
    protected void renderItem(PoseStack transform, SubmitNodeCollector collector, ItemStack stack, int light) {
        var computer = ClientPocketComputers.get(stack);
        var terminal = computer == null ? null : computer.getTerminal();

        int termWidth, termHeight;
        if (terminal == null) {
            var terminalSize = stack.get(ModRegistry.DataComponents.TERMINAL_SIZE.get());
            if (terminalSize != null) {
                termWidth = terminalSize.width();
                termHeight = terminalSize.height();
            } else {
                termWidth = Config.DEFAULT_POCKET_TERM_WIDTH;
                termHeight = Config.DEFAULT_POCKET_TERM_HEIGHT;
            }
        } else {
            termWidth = terminal.getWidth();
            termHeight = terminal.getHeight();
        }

        var width = termWidth * FONT_WIDTH + MARGIN * 2;
        var height = termHeight * FONT_HEIGHT + MARGIN * 2;

        // Setup various transformations. Note that these are partially adapted from the corresponding method
        // in ItemRenderer
        transform.pushPose();
        transform.mulPose(Axis.YP.rotationDegrees(180f));
        transform.mulPose(Axis.ZP.rotationDegrees(180f));
        transform.scale(0.5f, 0.5f, 0.5f);

        var scale = 0.75f / Math.max(width + BORDER * 2, height + BORDER * 2 + LIGHT_HEIGHT);
        transform.scale(scale, scale, -1.0f);
        transform.translate(-0.5 * width, -0.5 * height, 0);

        // Render the main frame
        var item = (PocketComputerItem) stack.getItem();
        var family = item.getFamily();
        var frameColour = DyedItemColor.getOrDefault(stack, -1);

        renderFrame(transform, collector, family, frameColour, light, width, height);

        // Render the light
        var lightColour = computer == null || computer.getLightState() == -1 ? Colour.BLACK.getHex() : computer.getLightState();
        renderLight(transform, collector, lightColour, width, height);

        if (terminal == null) {
            FixedWidthFontRenderer.drawEmptyTerminal(transform, collector, 0, 0, width, height);
        } else {
            collector.submitCustomGeometry(transform, FixedWidthFontRenderer.TERMINAL_TEXT, (pose, buffer) ->
                FixedWidthFontRenderer.drawTerminal(pose.pose(), buffer, MARGIN, MARGIN, terminal, MARGIN, MARGIN, MARGIN, MARGIN));
        }

        transform.popPose();
    }

    private static void renderFrame(PoseStack transform, SubmitNodeCollector submit, ComputerFamily family, int colour, int light, int width, int height) {
        var textures = colour != -1 ? GuiSprites.COMPUTER_COLOUR : GuiSprites.getComputerTextures(family);
        var spriteRenderer = new SpriteRenderer(transform, submit, 0, light, colour);
        renderBorder(spriteRenderer, textures, width, height);
    }

    private static void renderBorder(SpriteRenderer renderer, GuiSprites.ComputerTextures textures, int width, int height) {
        var sprites = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.GUI);

        // Find our border, forcing it to be a nine-sliced texture.
        var borderSprite = sprites.getSprite(textures.border());
        var borderSlice = getSlice(borderSprite, DEFAULT_BORDER);
        var borderBounds = borderSlice.border();

        // And take the separate bottom bit of the pocket computer.
        var bottomTexture = textures.pocketBottom();
        if (bottomTexture == null) throw new NullPointerException(textures + " has no pocket texture");
        var bottomSprite = sprites.getSprite(bottomTexture);
        var bottomSlice = getSlice(bottomSprite, DEFAULT_BOTTOM);
        var bottomBounds = bottomSlice.border();

        // Now draw a nine-sliced texture, by stitching together the top parts of the border with the pocket bottom.

        // Top bar
        renderer.blit(
            borderSprite, -borderBounds.left(), -borderBounds.top(), borderBounds.left(), borderBounds.top(),
            0, 0, borderSlice.width(), borderSlice.height()
        );
        renderer.blitTiled(
            borderSprite, 0, -borderBounds.top(), width, borderBounds.top(),
            borderBounds.left(), 0, borderSlice.width() - borderBounds.left() - borderBounds.right(), borderBounds.top(),
            borderSlice.width(), borderSlice.height()
        );
        renderer.blit(
            borderSprite, width, -borderBounds.top(), borderBounds.right(), borderBounds.top(),
            borderSlice.width() - borderBounds.right(), 0, borderSlice.width(), borderSlice.height()
        );

        // Vertical bars
        renderer.blitTiled(
            borderSprite, -borderBounds.left(), 0, borderBounds.left(), height,
            0, borderBounds.top(), borderBounds.left(), borderSlice.height() - borderBounds.top() - borderBounds.bottom(),
            borderSlice.width(), borderSlice.height()
        );
        renderer.blitTiled(
            borderSprite, width, 0, borderBounds.right(), height,
            borderSlice.width() - borderBounds.right(), borderBounds.top(), borderBounds.right(), borderSlice.height() - borderBounds.top() - borderBounds.bottom(),
            borderSlice.width(), borderSlice.height()
        );

        // Bottom
        renderer.blit(
            bottomSprite, -bottomBounds.left(), height, bottomBounds.left(), bottomSlice.height(),
            0, 0, bottomSlice.width(), bottomSlice.height()
        );
        renderer.blitTiled(
            bottomSprite, 0, height, width, bottomSlice.height(),
            bottomBounds.left(), 0, bottomSlice.width() - bottomBounds.left() - bottomBounds.right(), bottomSlice.height(),
            bottomSlice.width(), bottomSlice.height()
        );
        renderer.blit(
            bottomSprite, width, height, bottomBounds.right(), bottomSlice.height(),
            bottomSlice.width() - bottomBounds.right(), 0, bottomSlice.width(), bottomSlice.height()
        );
    }

    private static void renderLight(PoseStack transform, SubmitNodeCollector render, int colour, int width, int height) {
        render.submitCustomGeometry(transform, FixedWidthFontRenderer.TERMINAL_TEXT, (pose, buffer) -> FixedWidthFontRenderer.drawQuad(
            pose.pose(), buffer,
            width - LIGHT_HEIGHT * 2, height + BORDER / 2.0f, 0.001f, LIGHT_HEIGHT * 2, LIGHT_HEIGHT,
            ARGB.opaque(colour), LightCoordsUtil.FULL_BRIGHT
        ));
    }

    private static final GuiSpriteScaling.NineSlice DEFAULT_BORDER = new GuiSpriteScaling.NineSlice(
        36, 36, new GuiSpriteScaling.NineSlice.Border(12, 12, 12, 12), false
    );

    private static final GuiSpriteScaling.NineSlice DEFAULT_BOTTOM = new GuiSpriteScaling.NineSlice(
        36, 20, new GuiSpriteScaling.NineSlice.Border(12, 0, 12, 0), false
    );

    private static GuiSpriteScaling.NineSlice getSlice(GuiSpriteScaling scaling, GuiSpriteScaling.NineSlice fallback) {
        return scaling instanceof GuiSpriteScaling.NineSlice slice ? slice : fallback;
    }

    private static GuiSpriteScaling.NineSlice getSlice(TextureAtlasSprite sprite, GuiSpriteScaling.NineSlice fallback) {
        return getSlice(sprite.contents().getAdditionalMetadata(GuiMetadataSection.TYPE).orElse(GuiMetadataSection.DEFAULT).scaling(), fallback);
    }
}
