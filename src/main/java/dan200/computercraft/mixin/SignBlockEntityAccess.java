package dan200.computercraft.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.text.Text;

@Mixin (SignBlockEntity.class)
public interface SignBlockEntityAccess {
	@Accessor
	Text[] getText();
}
