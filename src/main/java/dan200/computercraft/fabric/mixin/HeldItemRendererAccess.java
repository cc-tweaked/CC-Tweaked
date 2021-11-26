/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.fabric.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin( ItemInHandRenderer.class )
public interface HeldItemRendererAccess
{
    @Invoker
    float callGetMapAngle( float tickDelta );

    @Invoker
    void callRenderArm( PoseStack matrices, MultiBufferSource vertexConsumers, int light, HumanoidArm arm );

    @Invoker
    void callRenderArmHoldingItem( PoseStack matrices, MultiBufferSource vertexConsumers, int light, float equipProgress, float swingProgress, HumanoidArm arm );
}
