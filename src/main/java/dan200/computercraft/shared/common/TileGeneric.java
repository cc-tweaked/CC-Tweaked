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
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class TileGeneric extends TileEntity
{
    private IBlockState blockState;
    private IBlockState blockStateLatest;

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
        Block block = getBlockState().getBlock();
        return block instanceof BlockGeneric ? (BlockGeneric) block : null;
    }

    @Nonnull
    @Override
    @SuppressWarnings( "ConstantConditions" )
    public Block getBlockType()
    {
        if( blockType == null && world != null )
        {
            blockState = blockStateLatest = world.getBlockState( pos );
            blockType = blockState.getBlock();
        }

        return blockType;
    }

    public final IBlockState getBlockState()
    {
        if( blockState == null && world != null )
        {
            blockState = blockStateLatest = world.getBlockState( pos );
            blockType = blockState.getBlock();
        }

        return blockState;
    }

    /**
     * A thread-safe variant of {@link #getBlockState()}, which will only fetch the cached version.
     *
     * This is not always guaranteed to be up-to-date.
     *
     * @return The cached block state
     */
    protected final IBlockState getBlockStateSafe()
    {
        return blockStateLatest;
    }

    public void updateContainingBlockInfo()
    {
        super.updateContainingBlockInfo();
        blockState = null;
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
        if( getWorld().setBlockState( getPos(), newState, 3 ) )
        {
            blockState = blockStateLatest = newState;
        }
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
