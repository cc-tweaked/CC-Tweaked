/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.integration.mcmp;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.peripheral.common.BlockPeripheral;
import dan200.computercraft.shared.peripheral.common.BlockPeripheralVariant;
import mcmultipart.api.multipart.IMultipart;
import mcmultipart.api.slot.EnumCenterSlot;
import mcmultipart.api.slot.EnumFaceSlot;
import mcmultipart.api.slot.IPartSlot;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class PartPeripheral implements IMultipart
{
    @Override
    public IPartSlot getSlotForPlacement( World world, BlockPos pos, IBlockState state, Direction facing, float hitX, float hitY, float hitZ, EntityLivingBase placer )
    {
        return getSlot( state );
    }

    @Override
    public IPartSlot getSlotFromWorld( IBlockAccess world, BlockPos pos, IBlockState state )
    {
        return getSlot( state );
    }

    @Nonnull
    private static IPartSlot getSlot( IBlockState state )
    {
        BlockPeripheralVariant type = state.getValue( BlockPeripheral.VARIANT );
        if( type == BlockPeripheralVariant.WirelessModemUpOn || type == BlockPeripheralVariant.WirelessModemUpOff )
        {
            return EnumFaceSlot.UP;
        }
        else if( type == BlockPeripheralVariant.WirelessModemDownOn || type == BlockPeripheralVariant.WirelessModemDownOff )
        {
            return EnumFaceSlot.DOWN;
        }
        else if( type == BlockPeripheralVariant.WirelessModemOff || type == BlockPeripheralVariant.WirelessModemOn )
        {
            return EnumFaceSlot.fromFace( state.getValue( BlockPeripheral.FACING ) );
        }
        else
        {
            return EnumCenterSlot.CENTER;
        }
    }

    @Override
    public Block getBlock()
    {
        return ComputerCraft.Blocks.peripheral;
    }
}
