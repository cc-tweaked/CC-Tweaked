/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.integration.mcmp;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.peripheral.modem.wireless.BlockAdvancedModem;
import mcmultipart.api.multipart.IMultipart;
import mcmultipart.api.slot.EnumFaceSlot;
import mcmultipart.api.slot.IPartSlot;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class PartAdvancedModem implements IMultipart
{
    @Override
    public IPartSlot getSlotForPlacement( World world, BlockPos pos, IBlockState state, EnumFacing facing, float hitX, float hitY, float hitZ, EntityLivingBase placer )
    {
        return EnumFaceSlot.fromFace( state.getValue( BlockAdvancedModem.FACING ) );
    }

    @Override
    public IPartSlot getSlotFromWorld( IBlockAccess world, BlockPos pos, IBlockState state )
    {
        return EnumFaceSlot.fromFace( state.getValue( BlockAdvancedModem.FACING ) );
    }

    @Override
    public Block getBlock()
    {
        return ComputerCraft.Blocks.advancedModem;
    }
}
