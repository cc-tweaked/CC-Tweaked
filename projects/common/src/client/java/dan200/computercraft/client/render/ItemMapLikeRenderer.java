// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/**
 * A base class for items which have map-like rendering when held in the hand.
 *
 * @see dan200.computercraft.client.ClientHooks#onRenderHeldItem(PoseStack, MultiBufferSource, int, InteractionHand, float, float, float, ItemStack)
 */
public abstract class ItemMapLikeRenderer {
    /**
     * The main rendering method for the item.
     *
     * @param transform The matrix transformation stack
     * @param render    The buffer to render to
     * @param stack     The stack to render
     * @param light     The packed lightmap coordinates.
     * @see ItemInHandRenderer#renderItem(LivingEntity, ItemStack, ItemDisplayContext, boolean, PoseStack, MultiBufferSource, int)
     */
    protected abstract void renderItem(PoseStack transform, MultiBufferSource render, ItemStack stack, int light);

    public void renderItemFirstPerson(PoseStack transform, MultiBufferSource render, int lightTexture, InteractionHand hand, float pitch, float equipProgress, float swingProgress, ItemStack stack) {
        Player player = Objects.requireNonNull(Minecraft.getInstance().player);

        transform.pushPose();
        if (hand == InteractionHand.MAIN_HAND && player.getOffhandItem().isEmpty()) {
            renderItemFirstPersonCenter(transform, render, lightTexture, pitch, equipProgress, swingProgress, stack);
        } else {
            renderItemFirstPersonSide(
                transform, render, lightTexture,
                hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite(),
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
     * @see ItemInHandRenderer#renderOneHandedMap(PoseStack, MultiBufferSource, int, float, HumanoidArm, float, ItemStack)
     */
    private void renderItemFirstPersonSide(PoseStack transform, MultiBufferSource render, int combinedLight, HumanoidArm side, float equipProgress, float swingProgress, ItemStack stack) {
        var minecraft = Minecraft.getInstance();
        var offset = side == HumanoidArm.RIGHT ? 1f : -1f;
        transform.translate(offset * 0.125f, -0.125f, 0f);

        // If the player is not invisible then render a single arm
        if (!minecraft.player.isInvisible()) {
            transform.pushPose();
            transform.mulPose(Axis.ZP.rotationDegrees(offset * 10f));
            minecraft.getEntityRenderDispatcher().getItemInHandRenderer().renderPlayerArm(transform, render, combinedLight, equipProgress, swingProgress, side);
            transform.popPose();
        }

        // Setup the appropriate transformations. This is just copied from the
        // corresponding method in ItemRenderer.
        transform.pushPose();
        transform.translate(offset * 0.51f, -0.08f + equipProgress * -1.2f, -0.75f);
        var f1 = Mth.sqrt(swingProgress);
        var f2 = Mth.sin(f1 * (float) Math.PI);
        var f3 = -0.5f * f2;
        var f4 = 0.4f * Mth.sin(f1 * ((float) Math.PI * 2f));
        var f5 = -0.3f * Mth.sin(swingProgress * (float) Math.PI);
        transform.translate(offset * f3, f4 - 0.3f * f2, f5);
        transform.mulPose(Axis.XP.rotationDegrees(f2 * -45f));
        transform.mulPose(Axis.YP.rotationDegrees(offset * f2 * -30f));

        renderItem(transform, render, stack, combinedLight);

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
     * @see ItemInHandRenderer#renderTwoHandedMap(PoseStack, MultiBufferSource, int, float, float, float)
     */
    private void renderItemFirstPersonCenter(PoseStack transform, MultiBufferSource render, int combinedLight, float pitch, float equipProgress, float swingProgress, ItemStack stack) {
        var minecraft = Minecraft.getInstance();
        var renderer = minecraft.getEntityRenderDispatcher().getItemInHandRenderer();

        // Setup the appropriate transformations. This is just copied from the
        // corresponding method in ItemRenderer.
        var swingRt = Mth.sqrt(swingProgress);
        var tX = -0.2f * Mth.sin(swingProgress * (float) Math.PI);
        var tZ = -0.4f * Mth.sin(swingRt * (float) Math.PI);
        transform.translate(0, -tX / 2, tZ);

        var pitchAngle = renderer.calculateMapTilt(pitch);
        transform.translate(0, 0.04F + equipProgress * -1.2f + pitchAngle * -0.5f, -0.72f);
        transform.mulPose(Axis.XP.rotationDegrees(pitchAngle * -85.0f));
        if (!minecraft.player.isInvisible()) {
            transform.pushPose();
            transform.mulPose(Axis.YP.rotationDegrees(90.0F));
            renderer.renderMapHand(transform, render, combinedLight, HumanoidArm.RIGHT);
            renderer.renderMapHand(transform, render, combinedLight, HumanoidArm.LEFT);
            transform.popPose();
        }

        var rX = Mth.sin(swingRt * (float) Math.PI);
        transform.mulPose(Axis.XP.rotationDegrees(rX * 20.0F));
        transform.scale(2.0F, 2.0F, 2.0F);

        renderItem(transform, render, stack, combinedLight);
    }
}
