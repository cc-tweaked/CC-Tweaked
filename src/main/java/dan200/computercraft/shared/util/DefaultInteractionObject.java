/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import dan200.computercraft.shared.network.container.ContainerType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.world.IInteractionObject;

import javax.annotation.Nonnull;

public interface DefaultInteractionObject<T extends Container> extends IInteractionObject
{
    @Nonnull
    @Override
    T createContainer( @Nonnull InventoryPlayer inventory, @Nonnull EntityPlayer player );

    @Nonnull
    ContainerType<T> getContainerType();

    @Nonnull
    @Override
    default String getGuiID()
    {
        return getContainerType().getId().toString();
    }
}
