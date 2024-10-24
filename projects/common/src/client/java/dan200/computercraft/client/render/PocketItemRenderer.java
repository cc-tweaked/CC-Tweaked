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
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.config.Config;
import dan200.computercraft.shared.pocket.items.PocketComputerItem;
import dan200.computercraft.shared.util.ARGB32;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

import static dan200.computercraft.client.render.ComputerBorderRenderer.*;
import static dan200.computercraft.client.render.text.FixedWidthFontRenderer.FONT_HEIGHT;
import static dan200.computercraft.client.render.text.FixedWidthFontRenderer.FONT_WIDTH;

/**
 * Emulates map rendering for pocket computers.
 */
public final class PocketItemRenderer extends ItemMapLikeRenderer {
    public static final PocketItemRenderer INSTANCE = new PocketItemRenderer();

    private PocketItemRenderer() {
    }

    @Override
    protected void renderItem(PoseStack transform, MultiBufferSource bufferSource, ItemStack stack, int light) {
        var computer = ClientPocketComputers.get(stack);
        var terminal = computer == null ? null : computer.getTerminal();

        int termWidth, termHeight;
        if (terminal == null) {
            termWidth = Config.pocketTermWidth;
            termHeight = Config.pocketTermHeight;
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
        var frameColour = item.getColour(stack);

        var matrix = transform.last().pose();
        renderFrame(matrix, bufferSource, family, frameColour, light, width, height);

        // Render the light
        var lightColour = computer == null || computer.getLightState() == -1 ? Colour.BLACK.getHex() : computer.getLightState();
        renderLight(transform, bufferSource, lightColour, width, height);

        var quadEmitter = FixedWidthFontRenderer.toVertexConsumer(transform, bufferSource.getBuffer(RenderTypes.TERMINAL));
        if (terminal == null) {
            FixedWidthFontRenderer.drawEmptyTerminal(quadEmitter, 0, 0, width, height);
        } else {
            FixedWidthFontRenderer.drawTerminal(quadEmitter, MARGIN, MARGIN, terminal, MARGIN, MARGIN, MARGIN, MARGIN);
        }

        transform.popPose();
    }

    private static void renderFrame(Matrix4f transform, MultiBufferSource render, ComputerFamily family, int colour, int light, int width, int height) {
        var texture = colour != -1 ? GuiSprites.COMPUTER_COLOUR : GuiSprites.getComputerTextures(family);

        var r = (colour >>> 16) & 0xFF;
        var g = (colour >>> 8) & 0xFF;
        var b = colour & 0xFF;

        var spriteRenderer = new SpriteRenderer(transform, render.getBuffer(RenderTypes.GUI_SPRITES), 0, light, r, g, b);
        ComputerBorderRenderer.render(spriteRenderer, texture, 0, 0, width, height, true);
    }

    private static void renderLight(PoseStack transform, MultiBufferSource render, int colour, int width, int height) {
        var buffer = render.getBuffer(RenderTypes.TERMINAL);
        FixedWidthFontRenderer.drawQuad(
            FixedWidthFontRenderer.toVertexConsumer(transform, buffer),
            width - LIGHT_HEIGHT * 2, height + BORDER / 2.0f, 0.001f, LIGHT_HEIGHT * 2, LIGHT_HEIGHT,
            ARGB32.opaque(colour), RenderTypes.FULL_BRIGHT_LIGHTMAP
        );
    }
}
