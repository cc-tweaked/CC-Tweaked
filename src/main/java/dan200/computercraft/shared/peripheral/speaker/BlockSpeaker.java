package dan200.computercraft.shared.peripheral.speaker;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.common.BlockGeneric;
import dan200.computercraft.shared.util.DirectionUtil;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BlockSpeaker extends BlockGeneric
{
    public static final PropertyDirection FACING = BlockHorizontal.FACING;

    public BlockSpeaker()
    {
        super( Material.ROCK );

        setHardness( 2.0f );
        setTranslationKey( "computercraft:speaker" );
        setCreativeTab( ComputerCraft.mainCreativeTab );
        setDefaultState( this.blockState.getBaseState().withProperty( FACING, EnumFacing.NORTH ) );
    }

    @Nonnull
    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer( this, FACING );
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
        return new TileSpeaker();
    }

    @Override
    public void onBlockPlacedBy( World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack )
    {
        EnumFacing dir = DirectionUtil.fromEntityRot( placer );
        world.setBlockState( pos, state.withProperty( FACING, dir ) );
    }
}
