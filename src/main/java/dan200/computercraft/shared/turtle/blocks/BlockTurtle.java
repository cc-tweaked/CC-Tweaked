/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.blocks;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.computer.blocks.BlockComputerBase;
import dan200.computercraft.shared.computer.blocks.TileComputerBase;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.util.DirectionUtil;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BlockTurtle extends BlockComputerBase
{
    public static final PropertyDirection FACING = BlockHorizontal.FACING;

    // Members

    public BlockTurtle()
    {
        super( Material.IRON );
        setHardness( 2.5f );
        setTranslationKey( "computercraft:turtle_normal" );
        setCreativeTab( ComputerCraft.mainCreativeTab );
        setDefaultState( this.blockState.getBaseState()
            .withProperty( FACING, EnumFacing.NORTH )
        );
    }

    @Nonnull
    @Override
    @Deprecated
    public EnumBlockRenderType getRenderType( IBlockState state )
    {
        return EnumBlockRenderType.INVISIBLE;
    }

    @Override
    @Deprecated
    public boolean isOpaqueCube( IBlockState state )
    {
        return false;
    }

    @Override
    @Deprecated
    public boolean isFullCube( IBlockState state )
    {
        return false;
    }

    @Nonnull
    @Override
    @Deprecated
    public BlockFaceShape getBlockFaceShape( IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing side )
    {
        return BlockFaceShape.UNDEFINED;
    }

    @Nonnull
    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, FACING );
    }

    @Nonnull
    @Override
    @Deprecated
    public IBlockState getStateFromMeta( int meta )
    {
        return getDefaultState();
    }

    @Override
    public int getMetaFromState( IBlockState state )
    {
        return 0;
    }

    @Nonnull
    @Override
    @Deprecated
    public IBlockState getActualState( @Nonnull IBlockState state, IBlockAccess world, BlockPos pos )
    {
        return state.withProperty( FACING, getDirection( world, pos ) );
    }

    @Override
    protected IBlockState getDefaultBlockState( ComputerFamily family, EnumFacing placedSide )
    {
        return getDefaultState();
    }

    @Override
    @Deprecated
    public float getExplosionResistance( @Nullable Entity exploder)
    {
        return getFamily() == ComputerFamily.Advanced || exploder instanceof EntityLivingBase || exploder instanceof EntityFireball
            ? 2000 : super.getExplosionResistance( exploder);
    }

    private ComputerFamily getFamily()
    {
        if( this == ComputerCraft.Blocks.turtleAdvanced )
        {
            return ComputerFamily.Advanced;
        }
        else
        {
            return ComputerFamily.Normal;
        }
    }

    @Override
    public ComputerFamily getFamily( int damage )
    {
        return getFamily();
    }

    @Override
    public ComputerFamily getFamily( IBlockState state )
    {
        return getFamily();
    }

    @Override
    protected TileComputerBase createTile( ComputerFamily family )
    {
        if( this == ComputerCraft.Blocks.turtleAdvanced )
        {
            return new TileTurtleAdvanced();
        }
        else
        {
            return new TileTurtleNormal();
        }
    }

    @Override
    public void onBlockPlacedBy( World world, BlockPos pos, IBlockState state, EntityLivingBase player, @Nonnull ItemStack itemstack )
    {
        // Not sure why this is necessary
        TileEntity tile = world.getTileEntity( pos );
        if( tile instanceof TileTurtle )
        {
            tile.setWorld( world ); // Not sure why this is necessary
            tile.setPos( pos ); // Not sure why this is necessary
            if( player instanceof EntityPlayer )
            {
                ((TileTurtle) tile).setOwningPlayer( ((EntityPlayer) player).getGameProfile() );
            }
        }

        // Set direction
        EnumFacing dir = DirectionUtil.fromEntityRot( player );
        setDirection( world, pos, dir.getOpposite() );
    }
}
