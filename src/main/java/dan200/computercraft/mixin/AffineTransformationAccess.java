package dan200.computercraft.mixin;

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
