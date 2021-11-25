/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.FirstPersonRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.HandSide;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3f;

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

        transform.pushPose();
        if( hand == Hand.MAIN_HAND && player.getOffhandItem().isEmpty() )
        {
            renderItemFirstPersonCenter( transform, render, lightTexture, pitch, equipProgress, swingProgress, stack );
        }
        else
        {
            renderItemFirstPersonSide(
                transform, render, lightTexture,
                hand == Hand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite(),
                equipProgress, swingProgress, stack
            );
        }
        transform.popPose();
    }

    /**
     * Renders the item to one side of the player.
     *
     * @param transform     The matrix transformation stack
     * @param render        The buffer to render to
     * @param combinedLight The current light level
     * @param side          The side to render on
     * @param equipProgress The equip progress of this item
     * @param swingProgress The swing progress of this item
     * @param stack         The stack to render
     * @see FirstPersonRenderer#renderMapFirstPersonSide(MatrixStack, IRenderTypeBuffer, int, float, HandSide, float, ItemStack)
     */
    private void renderItemFirstPersonSide( MatrixStack transform, IRenderTypeBuffer render, int combinedLight, HandSide side, float equipProgress, float swingProgress, ItemStack stack )
    {
        Minecraft minecraft = Minecraft.getInstance();
        float offset = side == HandSide.RIGHT ? 1f : -1f;
        transform.translate( offset * 0.125f, -0.125f, 0f );

        // If the player is not invisible then render a single arm
        if( !minecraft.player.isInvisible() )
        {
            transform.pushPose();
            transform.mulPose( Vector3f.ZP.rotationDegrees( offset * 10f ) );
            minecraft.getItemInHandRenderer().renderPlayerArm( transform, render, combinedLight, equipProgress, swingProgress, side );
            transform.popPose();
        }

        // Setup the appropriate transformations. This is just copied from the
        // corresponding method in ItemRenderer.
        transform.pushPose();
        transform.translate( offset * 0.51f, -0.08f + equipProgress * -1.2f, -0.75f );
        float f1 = MathHelper.sqrt( swingProgress );
        float f2 = MathHelper.sin( f1 * (float) Math.PI );
        float f3 = -0.5f * f2;
        float f4 = 0.4f * MathHelper.sin( f1 * ((float) Math.PI * 2f) );
        float f5 = -0.3f * MathHelper.sin( swingProgress * (float) Math.PI );
        transform.translate( offset * f3, f4 - 0.3f * f2, f5 );
        transform.mulPose( Vector3f.XP.rotationDegrees( f2 * -45f ) );
        transform.mulPose( Vector3f.YP.rotationDegrees( offset * f2 * -30f ) );

        renderItem( transform, render, stack );

        transform.popPose();
    }

    /**
     * Render an item in the middle of the screen.
     *
     * @param transform     The matrix transformation stack
     * @param render        The buffer to render to
     * @param combinedLight The current light level
     * @param pitch         The pitch of the player
     * @param equipProgress The equip progress of this item
     * @param swingProgress The swing progress of this item
     * @param stack         The stack to render
     * @see FirstPersonRenderer#renderMapFirstPerson(MatrixStack, IRenderTypeBuffer, int, float, float, float)
     */
    private void renderItemFirstPersonCenter( MatrixStack transform, IRenderTypeBuffer render, int combinedLight, float pitch, float equipProgress, float swingProgress, ItemStack stack )
    {
        Minecraft minecraft = Minecraft.getInstance();
        FirstPersonRenderer renderer = minecraft.getItemInHandRenderer();

        // Setup the appropriate transformations. This is just copied from the
        // corresponding method in ItemRenderer.
        float swingRt = MathHelper.sqrt( swingProgress );
        float tX = -0.2f * MathHelper.sin( swingProgress * (float) Math.PI );
        float tZ = -0.4f * MathHelper.sin( swingRt * (float) Math.PI );
        transform.translate( 0, -tX / 2, tZ );

        float pitchAngle = renderer.calculateMapTilt( pitch );
        transform.translate( 0, 0.04F + equipProgress * -1.2f + pitchAngle * -0.5f, -0.72f );
        transform.mulPose( Vector3f.XP.rotationDegrees( pitchAngle * -85.0f ) );
        if( !minecraft.player.isInvisible() )
        {
            transform.pushPose();
            transform.mulPose( Vector3f.YP.rotationDegrees( 90.0F ) );
            renderer.renderMapHand( transform, render, combinedLight, HandSide.RIGHT );
            renderer.renderMapHand( transform, render, combinedLight, HandSide.LEFT );
            transform.popPose();
        }

        float rX = MathHelper.sin( swingRt * (float) Math.PI );
        transform.mulPose( Vector3f.XP.rotationDegrees( rX * 20.0F ) );
        transform.scale( 2.0F, 2.0F, 2.0F );

        renderItem( transform, render, stack );
    }
}
