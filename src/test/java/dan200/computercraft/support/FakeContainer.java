/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.support;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.Container;

import javax.annotation.Nonnull;

public class FakeContainer extends Container
{
    public FakeContainer()
    {
        super( null, 0 );
    }

    @Override
    public boolean stillValid( @Nonnull PlayerEntity player )
    {
        return true;
    }
}
