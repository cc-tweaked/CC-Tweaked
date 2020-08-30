/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import net.minecraft.nbt.Tag;
import net.minecraft.util.math.Direction;
import net.minecraftforge.common.capabilities.Capability;

public class NullStorage<T> implements Capability.IStorage<T>
{
    @Override
    public Tag writeNBT( Capability<T> capability, T instance, Direction side )
    {
        return null;
    }

    @Override
    public void readNBT( Capability<T> capability, T instance, Direction side, Tag base )
    {
    }
}
