/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.mixed;

import net.minecraft.sortme.OptionMainHand;

public interface MixedFirstPersonRenderer
{
    void renderArms_CC();

    void renderArmFirstPerson_CC( float equip, float swing, OptionMainHand hand );

    float getMapAngleFromPitch_CC( float pitch );
}
