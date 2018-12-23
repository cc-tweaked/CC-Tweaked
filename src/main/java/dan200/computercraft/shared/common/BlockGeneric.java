/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.common;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public abstract class BlockGeneric extends Block implements
    ITileEntityProvider
{
    protected BlockGeneric( Material material )
    {
        super( material );
        this.hasTileEntity = true;
    }

    protected abstract IBlockState getDefaultBlockState( int damage, EnumFacing placedSide );

    protected abstract TileGeneric createTile( IBlockState state );

    protected abstract TileGeneric createTile( int damage );

    @Override
    public final void dropBlockAsItemWithChance( World world, @Nonnull BlockPos pos, @Nonnull IBlockState state, float chance, int fortune )
    {
    }

    @Override
    public final void getDrops( @Nonnull NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, @Nonnull IBlockState state, int fortune )
    {
        TileEntity tile = world.getTileEntity( pos );
        if( tile != null && tile instanceof TileGeneric )
        {
            TileGeneric generic = (TileGeneric) tile;
            generic.getDroppedItems( drops, false );
        }
    }

    @Nonnull
    @Override
    @Deprecated
    public final IBlockState getStateForPlacement( World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, int damage, EntityLivingBase placer )
    {
        return getDefaultBlockState( damage, side );
    }

    @Override
    public boolean removedByPlayer( @Nonnull IBlockState state, World world, @Nonnull BlockPos pos, @Nonnull EntityPlayer player, boolean willHarvest )
    {
        if( !world.isRemote )
        {
            // Drop items
            boolean creative = player.capabilities.isCreativeMode;
            dropAllItems( world, pos, creative );
        }

        // Remove block
        return super.removedByPlayer( state, world, pos, player, willHarvest );
    }

    public final void dropAllItems( World world, BlockPos pos, boolean creative )
    {
        // Get items to drop
        NonNullList<ItemStack> drops = NonNullList.create();
        TileEntity tile = world.getTileEntity( pos );
        if( tile != null && tile instanceof TileGeneric )
        {
            TileGeneric generic = (TileGeneric) tile;
            generic.getDroppedItems( drops, creative );
        }

        // Drop items
        if( drops.size() > 0 )
        {
            for( ItemStack item : drops )
            {
                dropItem( world, pos, item );
            }
        }
    }

    public final void dropItem( World world, BlockPos pos, @Nonnull ItemStack stack )
    {
        Block.spawnAsEntity( world, pos, stack );
    }

    @Override
    public final void breakBlock( @Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState newState )
    {
        TileEntity tile = world.getTileEntity( pos );
        super.breakBlock( world, pos, newState );
        world.removeTileEntity( pos );
        if( tile != null && tile instanceof TileGeneric )
        {
            TileGeneric generic = (TileGeneric) tile;
            generic.destroy();
        }
    }

    @Nonnull
    @Override
    public ItemStack getPickBlock( @Nonnull IBlockState state, RayTraceResult target, @Nonnull World world, @Nonnull BlockPos pos, EntityPlayer player )
    {
        TileEntity tile = world.getTileEntity( pos );
        if( tile != null && tile instanceof TileGeneric )
        {
            TileGeneric generic = (TileGeneric) tile;
            return generic.getPickedItem();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public final boolean onBlockActivated( World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ )
    {
        TileEntity tile = world.getTileEntity( pos );
        if( tile != null && tile instanceof TileGeneric )
        {
            TileGeneric generic = (TileGeneric) tile;
            return generic.onActivate( player, side, hitX, hitY, hitZ );
        }
        return false;
    }

    @Override
    @Deprecated
    public final void neighborChanged( IBlockState state, World world, BlockPos pos, Block block, BlockPos neighorPos )
    {
        TileEntity tile = world.getTileEntity( pos );
        if( tile != null && tile instanceof TileGeneric )
        {
            TileGeneric generic = (TileGeneric) tile;
            generic.onNeighbourChange();
        }
    }

    @Override
    public final void onNeighborChange( IBlockAccess world, BlockPos pos, BlockPos neighbour )
    {
        TileEntity tile = world.getTileEntity( pos );
        if( tile instanceof TileGeneric )
        {
            TileGeneric generic = (TileGeneric) tile;
            generic.onNeighbourTileEntityChange( neighbour );
        }
    }

    @Override
    @Deprecated
    public final boolean isSideSolid( IBlockState state, @Nonnull IBlockAccess world, @Nonnull BlockPos pos, EnumFacing side )
    {
        TileEntity tile = world.getTileEntity( pos );
        if( tile != null && tile instanceof TileGeneric )
        {
            TileGeneric generic = (TileGeneric) tile;
            return generic.isSolidOnSide( side.ordinal() );
        }
        return false;
    }

    @Override
    public final boolean canBeReplacedByLeaves( @Nonnull IBlockState state, @Nonnull IBlockAccess world, @Nonnull BlockPos pos )
    {
        return false; // Generify me if anyone ever feels the need to change this
    }

    @Override
    public float getExplosionResistance( World world, BlockPos pos, @Nullable Entity exploder, Explosion explosion )
    {
        TileEntity tile = world.getTileEntity( pos );
        if( tile != null && tile instanceof TileGeneric && tile.hasWorld() )
        {
            TileGeneric generic = (TileGeneric) tile;
            if( generic.isImmuneToExplosion( exploder ) )
            {
                return 2000.0f;
            }
        }
        return super.getExplosionResistance( world, pos, exploder, explosion );
    }

    @Nonnull
    @Override
    @Deprecated
    public final AxisAlignedBB getBoundingBox( IBlockState state, IBlockAccess world, BlockPos pos )
    {
        TileEntity tile = world.getTileEntity( pos );
        if( tile != null && tile instanceof TileGeneric && tile.hasWorld() )
        {
            TileGeneric generic = (TileGeneric) tile;
            return generic.getBounds();
        }
        return FULL_BLOCK_AABB;
    }

    @Nonnull
    @Override
    @Deprecated
    public final AxisAlignedBB getSelectedBoundingBox( IBlockState state, @Nonnull World world, @Nonnull BlockPos pos )
    {
        return getBoundingBox( state, world, pos ).offset( pos );
    }

    @Override
    @Deprecated
    public final AxisAlignedBB getCollisionBoundingBox( IBlockState state, @Nonnull IBlockAccess world, @Nonnull BlockPos pos )
    {
        TileEntity tile = world.getTileEntity( pos );
        if( tile != null && tile instanceof TileGeneric && tile.hasWorld() )
        {
            TileGeneric generic = (TileGeneric) tile;

            // Get collision bounds
            List<AxisAlignedBB> collision = new ArrayList<>( 1 );
            generic.getCollisionBounds( collision );

            // Return the union of the collision bounds
            if( collision.size() > 0 )
            {
                AxisAlignedBB aabb = collision.get( 0 );
                for( int i = 1; i < collision.size(); i++ )
                {
                    aabb = aabb.union( collision.get( i ) );
                }
                return aabb;
            }
        }
        return FULL_BLOCK_AABB;
    }

    @Override
    @Deprecated
    public final void addCollisionBoxToList( IBlockState state, @Nonnull World world, @Nonnull BlockPos pos, @Nonnull AxisAlignedBB bigBox, @Nonnull List<AxisAlignedBB> list, Entity entity, boolean p_185477_7_ )
    {
        TileEntity tile = world.getTileEntity( pos );
        if( tile != null && tile instanceof TileGeneric && tile.hasWorld() )
        {
            TileGeneric generic = (TileGeneric) tile;

            // Get collision bounds
            List<AxisAlignedBB> collision = new ArrayList<>( 1 );
            generic.getCollisionBounds( collision );

            // Add collision bounds to list
            if( collision.size() > 0 )
            {
                for( AxisAlignedBB localBounds : collision )
                {
                    addCollisionBoxToList( pos, bigBox, list, localBounds );
                }
            }
        }
    }

    @Override
    @Deprecated
    public final boolean canProvidePower( IBlockState state )
    {
        return true;
    }

    @Override
    public final boolean canConnectRedstone( IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side )
    {
        TileEntity tile = world.getTileEntity( pos );
        if( tile != null && tile instanceof TileGeneric )
        {
            TileGeneric generic = (TileGeneric) tile;
            return generic.getRedstoneConnectivity( side );
        }
        return false;
    }

    @Override
    @Deprecated
    public final int getStrongPower( IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing oppositeSide )
    {
        TileEntity tile = world.getTileEntity( pos );
        if( tile != null && tile instanceof TileGeneric && tile.hasWorld() )
        {
            TileGeneric generic = (TileGeneric) tile;
            return generic.getRedstoneOutput( oppositeSide.getOpposite() );
        }
        return 0;
    }

    @Override
    @Deprecated
    public final int getWeakPower( IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing oppositeSide )
    {
        return getStrongPower( state, world, pos, oppositeSide );
    }

    public boolean getBundledRedstoneConnectivity( World world, BlockPos pos, EnumFacing side )
    {
        TileEntity tile = world.getTileEntity( pos );
        if( tile != null && tile instanceof TileGeneric )
        {
            TileGeneric generic = (TileGeneric) tile;
            return generic.getBundledRedstoneConnectivity( side );
        }
        return false;
    }

    public int getBundledRedstoneOutput( World world, BlockPos pos, EnumFacing side )
    {
        TileEntity tile = world.getTileEntity( pos );
        if( tile != null && tile instanceof TileGeneric && tile.hasWorld() )
        {
            TileGeneric generic = (TileGeneric) tile;
            return generic.getBundledRedstoneOutput( side );
        }
        return 0;
    }

    @Nonnull
    @Override
    public final TileEntity createTileEntity( @Nonnull World world, @Nonnull IBlockState state )
    {
        return createTile( state );
    }

    @Nonnull
    @Override
    public final TileEntity createNewTileEntity( @Nonnull World world, int damage )
    {
        return createTile( damage );
    }
}
