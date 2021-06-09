/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.fabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.util.math.AffineTransformation;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.util.math.Quaternion;

@Mixin (AffineTransformation.class)
public interface AffineTransformationAccess {
	@Accessor
	Vector3f getTranslation();

	@Accessor
	Vector3f getScale();

	@Accessor
	Quaternion getRotation1();
}
