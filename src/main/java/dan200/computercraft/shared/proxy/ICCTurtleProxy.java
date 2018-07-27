/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.proxy;

import dan200.computercraft.api.turtle.ITurtleUpgrade;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

public interface ICCTurtleProxy
{
    void preInit();
    void init();

    void registerTurtleUpgrade( ITurtleUpgrade upgrade );
    ITurtleUpgrade getTurtleUpgrade( String id );
    ITurtleUpgrade getTurtleUpgrade( @Nonnull ItemStack item );
    void addAllUpgradedTurtles( NonNullList<ItemStack> list );

    void setDropConsumer( Entity entity, Consumer<ItemStack> consumer );
    void setDropConsumer( World world, BlockPos pos, Consumer<ItemStack> consumer );
    void clearDropConsumer();
}
