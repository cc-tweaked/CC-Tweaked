/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.support;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

import javax.annotation.Nonnull;

public class FakeContainer extends AbstractContainerMenu
{
    public FakeContainer()
    {
        super( null, 0 );
    }

    @Override
    public boolean stillValid( @Nonnull Player player )
    {
        return true;
    }
}
