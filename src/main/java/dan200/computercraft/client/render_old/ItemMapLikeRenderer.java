/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.FirstPersonRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.Vector3f;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.HandSide;
import net.minecraft.util.math.MathHelper;

public abstract class ItemMapLikeRenderer
{
    /**
     * The main rendering method for the item.
     *
     * @param transform The matrix transformation stack
     * @param render    The buffer to render to
     * @param stack     The stack to render
     * @see FirstPersonRenderer#renderItemInFirstPerson(AbstractClientPlayerEntity, float, float, Hand, float, ItemStack, float, MatrixStack, IRenderTypeBuffer, int)
     */
    protected abstract void renderItem( MatrixStack transform, IRenderTypeBuffer render, ItemStack stack );

    protected void renderItemFirstPerson( MatrixStack transform, IRenderTypeBuffer render, int lightTexture, Hand hand, float pitch, float equipProgress, float swingProgress, ItemStack stack )
    {
        PlayerEntity player = Minecraft.getInstance().player;

        transform.push();
        if( hand == Hand.MAIN_HAND && player.getHeldItemOffhand().isEmpty() )
        {
            renderItemFirstPersonCenter( transform, render, lightTexture, pitch, equipProgress, swingProgress, stack );
        }
        else
        {
            renderItemFirstPersonSide(
                transform, render, lightTexture,
                hand == Hand.MAIN_HAND ? player.getPrimaryHand() : player.getPrimaryHand().opposite(),
                equipProgress, swingProgress, stack
            );
        }
        transform.pop();
    }

    /**
     * Renders the item to one side of the player.
     *
     * @param side          The side to render on
     * @param equipProgress The equip progress of this item
     * @param swingProgress The swing progress of this item
     * @param stack         The stack to render
     * @see FirstPersonRenderer#renderMapFirstPersonSide(MatrixStack, IRenderTypeBuffer, int, float, HandSide, float, ItemStack)
     */
    private void renderItemFirstPersonSide( MatrixStack transform, IRenderTypeBuffer render, int lightTexture, HandSide side, float equipProgress, float swingProgress, ItemStack stack )
    {
        Minecraft minecraft = Minecraft.getInstance();
        float offset = side == HandSide.RIGHT ? 1f : -1f;
        transform.translate( offset * 0.125f, -0.125f, 0f );

        // If the player is not invisible then render a single arm
        if( !minecraft.player.isInvisible() )
        {
            transform.push();
            transform.rotate( Vector3f.field_229183_f_.func_229187_a_( offset * 10f ) );
            minecraft.getFirstPersonRenderer().renderArmFirstPerson( transform, render, lightTexture, equipProgress, swingProgress, side );
            transform.pop();
        }

        // Setup the appropriate transformations. This is just copied from the
        // corresponding method in ItemRenderer.
        transform.push();
        transform.translate( offset * 0.51f, -0.08f + equipProgress * -1.2f, -0.75f );
        float f1 = MathHelper.sqrt( swingProgress );
        float f2 = MathHelper.sin( f1 * (float) Math.PI );
        float f3 = -0.5f * f2;
        float f4 = 0.4f * MathHelper.sin( f1 * ((float) Math.PI * 2f) );
        float f5 = -0.3f * MathHelper.sin( swingProgress * (float) Math.PI );
        transform.translate( offset * f3, f4 - 0.3f * f2, f5 );
        transform.rotate( Vector3f.field_229179_b_.func_229187_a_( f2 * -45f ) );
        transform.rotate( Vector3f.field_229181_d_.func_229187_a_( offset * f2 * -30f ) );

        renderItem( transform, render, stack );

        transform.pop();
    }

    /**
     * Render an item in the middle of the screen.
     *
     * @param pitch         The pitch of the player
     * @param equipProgress The equip progress of this item
     * @param swingProgress The swing progress of this item
     * @param stack         The stack to render
     * @see FirstPersonRenderer#renderMapFirstPerson(MatrixStack, IRenderTypeBuffer, int, float, float, float)
     */
    private void renderItemFirstPersonCenter( MatrixStack transform, IRenderTypeBuffer render, int lightTexture, float pitch, float equipProgress, float swingProgress, ItemStack stack )
    {
        Minecraft minecraft = Minecraft.getInstance();
        FirstPersonRenderer renderer = minecraft.getFirstPersonRenderer();

        // Setup the appropriate transformations. This is just copied from the
        // corresponding method in ItemRenderer.
        float swingRt = MathHelper.sqrt( swingProgress );
        float tX = -0.2f * MathHelper.sin( swingProgress * (float) Math.PI );
        float tZ = -0.4f * MathHelper.sin( swingRt * (float) Math.PI );
        transform.translate( 0, -tX / 2, tZ );

        float pitchAngle = renderer.getMapAngleFromPitch( pitch );
        transform.translate( 0, 0.04F + equipProgress * -1.2f + pitchAngle * -0.5f, -0.72f );
        transform.rotate( Vector3f.field_229179_b_.func_229187_a_( pitchAngle * -85.0f ) );
        if( !minecraft.player.isInvisible() )
        {
            transform.push();
            transform.rotate( Vector3f.field_229181_d_.func_229187_a_( 90.0F ) );
            renderer.renderArm( transform, render, lightTexture, HandSide.RIGHT );
            renderer.renderArm( transform, render, lightTexture, HandSide.LEFT );
            transform.pop();
        }

        float rX = MathHelper.sin( swingRt * (float) Math.PI );
        transform.rotate( Vector3f.field_229179_b_.func_229187_a_( rX * 20.0F ) );
        transform.scale( 2.0F, 2.0F, 2.0F );

        renderItem( transform, render, stack );
    }
}
