// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * A base class for items which have map-like rendering when held in the hand.
 *
 * @see dan200.computercraft.client.ClientHooks#onRenderHeldItem(PoseStack, SubmitNodeCollector, int, InteractionHand, float, float, float, ItemStack)
 */
public abstract class ItemMapLikeRenderer {
    /**
     * The main rendering method for the item.
     *
     * @param transform The matrix transformation stack
     * @param collector The buffer to render to
     * @param stack     The stack to render
     * @param light     The packed lightmap coordinates.
     * @see FirstPersonHandsAndItemsRenderer#renderItem(LivingEntity, ItemStack, ItemDisplayContext, PoseStack, SubmitNodeCollector, int)
     */
    protected abstract void renderItem(PoseStack transform, SubmitNodeCollector collector, ItemStack stack, int light);

    public void renderItemFirstPerson(
        FirstPersonHandsAndItemsRenderer renderer, PlayerRenderState player, PoseStack transform, SubmitNodeCollector collector,
        int lightTexture, InteractionHand hand, float pitch, float equipProgress, float swingProgress, ItemStack stack
    ) {
        var avatar = player.avatarRenderState;
        if (avatar == null) return;

        transform.pushPose();
        if (hand == InteractionHand.MAIN_HAND && player.firstPersonHandsAndItems.offHandItem.isEmpty()) {
            renderItemFirstPersonCenter(renderer, player, transform, collector, lightTexture, pitch, equipProgress, swingProgress, stack);
        } else {
            renderItemFirstPersonSide(
                renderer, player, transform, collector, lightTexture,
                hand == InteractionHand.MAIN_HAND ? avatar.mainArm : avatar.mainArm.getOpposite(),
                equipProgress, swingProgress, stack
            );
        }
        transform.popPose();
    }

    /**
     * Renders the item to one side of the player.
     *
     * @param renderer      The {@link FirstPersonHandsAndItemsRenderer} instance.
     * @param player        The render state of the player holding this item.
     * @param transform     The matrix transformation stack
     * @param collector     The buffer to render to
     * @param combinedLight The current light level
     * @param side          The side to render on
     * @param equipProgress The equip progress of this item
     * @param swingProgress The swing progress of this item
     * @param stack         The stack to render
     * @see FirstPersonHandsAndItemsRenderer#renderOneHandedMap(PoseStack, SubmitNodeCollector, int, float, HumanoidArm, float, ItemStack, PlayerRenderState, FirstPersonHandsAndItemsRenderState)
     */
    private void renderItemFirstPersonSide(
        FirstPersonHandsAndItemsRenderer renderer, PlayerRenderState player, PoseStack transform, SubmitNodeCollector collector, int combinedLight,
        HumanoidArm side, float equipProgress, float swingProgress, ItemStack stack
    ) {
        var offset = side == HumanoidArm.RIGHT ? 1f : -1f;
        transform.translate(offset * 0.125f, -0.125f, 0f);

        // If the player is not invisible then render a single arm
        if (player.avatarRenderState != null && !player.avatarRenderState.isInvisible) {
            transform.pushPose();
            transform.rotateDegrees(Axis.ZP, offset * 10f);
            renderer.renderPlayerArm(transform, collector, combinedLight, equipProgress, swingProgress, side, player);
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
        transform.rotateDegrees(Axis.XP, f2 * -45f);
        transform.rotateDegrees(Axis.YP, offset * f2 * -30f);

        renderItem(transform, collector, stack, combinedLight);

        transform.popPose();
    }

    /**
     * Render an item in the middle of the screen.
     *
     * @param renderer      The {@link FirstPersonHandsAndItemsRenderer} instance.
     * @param player        The player holding this item.
     * @param transform     The matrix transformation stack
     * @param collector     The buffer to render to
     * @param combinedLight The current light level
     * @param pitch         The pitch of the player
     * @param equipProgress The equip progress of this item
     * @param swingProgress The swing progress of this item
     * @param stack         The stack to render
     * @see FirstPersonHandsAndItemsRenderer#renderTwoHandedMap(PoseStack, SubmitNodeCollector, int, float, float, float, PlayerRenderState, FirstPersonHandsAndItemsRenderState)
     */
    private void renderItemFirstPersonCenter(
        FirstPersonHandsAndItemsRenderer renderer, PlayerRenderState player, PoseStack transform, SubmitNodeCollector collector, int combinedLight,
        float pitch, float equipProgress, float swingProgress, ItemStack stack
    ) {
        // Setup the appropriate transformations. This is just copied from the
        // corresponding method in ItemRenderer.
        var swingRt = Mth.sqrt(swingProgress);
        var tX = -0.2f * Mth.sin(swingProgress * (float) Math.PI);
        var tZ = -0.4f * Mth.sin(swingRt * (float) Math.PI);
        transform.translate(0, -tX / 2, tZ);

        var pitchAngle = renderer.calculateMapTilt(pitch);
        transform.translate(0, 0.04F + equipProgress * -1.2f + pitchAngle * -0.5f, -0.72f);
        transform.rotateDegrees(Axis.XP, pitchAngle * -85.0f);
        if (player.avatarRenderState != null && !player.avatarRenderState.isInvisible) {
            transform.pushPose();
            transform.rotateDegrees(Axis.YP, 90.0F);
            renderer.renderMapHand(transform, collector, combinedLight, HumanoidArm.RIGHT, player);
            renderer.renderMapHand(transform, collector, combinedLight, HumanoidArm.LEFT, player);
            transform.popPose();
        }

        var rX = Mth.sin(swingRt * (float) Math.PI);
        transform.rotateDegrees(Axis.XP, rX * 20.0F);
        transform.scale(2.0F, 2.0F, 2.0F);

        renderItem(transform, collector, stack, combinedLight);
    }
}
