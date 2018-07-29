package dan200.computercraft.shared.peripheral.monitor;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.common.BlockGeneric;
import dan200.computercraft.shared.util.DirectionUtil;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.function.Supplier;

public class BlockMonitor extends BlockGeneric
{
    private static final EnumFacing[] ORIENTATIONS = new EnumFacing[]{
        // Note this is the same order as EnumFacing.VALUES
        EnumFacing.DOWN, EnumFacing.UP, EnumFacing.NORTH
    };

    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    public static final PropertyDirection ORIENTATION = PropertyDirection.create( "orientation", Arrays.asList( ORIENTATIONS ) );
    public static final PropertyBool LEFT = PropertyBool.create( "left" );
    public static final PropertyBool RIGHT = PropertyBool.create( "right" );
    public static final PropertyBool UP = PropertyBool.create( "up" );
    public static final PropertyBool DOWN = PropertyBool.create( "down" );

    private final Supplier<TileMonitor> factory;

    public BlockMonitor( Supplier<TileMonitor> factory )
    {
        super( Material.ROCK );
        this.factory = factory;

        setHardness( 2.0f );
        setCreativeTab( ComputerCraft.mainCreativeTab );
        setDefaultState( this.blockState.getBaseState()
            .withProperty( FACING, EnumFacing.NORTH )
            .withProperty( ORIENTATION, EnumFacing.NORTH )
            .withProperty( LEFT, false ).withProperty( RIGHT, false )
            .withProperty( UP, false ).withProperty( DOWN, false )
        );
    }

    @Nonnull
    @Override
    public String getTranslationKey()
    {
        return "tile." + getRegistryName();
    }

    @Nonnull
    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer( this, FACING, ORIENTATION, LEFT, RIGHT, UP, DOWN );
    }

    @Nonnull
    @Override
    @Deprecated
    public IBlockState getStateFromMeta( int meta )
    {
        return getDefaultState()
            .withProperty( FACING, EnumFacing.byHorizontalIndex( meta & 0x3 ) )
            .withProperty( ORIENTATION, ORIENTATIONS[(meta >> 2) & 0x3] );
    }

    @Override
    public int getMetaFromState( IBlockState state )
    {
        return state.getValue( FACING ).getHorizontalIndex() |
            (state.getValue( ORIENTATION ).getIndex() << 2);
    }

    @Override
    protected IBlockState getDefaultBlockState( int damage, EnumFacing placedSide )
    {
        return getDefaultState();
    }

    @Nonnull
    @Override
    @Deprecated
    public IBlockState getActualState( @Nonnull IBlockState state, IBlockAccess worldIn, BlockPos pos )
    {
        state = super.getActualState( state, worldIn, pos );

        TileEntity tile = worldIn.getTileEntity( pos );
        if( !(tile instanceof TileMonitor) ) return state;

        TileMonitor monitor = (TileMonitor) tile;
        return state
            .withProperty( DOWN, monitor.getYIndex() > 0 )
            .withProperty( UP, monitor.getYIndex() < monitor.getHeight() - 1 )
            .withProperty( LEFT, monitor.getXIndex() > 0 )
            .withProperty( RIGHT, monitor.getXIndex() < monitor.getWidth() - 1 );
    }

    @Nullable
    @Override
    public TileEntity createTileEntity( @Nonnull World world, @Nonnull IBlockState state )
    {
        return factory.get();
    }

    @Override
    public void onBlockPlacedBy( World world, BlockPos pos, IBlockState state, EntityLivingBase player, @Nonnull ItemStack stack )
    {
        TileEntity tile = world.getTileEntity( pos );

        if( tile instanceof TileMonitor && !world.isRemote ) ((TileMonitor) tile).contractNeighbours();

        // Update the block's direction and orientation
        world.setBlockState( pos, state
            .withProperty( FACING, DirectionUtil.fromEntityRot( player ) )
            .withProperty( ORIENTATION, DirectionUtil.fromPitchAngle( player ) ) );

        if( tile instanceof TileMonitor && !world.isRemote )
        {
            TileMonitor monitor = (TileMonitor) tile;
            monitor.contract();
            monitor.expand();
        }
    }
}
