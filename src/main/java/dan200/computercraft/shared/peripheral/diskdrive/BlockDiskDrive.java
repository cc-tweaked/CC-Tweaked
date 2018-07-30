package dan200.computercraft.shared.peripheral.diskdrive;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.common.BlockGeneric;
import dan200.computercraft.shared.peripheral.printer.TilePrinter;
import dan200.computercraft.shared.util.DirectionUtil;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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

    @Nullable
    @Override
    public TileEntity createTileEntity( @Nonnull World world, @Nonnull IBlockState state )
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

    @Override
    public void getDrops( @Nonnull NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, @Nonnull IBlockState state, int fortune )
    {
        drops.add( getItem( world, pos ) );
    }

    @Nonnull
    @Override
    public ItemStack getPickBlock( @Nonnull IBlockState state, RayTraceResult target, @Nonnull World world, @Nonnull BlockPos pos, EntityPlayer player )
    {
        return getItem( world, pos );
    }

    private ItemStack getItem( IBlockAccess world, BlockPos pos )
    {
        ItemStack stack = new ItemStack( this );
        TileEntity te = world.getTileEntity( pos );
        if( te instanceof TilePrinter )
        {
            String label = ((TilePrinter) te).getLabel();
            if( label != null ) stack.setStackDisplayName( label );
        }

        return stack;
    }

    @Override
    public void onBlockPlacedBy( World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack )
    {
        EnumFacing dir = DirectionUtil.fromEntityRot( placer );
        world.setBlockState( pos, state.withProperty( FACING, dir ) );

        TileEntity tile = world.getTileEntity( pos );
        if( stack.hasDisplayName() && tile instanceof TileDiskDrive )
        {
            TileDiskDrive peripheral = (TileDiskDrive) tile;
            peripheral.setLabel( stack.getDisplayName() );
        }
    }
}
