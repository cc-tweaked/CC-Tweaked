/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.fabric.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;

@Mixin( BlockEntity.class )
public class MixinBlockEntity
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
