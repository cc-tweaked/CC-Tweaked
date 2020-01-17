/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.common;

import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public abstract class BlockDirectional extends BlockGeneric
{
    protected BlockDirectional( Material material )
    {
        super( material );
    }

    public EnumFacing getDirection( IBlockAccess world, BlockPos pos )
    {
        TileEntity tile = world.getTileEntity( pos );
        if( tile instanceof IDirectionalTile )
        {
            IDirectionalTile directional = (IDirectionalTile) tile;
            return directional.getDirection();
        }
        return EnumFacing.NORTH;
    }

    public void setDirection( World world, BlockPos pos, EnumFacing dir )
    {
        TileEntity tile = world.getTileEntity( pos );
        if( tile instanceof IDirectionalTile )
        {
            IDirectionalTile directional = (IDirectionalTile) tile;
            directional.setDirection( dir );
        }
    }
}
