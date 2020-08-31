package dan200.computercraft.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;

@Mixin (BlockEntityType.class)
public interface BlockEntityTypeAccessor {
	@Invoker
	static <T extends BlockEntity> BlockEntityType<T> callCreate(String string, BlockEntityType.Builder<T> builder) { throw new UnsupportedOperationException(); }
}
