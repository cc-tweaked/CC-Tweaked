package dan200.computercraft.shared.peripheral.diskdrive;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.common.BlockGeneric;
import dan200.computercraft.shared.peripheral.common.TilePeripheralBase;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import javax.annotation.Nonnull;

public class BlockDiskDrive extends BlockGeneric
{
    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    public static final PropertyEnum<DiskDriveState> STATE = PropertyEnum.create( "state", DiskDriveState.class );

    public BlockDiskDrive()
    {
        super( Material.ROCK );

        setHardness( 2.0f );
        setTranslationKey( "computercraft:disk_drive" );
        setCreativeTab( ComputerCraft.mainCreativeTab );
        setDefaultState( this.blockState.getBaseState()
            .withProperty( FACING, EnumFacing.NORTH )
            .withProperty( STATE, DiskDriveState.EMPTY )
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
        return getDefaultState().withProperty( FACING, EnumFacing.byHorizontalIndex( meta ) );
    }

    @Override
    public int getMetaFromState( IBlockState state )
    {
        return state.getValue( FACING ).getHorizontalIndex();
    }

    @Override
    protected IBlockState getDefaultBlockState( int damage, EnumFacing placedSide )
    {
        IBlockState state = getDefaultState();
        if( placedSide.getAxis() != EnumFacing.Axis.Y ) state = state.withProperty( FACING, placedSide );
        return state;
    }

    @Override
    protected TilePeripheralBase createTile( int damage )
    {
        return new TileDiskDrive();
    }

    @Nonnull
    @Override
    @Deprecated
    public IBlockState getActualState( @Nonnull IBlockState state, IBlockAccess world, BlockPos pos )
    {
        TileEntity tile = world.getTileEntity( pos );
        if( tile instanceof TileDiskDrive )
        {
            state = state.withProperty( STATE, ((TileDiskDrive) tile).getState() );
        }
        return state;
    }
}
