/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.proxy;

import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;
import java.util.function.Function;

public interface ICCTurtleProxy
{
    void preInit();

    void init();

    void setDropConsumer( Entity entity, Function<ItemStack, ItemStack> consumer );

    void setDropConsumer( World world, BlockPos pos, Function<ItemStack, ItemStack> consumer );

    List<ItemStack> clearDropConsumer();
}
