/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.fabric.mixin;

import net.minecraft.util.math.AffineTransformation;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin( AffineTransformation.class )
public interface AffineTransformationAccess
{
    @Accessor
    Vec3f getTranslation();

    @Accessor
    Vec3f getScale();

    @Accessor
    Quaternion getRotation1();
}
