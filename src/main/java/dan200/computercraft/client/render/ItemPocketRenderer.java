/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.gui.FixedWidthFontRenderer;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.computer.core.ClientComputer;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import dan200.computercraft.shared.util.Colour;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
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
    protected void renderItem( MatrixStack transform, IRenderTypeBuffer render, ItemStack stack )
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
        transform.push();
        transform.rotate( Vector3f.YP.rotationDegrees( 180f ) );
        transform.rotate( Vector3f.ZP.rotationDegrees( 180f ) );
        transform.scale( 0.5f, 0.5f, 0.5f );

        float scale = 0.75f / Math.max( width + FRAME * 2, height + FRAME * 2 + LIGHT_HEIGHT );
        transform.scale( scale, scale, 0 );
        transform.translate( -0.5 * width, -0.5 * height, 0 );

        // Render the main frame
        ItemPocketComputer item = (ItemPocketComputer) stack.getItem();
        ComputerFamily family = item.getFamily();
        int frameColour = item.getColour( stack );

        Matrix4f matrix = transform.getLast().getMatrix();
        renderFrame( matrix, family, frameColour, width, height );

        // Render the light
        int lightColour = ItemPocketComputer.getLightState( stack );
        if( lightColour == -1 ) lightColour = Colour.BLACK.getHex();
        renderLight( matrix, lightColour, width, height );

        if( computer != null && terminal != null )
        {
            FixedWidthFontRenderer.drawTerminal( matrix, MARGIN, MARGIN, terminal, !computer.isColour(), MARGIN, MARGIN, MARGIN, MARGIN );
        }
        else
        {
            FixedWidthFontRenderer.drawEmptyTerminal( matrix, 0, 0, width, height );
        }

        transform.pop();
    }

    private static void renderFrame( Matrix4f transform, ComputerFamily family, int colour, int width, int height )
    {
        Minecraft.getInstance().getTextureManager().bindTexture( colour != -1
            ? BACKGROUND_COLOUR
            : family == ComputerFamily.NORMAL ? BACKGROUND_NORMAL : BACKGROUND_ADVANCED
        );

        float r = ((colour >>> 16) & 0xFF) / 255.0f;
        float g = ((colour >>> 8) & 0xFF) / 255.0f;
        float b = (colour & 0xFF) / 255.0f;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin( GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX );

        // Top left, middle, right
        renderTexture( transform, buffer, -FRAME, -FRAME, 12, 28, FRAME, FRAME, r, g, b );
        renderTexture( transform, buffer, 0, -FRAME, 0, 0, width, FRAME, r, g, b );
        renderTexture( transform, buffer, width, -FRAME, 24, 28, FRAME, FRAME, r, g, b );

        // Left and bright border
        renderTexture( transform, buffer, -FRAME, 0, 0, 28, FRAME, height, r, g, b );
        renderTexture( transform, buffer, width, 0, 36, 28, FRAME, height, r, g, b );

        // Bottom left, middle, right. We do this in three portions: the top inner corners, an extended region for
        // lights, and then the bottom outer corners.
        renderTexture( transform, buffer, -FRAME, height, 12, 40, FRAME, FRAME / 2, r, g, b );
        renderTexture( transform, buffer, 0, height, 0, 12, width, FRAME / 2, r, g, b );
        renderTexture( transform, buffer, width, height, 24, 40, FRAME, FRAME / 2, r, g, b );

        renderTexture( transform, buffer, -FRAME, height + FRAME / 2, 12, 44, FRAME, LIGHT_HEIGHT, FRAME, 4, r, g, b );
        renderTexture( transform, buffer, 0, height + FRAME / 2, 0, 16, width, LIGHT_HEIGHT, FRAME, 4, r, g, b );
        renderTexture( transform, buffer, width, height + FRAME / 2, 24, 44, FRAME, LIGHT_HEIGHT, FRAME, 4, r, g, b );

        renderTexture( transform, buffer, -FRAME, height + LIGHT_HEIGHT + FRAME / 2, 12, 40 + FRAME / 2, FRAME, FRAME / 2, r, g, b );
        renderTexture( transform, buffer, 0, height + LIGHT_HEIGHT + FRAME / 2, 0, 12 + FRAME / 2, width, FRAME / 2, r, g, b );
        renderTexture( transform, buffer, width, height + LIGHT_HEIGHT + FRAME / 2, 24, 40 + FRAME / 2, FRAME, FRAME / 2, r, g, b );

        tessellator.draw();
    }

    private static void renderLight( Matrix4f transform, int colour, int width, int height )
    {
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();

        float r = ((colour >>> 16) & 0xFF) / 255.0f;
        float g = ((colour >>> 8) & 0xFF) / 255.0f;
        float b = (colour & 0xFF) / 255.0f;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin( GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR );
        buffer.pos( transform, width - LIGHT_HEIGHT * 2, height + LIGHT_HEIGHT + FRAME / 2.0f, 0 ).color( r, g, b, 1.0f ).endVertex();
        buffer.pos( transform, width, height + LIGHT_HEIGHT + FRAME / 2.0f, 0 ).color( r, g, b, 1.0f ).endVertex();
        buffer.pos( transform, width, height + FRAME / 2.0f, 0 ).color( r, g, b, 1.0f ).endVertex();
        buffer.pos( transform, width - LIGHT_HEIGHT * 2, height + FRAME / 2.0f, 0 ).color( r, g, b, 1.0f ).endVertex();

        tessellator.draw();
        RenderSystem.enableTexture();
    }

    private static void renderTexture( Matrix4f transform, IVertexBuilder builder, int x, int y, int textureX, int textureY, int width, int height, float r, float g, float b )
    {
        renderTexture( transform, builder, x, y, textureX, textureY, width, height, width, height, r, g, b );
    }

    private static void renderTexture( Matrix4f transform, IVertexBuilder builder, int x, int y, int textureX, int textureY, int width, int height, int textureWidth, int textureHeight, float r, float g, float b )
    {
        float scale = 1 / 255.0f;
        builder.pos( transform, x, y + height, 0 ).color( r, g, b, 1.0f ).tex( textureX * scale, (textureY + textureHeight) * scale ).endVertex();
        builder.pos( transform, x + width, y + height, 0 ).color( r, g, b, 1.0f ).tex( (textureX + textureWidth) * scale, (textureY + textureHeight) * scale ).endVertex();
        builder.pos( transform, x + width, y, 0 ).color( r, g, b, 1.0f ).tex( (textureX + textureWidth) * scale, textureY * scale ).endVertex();
        builder.pos( transform, x, y, 0 ).color( r, g, b, 1.0f ).tex( textureX * scale, textureY * scale ).endVertex();
    }
}
