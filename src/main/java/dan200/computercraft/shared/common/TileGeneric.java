/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.common;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.network.ComputerCraftPacket;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class TileGeneric extends TileEntity
{
    public void requestTileEntityUpdate()
    {
        if( getWorld().isRemote )
        {
            ComputerCraftPacket packet = new ComputerCraftPacket();
            packet.m_packetType = ComputerCraftPacket.RequestTileEntityUpdate;

            BlockPos pos = getPos();
            packet.m_dataInt = new int[]{ pos.getX(), pos.getY(), pos.getZ() };
            ComputerCraft.sendToServer( packet );
        }
    }

    public void destroy()
    {
    }

    @Nullable
    public BlockGeneric getBlock()
    {
        Block block = getBlockType();
        return block instanceof BlockGeneric ? (BlockGeneric) block : null;
    }

    protected final IBlockState getBlockState()
    {
        return getWorld().getBlockState( getPos() );
    }

    public final void updateBlock()
    {
        markDirty();
        BlockPos pos = getPos();
        IBlockState state = getWorld().getBlockState( pos );
        getWorld().markBlockRangeForRenderUpdate( pos, pos );
        getWorld().notifyBlockUpdate( getPos(), state, state, 3 );
    }

    protected final void setBlockState( IBlockState newState )
    {
        getWorld().setBlockState( getPos(), newState, 3 );
    }

    public void getDroppedItems( @Nonnull NonNullList<ItemStack> drops, boolean creative )
    {
        if( !creative )
        {
            ItemStack drop = getPickedItem();
            if( !drop.isEmpty() ) drops.add( drop );
        }
    }

    @Nonnull
    public ItemStack getPickedItem()
    {
        return ItemStack.EMPTY;
    }

    public boolean onActivate( EntityPlayer player, EnumFacing side, float hitX, float hitY, float hitZ )
    {
        return false;
    }

    public void onNeighbourChange()
    {
    }

    public void onNeighbourTileEntityChange( @Nonnull BlockPos neighbour )
    {
    }

    @Nonnull
    public AxisAlignedBB getBounds()
    {
        return new AxisAlignedBB( 0.0, 0.0, 0.0, 1.0, 1.0, 1.0 );
    }

    protected double getInteractRange( EntityPlayer player )
    {
        return 8.0;
    }

    public boolean isUsable( EntityPlayer player, boolean ignoreRange )
    {
        if( player != null && player.isEntityAlive() )
        {
            if( getWorld().getTileEntity( getPos() ) == this )
            {
                if( !ignoreRange )
                {
                    double range = getInteractRange( player );
                    BlockPos pos = getPos();
                    return player.getEntityWorld() == getWorld() &&
                        player.getDistanceSq( pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5 ) <= (range * range);
                }
                return true;
            }
        }
        return false;
    }

    protected void writeDescription( @Nonnull NBTTagCompound nbttagcompound )
    {
    }

    protected void readDescription( @Nonnull NBTTagCompound nbttagcompound )
    {
    }

    @Override
    public boolean shouldRefresh( World world, BlockPos pos, @Nonnull IBlockState oldState, @Nonnull IBlockState newState )
    {
        return newState.getBlock() != oldState.getBlock();
    }

    @Override
    public final SPacketUpdateTileEntity getUpdatePacket()
    {
        // Communicate properties
        NBTTagCompound nbttagcompound = new NBTTagCompound();
        writeDescription( nbttagcompound );
        return new SPacketUpdateTileEntity( getPos(), 0, nbttagcompound );
    }

    @Override
    public final void onDataPacket( NetworkManager net, SPacketUpdateTileEntity packet )
    {
        switch( packet.getTileEntityType() )
        {
            case 0:
            {
                // Receive properties
                NBTTagCompound nbttagcompound = packet.getNbtCompound();
                readDescription( nbttagcompound );
                break;
            }
        }
    }

    @Nonnull
    @Override
    public NBTTagCompound getUpdateTag()
    {
        NBTTagCompound tag = super.getUpdateTag();
        writeDescription( tag );
        return tag;
    }

    @Override
    public void handleUpdateTag( @Nonnull NBTTagCompound tag )
    {
        super.handleUpdateTag( tag );
        readDescription( tag );
    }
}
