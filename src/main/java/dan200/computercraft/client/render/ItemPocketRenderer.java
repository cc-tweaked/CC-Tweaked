/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.gui.FixedWidthFontRenderer;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.computer.core.ClientComputer;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import dan200.computercraft.shared.util.Colour;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import static dan200.computercraft.client.gui.FixedWidthFontRenderer.FONT_HEIGHT;
import static dan200.computercraft.client.gui.FixedWidthFontRenderer.FONT_WIDTH;
import static dan200.computercraft.client.render.ComputerBorderRenderer.*;

/**
 * Emulates map rendering for pocket computers.
 */
public final class ItemPocketRenderer extends ItemMapLikeRenderer
{
    public static final ItemPocketRenderer INSTANCE = new ItemPocketRenderer();

    private ItemPocketRenderer()
    {
    }

    @Override
    protected void renderItem( PoseStack transform, MultiBufferSource render, ItemStack stack, int light )
    {
        ClientComputer computer = ItemPocketComputer.createClientComputer( stack );
        Terminal terminal = computer == null ? null : computer.getTerminal();

        int termWidth, termHeight;
        if( terminal == null )
        {
            termWidth = ComputerCraft.pocketTermWidth;
            termHeight = ComputerCraft.pocketTermHeight;
        }
        else
        {
            termWidth = terminal.getWidth();
            termHeight = terminal.getHeight();
        }

        int width = termWidth * FONT_WIDTH + MARGIN * 2;
        int height = termHeight * FONT_HEIGHT + MARGIN * 2;

        // Setup various transformations. Note that these are partially adapted from the corresponding method
        // in ItemRenderer
        transform.pushPose();
        transform.mulPose( Vector3f.YP.rotationDegrees( 180f ) );
        transform.mulPose( Vector3f.ZP.rotationDegrees( 180f ) );
        transform.scale( 0.5f, 0.5f, 0.5f );

        float scale = 0.75f / Math.max( width + BORDER * 2, height + BORDER * 2 + LIGHT_HEIGHT );
        transform.scale( scale, scale, 0 );
        transform.translate( -0.5 * width, -0.5 * height, 0 );

        // Render the main frame
        ItemPocketComputer item = (ItemPocketComputer) stack.getItem();
        ComputerFamily family = item.getFamily();
        int frameColour = item.getColour( stack );

        Matrix4f matrix = transform.last()
            .pose();
        renderFrame( matrix, render, family, frameColour, light, width, height );

        // Render the light
        int lightColour = ItemPocketComputer.getLightState( stack );
        if( lightColour == -1 )
        {
            lightColour = Colour.BLACK.getHex();
        }
        renderLight( matrix, lightColour, width, height );

        if( computer != null && terminal != null )
        {
            FixedWidthFontRenderer.drawTerminal(
                matrix, render.getBuffer( RenderTypes.TERMINAL_WITHOUT_DEPTH ),
                MARGIN, MARGIN, terminal, !computer.isColour(), MARGIN, MARGIN, MARGIN, MARGIN
            );
            FixedWidthFontRenderer.drawBlocker( transform.last().pose(), render, 0, 0, width, height );

        }
        else
        {
            FixedWidthFontRenderer.drawEmptyTerminal( matrix, 0, 0, width, height );
        }

        transform.popPose();
    }

    private static void renderFrame( Matrix4f transform, MultiBufferSource render, ComputerFamily family, int colour, int light, int width, int height )
    {
        RenderSystem.enableBlend();
        Minecraft.getInstance()
            .getTextureManager()
            .bindForSetup( colour != -1 ? ComputerBorderRenderer.BACKGROUND_COLOUR : ComputerBorderRenderer.getTexture( family ) );

        ResourceLocation texture = colour != -1 ? ComputerBorderRenderer.BACKGROUND_COLOUR : ComputerBorderRenderer.getTexture( family );

        float r = ((colour >>> 16) & 0xFF) / 255.0f;
        float g = ((colour >>> 8) & 0xFF) / 255.0f;
        float b = (colour & 0xFF) / 255.0f;

        ComputerBorderRenderer.render( transform, render.getBuffer( RenderType.text( texture ) ), 0, 0, 0, light, width, height, true, r, g, b );
    }

    private static void renderLight( Matrix4f transform, int colour, int width, int height )
    {
        RenderSystem.disableTexture();

        float r = ((colour >>> 16) & 0xFF) / 255.0f;
        float g = ((colour >>> 8) & 0xFF) / 255.0f;
        float b = (colour & 0xFF) / 255.0f;

        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();
        buffer.begin( VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR );
        buffer.vertex( transform, width - LIGHT_HEIGHT * 2, height + LIGHT_HEIGHT + BORDER / 2.0f, 0 )
            .color( r, g, b, 1.0f )
            .endVertex();
        buffer.vertex( transform, width, height + LIGHT_HEIGHT + BORDER / 2.0f, 0 )
            .color( r, g, b, 1.0f )
            .endVertex();
        buffer.vertex( transform, width, height + BORDER / 2.0f, 0 )
            .color( r, g, b, 1.0f )
            .endVertex();
        buffer.vertex( transform, width - LIGHT_HEIGHT * 2, height + BORDER / 2.0f, 0 )
            .color( r, g, b, 1.0f )
            .endVertex();

        tessellator.end();
        RenderSystem.enableTexture();
    }
}
