package dan200.computercraft.client.render;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.media.items.ItemPrintout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.RenderItemInFrameEvent;
import net.minecraftforge.client.event.RenderSpecificHandEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import static dan200.computercraft.client.gui.FixedWidthFontRenderer.FONT_HEIGHT;
import static dan200.computercraft.client.gui.FixedWidthFontRenderer.FONT_WIDTH;
import static dan200.computercraft.client.render.PrintoutRenderer.*;
import static dan200.computercraft.shared.media.items.ItemPrintout.LINES_PER_PAGE;
import static dan200.computercraft.shared.media.items.ItemPrintout.LINE_MAX_LENGTH;

public class ItemPrintoutRenderer
{
    @SubscribeEvent
    public void onRenderInHand( RenderSpecificHandEvent event )
    {
        ItemStack stack = event.getItemStack();
        if( stack.getItem() != ComputerCraft.Items.printout ) return;

        event.setCanceled( true );

        EntityPlayer player = Minecraft.getMinecraft().player;

        GlStateManager.pushMatrix();
        if( event.getHand() == EnumHand.MAIN_HAND && player.getHeldItemOffhand().isEmpty() )
        {
            renderPrintoutFirstPersonCentre(
                event.getInterpolatedPitch(),
                event.getEquipProgress(),
                event.getSwingProgress(),
                stack
            );
        }
        else
        {
            renderPrintoutFirstPersonSide(
                event.getHand() == EnumHand.MAIN_HAND ? player.getPrimaryHand() : player.getPrimaryHand().opposite(),
                event.getEquipProgress(),
                event.getSwingProgress(),
                stack
            );
        }
        GlStateManager.popMatrix();
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
    private void renderPrintoutFirstPersonSide( EnumHandSide side, float equipProgress, float swingProgress, ItemStack stack )
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

        renderPrintoutFirstPerson( stack );

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
    private void renderPrintoutFirstPersonCentre( float pitch, float equipProgress, float swingProgress, ItemStack stack )
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

        renderPrintoutFirstPerson( stack );
    }


    private static void renderPrintoutFirstPerson( ItemStack stack )
    {
        // Setup various transformations. Note that these are partially adapated from the corresponding method
        // in ItemRenderer.renderMapFirstPerson
        GlStateManager.disableLighting();

        GlStateManager.rotate( 180f, 0f, 1f, 0f );
        GlStateManager.rotate( 180f, 0f, 0f, 1f );
        GlStateManager.scale( 0.42f, 0.42f, -0.42f );
        GlStateManager.translate( -0.5f, -0.48f, 0.0f );

        drawPrintout( stack );

        GlStateManager.enableLighting();
    }

    @SubscribeEvent
    public void onRenderInFrame( RenderItemInFrameEvent event )
    {
        ItemStack stack = event.getItem();
        if( stack.getItem() != ComputerCraft.Items.printout ) return;

        event.setCanceled( true );

        GlStateManager.disableLighting();

        // Move a little bit forward to ensure we're not clipping with the frame
        GlStateManager.translate( 0.0f, 0.0f, -0.001f );
        GlStateManager.rotate( 180f, 0f, 0f, 1f );
        GlStateManager.scale( 0.95f, 0.95f, -0.95f );
        GlStateManager.translate( -0.5f, -0.5f, 0.0f );

        drawPrintout( stack );

        GlStateManager.enableLighting();
    }

    private static void drawPrintout( ItemStack stack )
    {
        int pages = ItemPrintout.getPageCount( stack );
        boolean book = ItemPrintout.getType( stack ) == ItemPrintout.Type.Book;

        double width = LINE_MAX_LENGTH * FONT_WIDTH + X_TEXT_MARGIN * 2;
        double height = LINES_PER_PAGE * FONT_HEIGHT + Y_TEXT_MARGIN * 2;

        // Non-books will be left aligned
        if( !book ) width += offsetAt( pages );

        double visualWidth = width, visualHeight = height;

        // Meanwhile books will be centred
        if( book )
        {
            visualWidth += 2 * COVER_SIZE + 2 * offsetAt( pages );
            visualHeight += 2 * COVER_SIZE;
        }

        double max = Math.max( visualHeight, visualWidth );

        // Scale the printout to fit correctly.
        double scale = 1.0 / max;
        GlStateManager.scale( scale, scale, scale );
        GlStateManager.translate( (max - width) / 2.0f, (max - height) / 2.0f, 0.0f );

        drawBorder( 0, 0, -0.01, 0, pages, book );
        drawText( X_TEXT_MARGIN, Y_TEXT_MARGIN, 0, ItemPrintout.getText( stack ), ItemPrintout.getColours( stack ) );
    }
}
