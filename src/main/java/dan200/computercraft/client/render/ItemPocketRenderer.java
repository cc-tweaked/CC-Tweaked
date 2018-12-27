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
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.math.MathHelper;
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
public class ItemPocketRenderer
{
    @SubscribeEvent
    public void renderItem( RenderSpecificHandEvent event )
    {
        ItemStack stack = event.getItemStack();
        if( !(stack.getItem() instanceof ItemPocketComputer) ) return;

        event.setCanceled( true );

        EntityPlayer player = Minecraft.getMinecraft().player;

        GlStateManager.pushMatrix();
        if( event.getHand() == EnumHand.MAIN_HAND && player.getHeldItemOffhand().isEmpty() )
        {
            renderItemFirstCentre(
                event.getInterpolatedPitch(),
                event.getEquipProgress(),
                event.getSwingProgress(),
                stack
            );
        }
        else
        {
            renderItemFirstPersonSide(
                event.getHand() == EnumHand.MAIN_HAND ? player.getPrimaryHand() : player.getPrimaryHand().opposite(),
                event.getEquipProgress(),
                event.getSwingProgress(),
                stack
            );
        }
        GlStateManager.popMatrix();
    }

    /**
     * The main rendering method for pocket computers and their associated terminal
     *
     * @param stack The stack to render
     * @see ItemRenderer#renderMapFirstPerson(ItemStack)
     */
    private void renderPocketComputerItem( ItemStack stack )
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
                    for( int line = 0; line < th; ++line )
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

    /**
     * Renders a pocket computer to one side of the player.
     *
     * @param side          The side to render on
     * @param equipProgress The equip progress of this item
     * @param swingProgress The swing progress of this item
     * @param stack         The stack to render
     * @see ItemRenderer#renderMapFirstPersonSide(float, EnumHandSide, float, ItemStack)
     */
    private void renderItemFirstPersonSide( EnumHandSide side, float equipProgress, float swingProgress, ItemStack stack )
    {
        Minecraft minecraft = Minecraft.getMinecraft();
        float offset = side == EnumHandSide.RIGHT ? 1f : -1f;
        GlStateManager.translate( offset * 0.125f, -0.125f, 0f );

        // If the player is not invisible then render a single arm
        if( !minecraft.player.isInvisible() )
        {
            GlStateManager.pushMatrix();
            GlStateManager.rotate( offset * 10f, 0f, 0f, 1f );
            minecraft.getItemRenderer().renderArmFirstPerson( equipProgress, swingProgress, side );
            GlStateManager.popMatrix();
        }

        // Setup the appropriate transformations. This is just copied from the
        // corresponding method in ItemRenderer.
        GlStateManager.pushMatrix();
        GlStateManager.translate( offset * 0.51f, -0.08f + equipProgress * -1.2f, -0.75f );
        float f1 = MathHelper.sqrt( swingProgress );
        float f2 = MathHelper.sin( f1 * (float) Math.PI );
        float f3 = -0.5f * f2;
        float f4 = 0.4f * MathHelper.sin( f1 * ((float) Math.PI * 2f) );
        float f5 = -0.3f * MathHelper.sin( swingProgress * (float) Math.PI );
        GlStateManager.translate( offset * f3, f4 - 0.3f * f2, f5 );
        GlStateManager.rotate( f2 * -45f, 1f, 0f, 0f );
        GlStateManager.rotate( offset * f2 * -30f, 0f, 1f, 0f );

        renderPocketComputerItem( stack );

        GlStateManager.popMatrix();
    }

    /**
     * Render an item in the middle of the screen
     *
     * @param pitch         The pitch of the player
     * @param equipProgress The equip progress of this item
     * @param swingProgress The swing progress of this item
     * @param stack         The stack to render
     * @see ItemRenderer#renderMapFirstPerson(float, float, float)
     */
    private void renderItemFirstCentre( float pitch, float equipProgress, float swingProgress, ItemStack stack )
    {
        ItemRenderer itemRenderer = Minecraft.getMinecraft().getItemRenderer();

        // Setup the appropriate transformations. This is just copied from the
        // corresponding method in ItemRenderer.
        float swingRt = MathHelper.sqrt( swingProgress );
        float tX = -0.2f * MathHelper.sin( swingProgress * (float) Math.PI );
        float tZ = -0.4f * MathHelper.sin( swingRt * (float) Math.PI );
        GlStateManager.translate( 0f, -tX / 2f, tZ );
        float pitchAngle = itemRenderer.getMapAngleFromPitch( pitch );
        GlStateManager.translate( 0f, 0.04f + equipProgress * -1.2f + pitchAngle * -0.5f, -0.72f );
        GlStateManager.rotate( pitchAngle * -85f, 1f, 0f, 0f );
        itemRenderer.renderArms();
        float rX = MathHelper.sin( swingRt * (float) Math.PI );
        GlStateManager.rotate( rX * 20f, 1f, 0f, 0f );
        GlStateManager.scale( 2f, 2f, 2f );

        renderPocketComputerItem( stack );
    }
}
