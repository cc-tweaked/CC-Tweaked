package dan200.computercraft.fabric.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;

@Mixin( BlockEntity.class )
public class BlockEntityMixin
{
    @Final
    @Mutable
    @Shadow
    protected BlockPos pos;

    public void setBlockPos( BlockPos pos )
    {
        this.pos = pos;
    }
}
