/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.render;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.FrameInfo;
import dan200.computercraft.client.gui.FixedWidthFontRenderer;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.core.terminal.TextBuffer;
import dan200.computercraft.shared.computer.core.ClientComputer;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import dan200.computercraft.shared.util.Palette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.RenderSpecificHandEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import static dan200.computercraft.client.gui.FixedWidthFontRenderer.FONT_HEIGHT;
import static dan200.computercraft.client.gui.FixedWidthFontRenderer.FONT_WIDTH;

/**
 * Emulates map rendering for pocket computers
 */
@SideOnly( Side.CLIENT )
public class ItemPocketRenderer extends ItemMapLikeRenderer
{
    @SubscribeEvent
    public void renderItem( RenderSpecificHandEvent event )
    {
        ItemStack stack = event.getItemStack();
        if( !(stack.getItem() instanceof ItemPocketComputer) ) return;

        event.setCanceled( true );
        renderItemFirstPerson( event.getHand(), event.getInterpolatedPitch(), event.getEquipProgress(), event.getSwingProgress(), event.getItemStack() );
    }

    @Override
    protected void renderItem( ItemStack stack )
    {
        // Setup various transformations. Note that these are partially adapated from the corresponding method
        // in ItemRenderer
        GlStateManager.disableLighting();

        GlStateManager.rotate( 180f, 0f, 1f, 0f );
        GlStateManager.rotate( 180f, 0f, 0f, 1f );
        GlStateManager.scale( 0.5, 0.5, 0.5 );

        ItemPocketComputer pocketComputer = ComputerCraft.Items.pocketComputer;
        ClientComputer computer = pocketComputer.createClientComputer( stack );

        {
            // First render the background item. We use the item's model rather than a direct texture as this ensures
            // we display the pocket light and other such decorations.
            GlStateManager.pushMatrix();

            GlStateManager.scale( 1.0f, -1.0f, 1.0f );

            Minecraft minecraft = Minecraft.getMinecraft();
            TextureManager textureManager = minecraft.getTextureManager();
            RenderItem renderItem = minecraft.getRenderItem();

            // Copy of RenderItem#renderItemModelIntoGUI but without the translation or scaling
            textureManager.bindTexture( TextureMap.LOCATION_BLOCKS_TEXTURE );
            textureManager.getTexture( TextureMap.LOCATION_BLOCKS_TEXTURE ).setBlurMipmap( false, false );

            GlStateManager.enableRescaleNormal();
            GlStateManager.enableAlpha();
            GlStateManager.alphaFunc( GL11.GL_GREATER, 0.1F );
            GlStateManager.enableBlend();
            GlStateManager.blendFunc( GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA );
            GlStateManager.color( 1.0F, 1.0F, 1.0F, 1.0F );

            IBakedModel bakedmodel = renderItem.getItemModelWithOverrides( stack, null, null );
            bakedmodel = ForgeHooksClient.handleCameraTransforms( bakedmodel, ItemCameraTransforms.TransformType.GUI, false );
            renderItem.renderItem( stack, bakedmodel );

            GlStateManager.disableAlpha();
            GlStateManager.disableRescaleNormal();

            GlStateManager.popMatrix();
        }

        // If we've a computer and terminal then attempt to render it.
        if( computer != null )
        {
            Terminal terminal = computer.getTerminal();
            if( terminal != null )
            {
                synchronized( terminal )
                {
                    GlStateManager.pushMatrix();
                    GlStateManager.disableDepth();

                    // Reset the position to be at the top left corner of the pocket computer
                    // Note we translate towards the screen slightly too.
                    GlStateManager.translate( -8 / 16.0, -8 / 16.0, 0.5 / 16.0 );
                    // Translate to the top left of the screen.
                    GlStateManager.translate( 4 / 16.0, 3 / 16.0, 0 );

                    // Work out the scaling required to resize the terminal in order to fit on the computer
                    final int margin = 2;
                    int tw = terminal.getWidth();
                    int th = terminal.getHeight();
                    int width = tw * FONT_WIDTH + margin * 2;
                    int height = th * FONT_HEIGHT + margin * 2;
                    int max = Math.max( height, width );

                    // The grid is 8 * 8 wide, so we start with a base of 1/2 (8 / 16).
                    double scale = 1.0 / 2.0 / max;
                    GlStateManager.scale( scale, scale, scale );

                    // The margin/start positions are determined in order for the terminal to be centred.
                    int startX = (max - width) / 2 + margin;
                    int startY = (max - height) / 2 + margin;

                    FixedWidthFontRenderer fontRenderer = FixedWidthFontRenderer.instance();
                    boolean greyscale = !computer.isColour();
                    Palette palette = terminal.getPalette();

                    // Render the actual text
                    for( int line = 0; line < th; line++ )
                    {
                        TextBuffer text = terminal.getLine( line );
                        TextBuffer colour = terminal.getTextColourLine( line );
                        TextBuffer backgroundColour = terminal.getBackgroundColourLine( line );
                        fontRenderer.drawString(
                            text, startX, startY + line * FONT_HEIGHT,
                            colour, backgroundColour, margin, margin, greyscale, palette
                        );
                    }

                    // And render the cursor;
                    int tx = terminal.getCursorX(), ty = terminal.getCursorY();
                    if( terminal.getCursorBlink() && FrameInfo.instance().getGlobalCursorBlink() &&
                        tx >= 0 && ty >= 0 && tx < tw && ty < th )
                    {
                        TextBuffer cursorColour = new TextBuffer( "0123456789abcdef".charAt( terminal.getTextColour() ), 1 );
                        fontRenderer.drawString(
                            new TextBuffer( '_', 1 ), startX + FONT_WIDTH * tx, startY + FONT_HEIGHT * ty,
                            cursorColour, null, 0, 0, greyscale, palette
                        );
                    }

                    GlStateManager.enableDepth();
                    GlStateManager.popMatrix();
                }
            }
        }

        GlStateManager.enableLighting();
    }
}
