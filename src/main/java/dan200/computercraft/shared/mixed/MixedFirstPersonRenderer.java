/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.mixed;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;

public interface MixedFirstPersonRenderer {
    void renderArmFirstPerson_CC(MatrixStack stack, VertexConsumerProvider consumerProvider, int light, float equip, float swing, Arm hand);

    float getMapAngleFromPitch_CC(float pitch);
}
