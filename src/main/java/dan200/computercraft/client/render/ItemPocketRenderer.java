/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.render;

import static dan200.computercraft.client.render.ComputerBorderRenderer.*;
import static dan200.computercraft.client.gui.FixedWidthFontRenderer.FONT_HEIGHT;
import static dan200.computercraft.client.gui.FixedWidthFontRenderer.FONT_WIDTH;
import static dan200.computercraft.client.render.ComputerBorderRenderer.BORDER;
import static dan200.computercraft.client.render.ComputerBorderRenderer.MARGIN;

import com.mojang.blaze3d.systems.RenderSystem;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.gui.FixedWidthFontRenderer;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.computer.core.ClientComputer;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import dan200.computercraft.shared.util.Colour;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Matrix4f;

import static dan200.computercraft.client.render.ComputerBorderRenderer.*;

/**
 * Emulates map rendering for pocket computers.
 */
public final class ItemPocketRenderer extends ItemMapLikeRenderer {
    public static final ItemPocketRenderer INSTANCE = new ItemPocketRenderer();

    private ItemPocketRenderer() {
    }

    @Override
    protected void renderItem(MatrixStack transform, VertexConsumerProvider render, ItemStack stack) {
        ClientComputer computer = ItemPocketComputer.createClientComputer(stack);
        Terminal terminal = computer == null ? null : computer.getTerminal();

        int termWidth, termHeight;
        if (terminal == null) {
            termWidth = ComputerCraft.pocketTermWidth;
            termHeight = ComputerCraft.pocketTermHeight;
        } else {
            termWidth = terminal.getWidth();
            termHeight = terminal.getHeight();
        }

        int width = termWidth * FONT_WIDTH + MARGIN * 2;
        int height = termHeight * FONT_HEIGHT + MARGIN * 2;

        // Setup various transformations. Note that these are partially adapted from the corresponding method
        // in ItemRenderer
        transform.push();
        transform.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(180f));
        transform.multiply(Vector3f.POSITIVE_Z.getDegreesQuaternion(180f));
        transform.scale(0.5f, 0.5f, 0.5f);

        float scale = 0.75f / Math.max(width + BORDER * 2, height + BORDER * 2 + LIGHT_HEIGHT);
        transform.scale(scale, scale, 0);
        transform.translate(-0.5 * width, -0.5 * height, 0);

        // Render the main frame
        ItemPocketComputer item = (ItemPocketComputer) stack.getItem();
        ComputerFamily family = item.getFamily();
        int frameColour = item.getColour(stack);

        Matrix4f matrix = transform.peek()
                                   .getModel();
        renderFrame(matrix, family, frameColour, width, height);

        // Render the light
        int lightColour = ItemPocketComputer.getLightState(stack);
        if (lightColour == -1) {
            lightColour = Colour.BLACK.getHex();
        }
        renderLight(matrix, lightColour, width, height);

        if (computer != null && terminal != null) {
            FixedWidthFontRenderer.drawTerminal(matrix, MARGIN, MARGIN, terminal, !computer.isColour(), MARGIN, MARGIN, MARGIN, MARGIN);
        } else {
            FixedWidthFontRenderer.drawEmptyTerminal(matrix, 0, 0, width, height);
        }

        transform.pop();
    }

    private static void renderFrame(Matrix4f transform, ComputerFamily family, int colour, int width, int height) {
        RenderSystem.enableBlend();
        MinecraftClient.getInstance()
                       .getTextureManager()
                       .bindTexture(colour != -1 ? ComputerBorderRenderer.BACKGROUND_COLOUR : ComputerBorderRenderer.getTexture(family));

        float r = ((colour >>> 16) & 0xFF) / 255.0f;
        float g = ((colour >>> 8) & 0xFF) / 255.0f;
        float b = (colour & 0xFF) / 255.0f;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR_TEXTURE);

        ComputerBorderRenderer.render(transform, buffer, 0, 0, 0, width, height, true, r, g, b);

        tessellator.draw();
    }

    private static void renderLight(Matrix4f transform, int colour, int width, int height) {
        RenderSystem.disableTexture();

        float r = ((colour >>> 16) & 0xFF) / 255.0f;
        float g = ((colour >>> 8) & 0xFF) / 255.0f;
        float b = (colour & 0xFF) / 255.0f;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR);
        buffer.vertex(transform, width - LIGHT_HEIGHT * 2, height + LIGHT_HEIGHT + BORDER / 2.0f, 0)
              .color(r, g, b, 1.0f)
              .next();
        buffer.vertex(transform, width, height + LIGHT_HEIGHT + BORDER / 2.0f, 0)
              .color(r, g, b, 1.0f)
              .next();
        buffer.vertex(transform, width, height + BORDER / 2.0f, 0)
              .color(r, g, b, 1.0f)
              .next();
        buffer.vertex(transform, width - LIGHT_HEIGHT * 2, height + BORDER / 2.0f, 0)
              .color(r, g, b, 1.0f)
              .next();

        tessellator.draw();
        RenderSystem.enableTexture();
    }
}
