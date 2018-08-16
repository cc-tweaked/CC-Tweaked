/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.blocks;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.IComputer;
import dan200.computercraft.shared.computer.items.ComputerItemFactory;
import dan200.computercraft.shared.util.DirectionUtil;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Supplier;

public class BlockComputer extends BlockComputerBase
{
    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    public static final PropertyEnum<ComputerState> STATE = PropertyEnum.create( "state", ComputerState.class );

    private final ComputerFamily family;
    private final Supplier<TileComputer> factory;

    public BlockComputer( ComputerFamily family, Supplier<TileComputer> factory )
    {
        super( Material.ROCK );
        this.family = family;
        this.factory = factory;

        setHardness( 2.0f );
        setCreativeTab( ComputerCraft.mainCreativeTab );
        setDefaultState( this.blockState.getBaseState()
            .withProperty( FACING, EnumFacing.NORTH )
            .withProperty( STATE, ComputerState.Off )
        );
    }

    @Nonnull
    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer( this, FACING, STATE );
    }

    @Nonnull
    @Override
    @Deprecated
    public IBlockState getStateFromMeta( int meta )
    {
        EnumFacing dir = EnumFacing.byIndex( meta & 0x7 );
        if( dir.getAxis() == EnumFacing.Axis.Y ) dir = EnumFacing.NORTH;

        return getDefaultState().withProperty( FACING, dir );
    }

    @Override
    public int getMetaFromState( IBlockState state )
    {
        return state.getValue( FACING ).getIndex();
    }

    @Override
    public ComputerFamily getFamily()
    {
        return family;
    }

    @Override
    protected IBlockState getDefaultBlockState( EnumFacing placedSide )
    {
        IBlockState state = getDefaultState();
        if( placedSide.getAxis() != EnumFacing.Axis.Y ) state = state.withProperty( FACING, placedSide );
        return state;
    }

    @Nonnull
    @Override
    @Deprecated
    public IBlockState getActualState( @Nonnull IBlockState state, IBlockAccess world, BlockPos pos )
    {
        TileEntity tile = world.getTileEntity( pos );
        if( tile instanceof IComputerTile )
        {
            IComputer computer = ((IComputerTile) tile).getComputer();
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

    @Nullable
    @Override
    public TileEntity createTileEntity( @Nonnull World world, @Nonnull IBlockState state )
    {
        return factory.get();
    }

    @Override
    protected void getDroppedItems( IBlockState state, IBlockAccess world, BlockPos pos, @Nonnull NonNullList<ItemStack> drops, boolean creative )
    {
        TileEntity te = world.getTileEntity( pos );
        if( te instanceof TileComputer )
        {
            TileComputer computer = (TileComputer) te;
            if( !creative || computer.createProxy().getLabel() != null )
            {
                drops.add( ComputerItemFactory.create( computer ) );
            }
        }
    }

    @Override
    protected ItemStack getComputerItem( IBlockState state, IBlockAccess world, BlockPos pos )
    {
        TileEntity te = world.getTileEntity( pos );
        return te instanceof TileComputer ? ComputerItemFactory.create( (TileComputer) te ) : ItemStack.EMPTY;
    }

    @Override
    public void onBlockPlacedBy( World world, BlockPos pos, IBlockState state, EntityLivingBase player, @Nonnull ItemStack stack )
    {
        // Set direction
        EnumFacing dir = DirectionUtil.fromEntityRot( player );
        world.setBlockState( pos, state.withProperty( FACING, dir ) );

        // Trigger an update due to direction changing
        TileEntity tile = world.getTileEntity( pos );
        if( tile instanceof TileComputer ) ((TileComputer) tile).updateInput();
    }
}
