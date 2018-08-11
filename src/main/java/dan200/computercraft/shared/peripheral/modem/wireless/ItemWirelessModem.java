/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2018. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.modem.wireless;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class ItemWirelessModem extends ItemBlock
{
    public ItemWirelessModem( Block block )
    {
        super( block );
    }

    @Override
    public boolean canPlaceBlockOnSide( World world, @Nonnull BlockPos pos, @Nonnull EnumFacing side, EntityPlayer player, ItemStack stack )
    {
        return world.isSideSolid( pos, side );
    }
}
