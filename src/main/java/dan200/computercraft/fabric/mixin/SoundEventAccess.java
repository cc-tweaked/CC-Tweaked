package dan200.computercraft.fabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

@Mixin (SoundEvent.class)
public interface SoundEventAccess {
	@Accessor
	Identifier getId();
}
