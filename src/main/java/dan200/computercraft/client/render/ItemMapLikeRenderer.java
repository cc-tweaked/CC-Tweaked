/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2018. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.math.MathHelper;

public abstract class ItemMapLikeRenderer
{
    /**
     * The main rendering method for the item
     *
     * @param stack The stack to render
     * @see ItemRenderer#renderMapFirstPerson(ItemStack)
     */
    protected abstract void renderItem( ItemStack stack );

    protected void renderItemFirstPerson( EnumHand hand, float pitch, float equipProgress, float swingProgress, ItemStack stack )
    {
        EntityPlayer player = Minecraft.getMinecraft().player;

        GlStateManager.pushMatrix();
        if( hand == EnumHand.MAIN_HAND && player.getHeldItemOffhand().isEmpty() )
        {
            renderItemFirstCentre( pitch, equipProgress, swingProgress, stack );
        }
        else
        {
            renderItemFirstPersonSide(
                hand == EnumHand.MAIN_HAND ? player.getPrimaryHand() : player.getPrimaryHand().opposite(),
                equipProgress, swingProgress, stack
            );
        }
        GlStateManager.popMatrix();
    }

    /**
     * Renders the item to one side of the player.
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

        renderItem( stack );

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

        renderItem( stack );
    }
}
