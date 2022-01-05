/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import dan200.computercraft.fabric.mixin.HeldItemRendererAccess;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;

@Environment( EnvType.CLIENT )
public abstract class ItemMapLikeRenderer
{
    public void renderItemFirstPerson(
        MatrixStack transform, VertexConsumerProvider render, int lightTexture, Hand hand, float pitch, float equipProgress,
        float swingProgress, ItemStack stack
    )
    {
        PlayerEntity player = MinecraftClient.getInstance().player;

        transform.push();
        if( hand == Hand.MAIN_HAND && player.getOffHandStack().isEmpty() )
        {
            renderItemFirstPersonCenter( transform, render, lightTexture, pitch, equipProgress, swingProgress, stack );
        }
        else
        {
            renderItemFirstPersonSide( transform,
                render,
                lightTexture,
                hand == Hand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite(),
                equipProgress,
                swingProgress,
                stack );
        }
        transform.pop();
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
     */
    private void renderItemFirstPersonCenter( MatrixStack transform, VertexConsumerProvider render, int combinedLight, float pitch, float equipProgress,
                                              float swingProgress, ItemStack stack )
    {
        MinecraftClient minecraft = MinecraftClient.getInstance();
        HeldItemRenderer renderer = minecraft.getHeldItemRenderer();

        // Setup the appropriate transformations. This is just copied from the
        // corresponding method in ItemRenderer.
        float swingRt = MathHelper.sqrt( swingProgress );
        float tX = -0.2f * MathHelper.sin( swingProgress * (float) Math.PI );
        float tZ = -0.4f * MathHelper.sin( swingRt * (float) Math.PI );
        transform.translate( 0, -tX / 2, tZ );

        HeldItemRendererAccess access = (HeldItemRendererAccess) renderer;
        float pitchAngle = access.callGetMapAngle( pitch );
        transform.translate( 0, 0.04F + equipProgress * -1.2f + pitchAngle * -0.5f, -0.72f );
        transform.multiply( Vector3f.POSITIVE_X.getDegreesQuaternion( pitchAngle * -85.0f ) );
        if( !minecraft.player.isInvisible() )
        {
            transform.push();
            transform.multiply( Vector3f.POSITIVE_Y.getDegreesQuaternion( 90.0F ) );
            access.callRenderArm( transform, render, combinedLight, Arm.RIGHT );
            access.callRenderArm( transform, render, combinedLight, Arm.LEFT );
            transform.pop();
        }

        float rX = MathHelper.sin( swingRt * (float) Math.PI );
        transform.multiply( Vector3f.POSITIVE_X.getDegreesQuaternion( rX * 20.0F ) );
        transform.scale( 2.0F, 2.0F, 2.0F );

        renderItem( transform, render, stack );
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
     */
    private void renderItemFirstPersonSide( MatrixStack transform, VertexConsumerProvider render, int combinedLight, Arm side, float equipProgress,
                                            float swingProgress, ItemStack stack )
    {
        MinecraftClient minecraft = MinecraftClient.getInstance();
        float offset = side == Arm.RIGHT ? 1f : -1f;
        transform.translate( offset * 0.125f, -0.125f, 0f );

        // If the player is not invisible then render a single arm
        if( !minecraft.player.isInvisible() )
        {
            transform.push();
            transform.multiply( Vector3f.POSITIVE_Z.getDegreesQuaternion( offset * 10f ) );
            ((HeldItemRendererAccess) minecraft.getHeldItemRenderer())
                .callRenderArmHoldingItem( transform, render, combinedLight, equipProgress, swingProgress, side );
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
        transform.multiply( Vector3f.POSITIVE_X.getDegreesQuaternion( f2 * -45f ) );
        transform.multiply( Vector3f.POSITIVE_Y.getDegreesQuaternion( offset * f2 * -30f ) );

        renderItem( transform, render, stack );

        transform.pop();
    }

    /**
     * The main rendering method for the item.
     *
     * @param transform The matrix transformation stack
     * @param render    The buffer to render to
     * @param stack     The stack to render
     */
    protected abstract void renderItem( MatrixStack transform, VertexConsumerProvider render, ItemStack stack );
}
