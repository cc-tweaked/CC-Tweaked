/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.gui.FixedWidthFontRenderer;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.computer.core.ClientComputer;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import dan200.computercraft.shared.util.Colour;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderSpecificHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL11;

import static dan200.computercraft.client.gui.FixedWidthFontRenderer.FONT_HEIGHT;
import static dan200.computercraft.client.gui.FixedWidthFontRenderer.FONT_WIDTH;
import static dan200.computercraft.client.gui.GuiComputer.*;

/**
 * Emulates map rendering for pocket computers.
 */
@Mod.EventBusSubscriber( modid = ComputerCraft.MOD_ID, value = Dist.CLIENT )
public final class ItemPocketRenderer extends ItemMapLikeRenderer
{
    private static final int MARGIN = 2;
    private static final int FRAME = 12;
    private static final int LIGHT_HEIGHT = 8;

    private static final ItemPocketRenderer INSTANCE = new ItemPocketRenderer();

    private ItemPocketRenderer()
    {
    }

    @SubscribeEvent
    public static void renderItem( RenderSpecificHandEvent event )
    {
        ItemStack stack = event.getItemStack();
        if( !(stack.getItem() instanceof ItemPocketComputer) ) return;

        event.setCanceled( true );
        INSTANCE.renderItemFirstPerson( event.getHand(), event.getInterpolatedPitch(), event.getEquipProgress(), event.getSwingProgress(), event.getItemStack() );
    }

