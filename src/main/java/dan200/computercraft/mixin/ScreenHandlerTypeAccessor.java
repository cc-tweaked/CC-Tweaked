package dan200.computercraft.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;

@Mixin (ScreenHandlerType.class)
public interface ScreenHandlerTypeAccessor {
	@Invoker
	static <T extends ScreenHandler> ScreenHandlerType<T> callRegister(String id, ScreenHandlerType.Factory<T> factory) { throw new UnsupportedOperationException(); }
}
