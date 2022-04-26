/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.render.text.FixedWidthFontRenderer;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.computer.core.ClientComputer;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import dan200.computercraft.shared.util.Colour;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static dan200.computercraft.client.render.ComputerBorderRenderer.*;
import static dan200.computercraft.client.render.text.FixedWidthFontRenderer.FONT_HEIGHT;
import static dan200.computercraft.client.render.text.FixedWidthFontRenderer.FONT_WIDTH;

/**
 * Emulates map rendering for pocket computers.
 */
@Mod.EventBusSubscriber( modid = ComputerCraft.MOD_ID, value = Dist.CLIENT )
public final class ItemPocketRenderer extends ItemMapLikeRenderer
{
    private static final ItemPocketRenderer INSTANCE = new ItemPocketRenderer();

    private ItemPocketRenderer()
    {
    }

    @SubscribeEvent
    public static void onRenderInHand( RenderHandEvent event )
    {
        ItemStack stack = event.getItemStack();
        if( !(stack.getItem() instanceof ItemPocketComputer) ) return;

        event.setCanceled( true );
        INSTANCE.renderItemFirstPerson(
            event.getMatrixStack(), event.getBuffers(), event.getLight(),
            event.getHand(), event.getInterpolatedPitch(), event.getEquipProgress(), event.getSwingProgress(), event.getItemStack()
        );
    }

    @Override
    protected void renderItem( MatrixStack transform, IRenderTypeBuffer bufferSource, ItemStack stack, int light )
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

        Matrix4f matrix = transform.last().pose();
        renderFrame( matrix, bufferSource, family, frameColour, light, width, height );

        // Render the light
        int lightColour = ItemPocketComputer.getLightState( stack );
        if( lightColour == -1 ) lightColour = Colour.BLACK.getHex();
        renderLight( matrix, bufferSource, lightColour, width, height );

        if( computer != null && terminal != null )
        {
            FixedWidthFontRenderer.drawTerminal(
                matrix, bufferSource.getBuffer( RenderTypes.TERMINAL_WITHOUT_DEPTH ),
                MARGIN, MARGIN, terminal, !computer.isColour(), MARGIN, MARGIN, MARGIN, MARGIN
            );
            FixedWidthFontRenderer.drawBlocker(
                matrix, bufferSource.getBuffer( RenderTypes.TERMINAL_BLOCKER ),
                0, 0, width, height
            );
        }
        else
        {
            FixedWidthFontRenderer.drawEmptyTerminal(
                matrix, bufferSource.getBuffer( RenderTypes.TERMINAL_WITH_DEPTH ),
                0, 0, width, height
            );
        }

        transform.popPose();
    }

    private static void renderFrame( Matrix4f transform, IRenderTypeBuffer bufferSource, ComputerFamily family, int colour, int light, int width, int height )
    {
        ResourceLocation texture = colour != -1 ? ComputerBorderRenderer.BACKGROUND_COLOUR : ComputerBorderRenderer.getTexture( family );

        float r = ((colour >>> 16) & 0xFF) / 255.0f;
        float g = ((colour >>> 8) & 0xFF) / 255.0f;
        float b = (colour & 0xFF) / 255.0f;

        ComputerBorderRenderer.render( transform, bufferSource.getBuffer( ComputerBorderRenderer.getRenderType( texture ) ), 0, 0, 0, light, width, height, true, r, g, b );
    }

    private static void renderLight( Matrix4f transform, IRenderTypeBuffer bufferSource, int colour, int width, int height )
    {
        byte r = (byte) ((colour >>> 16) & 0xFF);
        byte g = (byte) ((colour >>> 8) & 0xFF);
        byte b = (byte) (colour & 0xFF);
        byte[] c = new byte[] { r, g, b, (byte) 255 };

        IVertexBuilder buffer = bufferSource.getBuffer( RenderTypes.TERMINAL_WITH_DEPTH );
        FixedWidthFontRenderer.drawQuad(
            transform, buffer,
            width - LIGHT_HEIGHT * 2, height + BORDER / 2.0f, 0.001f, LIGHT_HEIGHT * 2, LIGHT_HEIGHT,
            c, RenderTypes.FULL_BRIGHT_LIGHTMAP
        );
    }
}