    @Override
    protected void renderItem( ItemStack stack )
    {
        ClientComputer computer = ItemPocketComputer.createClientComputer( stack );
        Terminal terminal = computer == null ? null : computer.getTerminal();

        int termWidth, termHeight;
        if( terminal == null )
        {
            termWidth = ComputerCraft.terminalWidth_pocketComputer;
            termHeight = ComputerCraft.terminalHeight_pocketComputer;
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
        GlStateManager.pushMatrix();

        GlStateManager.disableLighting();
        GlStateManager.disableDepthTest();

        GlStateManager.rotatef( 180f, 0f, 1f, 0f );
        GlStateManager.rotatef( 180f, 0f, 0f, 1f );
        GlStateManager.scalef( 0.5f, 0.5f, 0.5f );

        double scale = 0.75 / Math.max( width + FRAME * 2, height + FRAME * 2 + LIGHT_HEIGHT );
        GlStateManager.scaled( scale, scale, 0 );
        GlStateManager.translated( -0.5 * width, -0.5 * height, 0 );

        // Render the main frame
        ItemPocketComputer item = (ItemPocketComputer) stack.getItem();
        ComputerFamily family = item.getFamily();
        int frameColour = item.getColour( stack );
        renderFrame( family, frameColour, width, height );

        // Render the light
        int lightColour = ItemPocketComputer.getLightState( stack );
        if( lightColour == -1 ) lightColour = Colour.Black.getHex();
        renderLight( lightColour, width, height );

        if( computer != null && terminal != null )
        {
            FixedWidthFontRenderer.drawTerminal( MARGIN, MARGIN, terminal, !computer.isColour(), MARGIN, MARGIN, MARGIN, MARGIN );
        }
        else
        {
            FixedWidthFontRenderer.drawEmptyTerminal( 0, 0, width, height );
        }

        GlStateManager.enableDepthTest();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }

    private static void renderFrame( ComputerFamily family, int colour, int width, int height )
    {

        Minecraft.getInstance().getTextureManager().bindTexture( colour != -1
            ? BACKGROUND_COLOUR
            : family == ComputerFamily.Normal ? BACKGROUND_NORMAL : BACKGROUND_ADVANCED
        );

        float r = ((colour >>> 16) & 0xFF) / 255.0f;
        float g = ((colour >>> 8) & 0xFF) / 255.0f;
        float b = (colour & 0xFF) / 255.0f;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin( GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR );

        // Top left, middle, right
        renderTexture( buffer, -FRAME, -FRAME, 12, 28, FRAME, FRAME, r, g, b );
        renderTexture( buffer, 0, -FRAME, 0, 0, width, FRAME, r, g, b );
        renderTexture( buffer, width, -FRAME, 24, 28, FRAME, FRAME, r, g, b );

        // Left and bright border
        renderTexture( buffer, -FRAME, 0, 0, 28, FRAME, height, r, g, b );
        renderTexture( buffer, width, 0, 36, 28, FRAME, height, r, g, b );

        // Bottom left, middle, right. We do this in three portions: the top inner corners, an extended region for
        // lights, and then the bottom outer corners.
        renderTexture( buffer, -FRAME, height, 12, 40, FRAME, FRAME / 2, r, g, b );
        renderTexture( buffer, 0, height, 0, 12, width, FRAME / 2, r, g, b );
        renderTexture( buffer, width, height, 24, 40, FRAME, FRAME / 2, r, g, b );

        renderTexture( buffer, -FRAME, height + FRAME / 2, 12, 44, FRAME, LIGHT_HEIGHT, FRAME, 4, r, g, b );
        renderTexture( buffer, 0, height + FRAME / 2, 0, 16, width, LIGHT_HEIGHT, FRAME, 4, r, g, b );
        renderTexture( buffer, width, height + FRAME / 2, 24, 44, FRAME, LIGHT_HEIGHT, FRAME, 4, r, g, b );

        renderTexture( buffer, -FRAME, height + LIGHT_HEIGHT + FRAME / 2, 12, 40 + FRAME / 2, FRAME, FRAME / 2, r, g, b );
        renderTexture( buffer, 0, height + LIGHT_HEIGHT + FRAME / 2, 0, 12 + FRAME / 2, width, FRAME / 2, r, g, b );
        renderTexture( buffer, width, height + LIGHT_HEIGHT + FRAME / 2, 24, 40 + FRAME / 2, FRAME, FRAME / 2, r, g, b );

        tessellator.draw();
    }

    private static void renderLight( int colour, int width, int height )
    {
        GlStateManager.enableBlend();
        GlStateManager.disableTexture();

        float r = ((colour >>> 16) & 0xFF) / 255.0f;
        float g = ((colour >>> 8) & 0xFF) / 255.0f;
        float b = (colour & 0xFF) / 255.0f;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin( GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR );
        buffer.pos( width - LIGHT_HEIGHT * 2, height + LIGHT_HEIGHT + FRAME / 2.0f, 0.0D ).color( r, g, b, 1.0f ).endVertex();
        buffer.pos( width, height + LIGHT_HEIGHT + FRAME / 2.0f, 0.0D ).color( r, g, b, 1.0f ).endVertex();
        buffer.pos( width, height + FRAME / 2.0f, 0.0D ).color( r, g, b, 1.0f ).endVertex();
        buffer.pos( width - LIGHT_HEIGHT * 2, height + FRAME / 2.0f, 0.0D ).color( r, g, b, 1.0f ).endVertex();

        tessellator.draw();
        GlStateManager.enableTexture();
    }

    private static void renderTexture( BufferBuilder builder, int x, int y, int textureX, int textureY, int width, int height, float r, float g, float b )
    {
        renderTexture( builder, x, y, textureX, textureY, width, height, width, height, r, g, b );
    }

    private static void renderTexture( BufferBuilder builder, int x, int y, int textureX, int textureY, int width, int height, int textureWidth, int textureHeight, float r, float g, float b )
    {
        float scale = 1 / 255.0f;
        builder.pos( x, y + height, 0 ).tex( textureX * scale, (textureY + textureHeight) * scale ).color( r, g, b, 1.0f ).endVertex();
        builder.pos( x + width, y + height, 0 ).tex( (textureX + textureWidth) * scale, (textureY + textureHeight) * scale ).color( r, g, b, 1.0f ).endVertex();
        builder.pos( x + width, y, 0 ).tex( (textureX + textureWidth) * scale, textureY * scale ).color( r, g, b, 1.0f ).endVertex();
        builder.pos( x, y, 0 ).tex( textureX * scale, textureY * scale ).color( r, g, b, 1.0f ).endVertex();
    }
}
