/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.blocks;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.items.ComputerItemFactory;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import javax.annotation.Nonnull;

public class BlockCommandComputer extends BlockComputerBase
{
    // Statics

    public static final class Properties
    {
        public static final PropertyDirection FACING = PropertyDirection.create( "facing", EnumFacing.Plane.HORIZONTAL );
        public static final PropertyEnum<ComputerState> STATE = PropertyEnum.create( "state", ComputerState.class );
    }

    // Members

    public BlockCommandComputer()
    {
        super( Material.IRON );
        setBlockUnbreakable();
        setResistance( 6000000.0F );
        setTranslationKey( "computercraft:command_computer" );
        setCreativeTab( ComputerCraft.mainCreativeTab );
        setDefaultState( blockState.getBaseState()
            .withProperty( Properties.FACING, EnumFacing.NORTH )
            .withProperty( Properties.STATE, ComputerState.Off )
        );
    }

    @Nonnull
    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer( this, Properties.FACING, Properties.STATE );
    }

    @Nonnull
    @Override
    @Deprecated
    public IBlockState getStateFromMeta( int meta )
    {
        EnumFacing dir = EnumFacing.byIndex( meta & 0x7 );
        if( dir.getAxis() == EnumFacing.Axis.Y )
        {
            dir = EnumFacing.NORTH;
        }
        return getDefaultState().withProperty( Properties.FACING, dir );
    }

    @Override
    public int getMetaFromState( IBlockState state )
    {
        return state.getValue( Properties.FACING ).getIndex();
    }

    @Nonnull
    @Override
    @Deprecated
    public IBlockState getActualState( @Nonnull IBlockState state, IBlockAccess world, BlockPos pos )
    {
        TileEntity tile = world.getTileEntity( pos );
        return state.withProperty( Properties.STATE, tile instanceof TileComputer ? ((TileComputer) tile).getState() : ComputerState.Off );
    }

    @Override
    protected IBlockState getDefaultBlockState( ComputerFamily family, EnumFacing placedSide )
    {
        return getDefaultState().withProperty( Properties.FACING, placedSide );
    }

    @Override
    public ComputerFamily getFamily( int damage )
    {
        return ComputerFamily.Command;
    }

    @Override
    public ComputerFamily getFamily( IBlockState state )
    {
        return ComputerFamily.Command;
    }

    @Override
    protected TileComputer createTile( ComputerFamily family )
    {
        return new TileCommandComputer();
    }

    @Nonnull
    @Override
    protected ItemStack getItem( TileComputerBase tile )
    {
        return tile instanceof TileCommandComputer ? ComputerItemFactory.create( (TileComputer) tile ) : ItemStack.EMPTY;
    }
}
