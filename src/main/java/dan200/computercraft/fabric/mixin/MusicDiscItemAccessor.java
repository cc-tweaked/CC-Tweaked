package dan200.computercraft.fabric.mixin;

import net.minecraft.item.MusicDiscItem;
import net.minecraft.sound.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MusicDiscItem.class)
public interface MusicDiscItemAccessor {
    @Accessor
    SoundEvent getSound();
}
