/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.blocks;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.items.ComputerItemFactory;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
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

public class BlockComputer extends BlockComputerBase
{
    // Statics
    public static final class Properties
    {
        public static final PropertyDirection FACING = BlockHorizontal.FACING;
        public static final PropertyBool ADVANCED = PropertyBool.create( "advanced" );
        public static final PropertyEnum<ComputerState> STATE = PropertyEnum.create( "state", ComputerState.class );
    }

    // Members

    public BlockComputer()
    {
        super( Material.ROCK );
        setHardness( 2.0f );
        setTranslationKey( "computercraft:computer" );
        setCreativeTab( ComputerCraft.mainCreativeTab );
        setDefaultState( blockState.getBaseState()
            .withProperty( Properties.FACING, EnumFacing.NORTH )
            .withProperty( Properties.ADVANCED, false )
            .withProperty( Properties.STATE, ComputerState.Off )
        );
    }

    @Nonnull
    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer( this, Properties.FACING, Properties.ADVANCED, Properties.STATE );
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

        IBlockState state = getDefaultState().withProperty( Properties.FACING, dir );
        if( meta > 8 )
        {
            state = state.withProperty( Properties.ADVANCED, true );
        }
        else
        {
            state = state.withProperty( Properties.ADVANCED, false );
        }
        return state;
    }

    @Override
    public int getMetaFromState( IBlockState state )
    {
        int meta = state.getValue( Properties.FACING ).getIndex();
        if( state.getValue( Properties.ADVANCED ) )
        {
            meta += 8;
        }
        return meta;
    }

    @Override
    protected IBlockState getDefaultBlockState( ComputerFamily family, EnumFacing placedSide )
    {
        return getDefaultState()
            .withProperty( Properties.FACING, placedSide )
            .withProperty( Properties.ADVANCED, family == ComputerFamily.Advanced );
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
    public ComputerFamily getFamily( int damage )
    {
        return ComputerCraft.Items.computer.getFamily( damage );
    }

    @Override
    public ComputerFamily getFamily( IBlockState state )
    {
        if( state.getValue( Properties.ADVANCED ) )
        {
            return ComputerFamily.Advanced;
        }
        else
        {
            return ComputerFamily.Normal;
        }
    }

    @Override
    protected TileComputer createTile( ComputerFamily family )
    {
        return new TileComputer();
    }

    @Nonnull
    @Override
    protected ItemStack getItem( TileComputerBase tile )
    {
        return tile instanceof TileComputer ? ComputerItemFactory.create( (TileComputer) tile ) : ItemStack.EMPTY;
    }
}
