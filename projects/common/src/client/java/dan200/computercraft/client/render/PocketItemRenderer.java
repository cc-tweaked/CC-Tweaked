/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import dan200.computercraft.client.pocket.ClientPocketComputers;
import dan200.computercraft.client.render.text.FixedWidthFontRenderer;
import dan200.computercraft.core.util.Colour;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.pocket.items.PocketComputerItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;

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
        var terminal = computer.getTerminal();

        var termWidth = terminal.getWidth();
        var termHeight = terminal.getHeight();

        var width = termWidth * FONT_WIDTH + MARGIN * 2;
        var height = termHeight * FONT_HEIGHT + MARGIN * 2;

        // Setup various transformations. Note that these are partially adapted from the corresponding method
        // in ItemRenderer
        transform.pushPose();
        transform.mulPose(Vector3f.YP.rotationDegrees(180f));
        transform.mulPose(Vector3f.ZP.rotationDegrees(180f));
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
        var lightColour = ClientPocketComputers.get(stack).getLightState();
        if (lightColour == -1) lightColour = Colour.BLACK.getHex();
        renderLight(transform, bufferSource, lightColour, width, height);

        FixedWidthFontRenderer.drawTerminal(
            FixedWidthFontRenderer.toVertexConsumer(transform, bufferSource.getBuffer(RenderTypes.TERMINAL)),
            MARGIN, MARGIN, terminal, MARGIN, MARGIN, MARGIN, MARGIN
        );

        transform.popPose();
    }

    private static void renderFrame(Matrix4f transform, MultiBufferSource render, ComputerFamily family, int colour, int light, int width, int height) {
        var texture = colour != -1 ? ComputerBorderRenderer.BACKGROUND_COLOUR : ComputerBorderRenderer.getTexture(family);

        var r = ((colour >>> 16) & 0xFF) / 255.0f;
        var g = ((colour >>> 8) & 0xFF) / 255.0f;
        var b = (colour & 0xFF) / 255.0f;

        ComputerBorderRenderer.render(transform, render.getBuffer(ComputerBorderRenderer.getRenderType(texture)), 0, 0, 0, light, width, height, true, r, g, b);
    }

    private static void renderLight(PoseStack transform, MultiBufferSource render, int colour, int width, int height) {
        var r = (byte) ((colour >>> 16) & 0xFF);
        var g = (byte) ((colour >>> 8) & 0xFF);
        var b = (byte) (colour & 0xFF);
        var c = new byte[]{ r, g, b, (byte) 255 };

        var buffer = render.getBuffer(RenderTypes.TERMINAL);
        FixedWidthFontRenderer.drawQuad(
            FixedWidthFontRenderer.toVertexConsumer(transform, buffer),
            width - LIGHT_HEIGHT * 2, height + BORDER / 2.0f, 0.001f, LIGHT_HEIGHT * 2, LIGHT_HEIGHT,
            c, RenderTypes.FULL_BRIGHT_LIGHTMAP
        );
    }
}
