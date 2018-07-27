/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.blocks;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.IComputer;
import dan200.computercraft.shared.computer.items.ItemComputer;
import dan200.computercraft.shared.util.DirectionUtil;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class BlockComputer extends BlockComputerBase
{
    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    public static final PropertyBool ADVANCED = PropertyBool.create("advanced");
    public static final PropertyEnum<ComputerState> STATE = PropertyEnum.create("state", ComputerState.class);

    // Members
    
    public BlockComputer()
    {
        super( Material.ROCK );
        setHardness( 2.0f );
        setTranslationKey( "computercraft:computer" );
        setCreativeTab( ComputerCraft.mainCreativeTab );
        setDefaultState( this.blockState.getBaseState()
            .withProperty( FACING, EnumFacing.NORTH )
            .withProperty( ADVANCED, false )
            .withProperty( STATE, ComputerState.Off )
        );
    }

    @Nonnull
    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer( this, FACING, ADVANCED, STATE );
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

        IBlockState state = getDefaultState().withProperty( FACING, dir );
        if( meta > 8 )
        {
            state = state.withProperty( ADVANCED, true );
        }
        else
        {
            state = state.withProperty( ADVANCED, false );
        }
        return state;
    }

    @Override
    public int getMetaFromState( IBlockState state )
    {
        int meta = state.getValue( FACING ).getIndex();
        if( state.getValue( ADVANCED ) )
        {
            meta += 8;
        }
        return meta;
    }

    @Override
    protected IBlockState getDefaultBlockState( ComputerFamily family, EnumFacing placedSide )
    {
        IBlockState state = getDefaultState();
        if( placedSide.getAxis() != EnumFacing.Axis.Y )
        {
            state = state.withProperty( FACING, placedSide );
        }

        switch( family )
        {
            case Normal:
            default:
            {
                return state.withProperty( ADVANCED, false );
            }
            case Advanced:
            {
                return state.withProperty( ADVANCED, true );
            }
        }
    }

    @Nonnull
    @Override
    @Deprecated
    public IBlockState getActualState( @Nonnull IBlockState state, IBlockAccess world, BlockPos pos )
    {
        TileEntity tile = world.getTileEntity( pos );
        if( tile instanceof IComputerTile )
        {
            IComputer computer = ((IComputerTile)tile).getComputer();
            if( computer != null && computer.isOn() )
            {
                if( computer.isCursorDisplayed() )
                {
                    return state.withProperty( STATE, ComputerState.Blinking );
                }
                else
                {
                    return state.withProperty( STATE, ComputerState.On );
                }
            }
        }
        return state.withProperty( STATE, ComputerState.Off );
    }

    @Override
    public ComputerFamily getFamily( int damage )
    {
        return ((ItemComputer) Item.getItemFromBlock(this)).getFamily( damage );
    }

    @Override
    public ComputerFamily getFamily( IBlockState state )
    {
        if( state.getValue( ADVANCED ) ) {
            return ComputerFamily.Advanced;
        } else {
            return ComputerFamily.Normal;
        }
    }

    @Override
    protected TileComputer createTile( ComputerFamily family )
    {
        return new TileComputer();
    }

    @Override
    public void onBlockPlacedBy( World world, BlockPos pos, IBlockState state, EntityLivingBase player, @Nonnull ItemStack stack )
    {
        // Not sure why this is necessary
        TileEntity tile = world.getTileEntity( pos );
        if( tile instanceof TileComputer )
        {
            tile.setWorld( world ); // Not sure why this is necessary
            tile.setPos( pos ); // Not sure why this is necessary
        }

        // Set direction
        EnumFacing dir = DirectionUtil.fromEntityRot( player );
        setDirection( world, pos, dir );
    }
}
