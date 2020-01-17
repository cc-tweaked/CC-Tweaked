/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.common;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class TileGeneric extends TileEntity
{
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

    public boolean onActivate( EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ )
    {
        return false;
    }

    @Deprecated
    public void onNeighbourChange()
    {
    }

    public void onNeighbourChange( @Nonnull BlockPos neighbour )
    {
        onNeighbourChange();
    }

    public void onNeighbourTileEntityChange( @Nonnull BlockPos neighbour )
    {
    }

    protected void updateTick()
    {
    }

    public boolean getRedstoneConnectivity( EnumFacing side )
    {
        return false;
    }

    public int getRedstoneOutput( EnumFacing side )
    {
        return 0;
    }

    public boolean getBundledRedstoneConnectivity( @Nonnull EnumFacing side )
    {
        return false;
    }

    public int getBundledRedstoneOutput( @Nonnull EnumFacing side )
    {
        return 0;
    }

    protected double getInteractRange( EntityPlayer player )
    {
        return 8.0;
    }

    public boolean isUsable( EntityPlayer player, boolean ignoreRange )
    {
        if( player == null || !player.isEntityAlive() || getWorld().getTileEntity( getPos() ) != this ) return false;
        if( ignoreRange ) return true;

        double range = getInteractRange( player );
        BlockPos pos = getPos();
        return player.getEntityWorld() == getWorld() &&
            player.getDistanceSq( pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5 ) <= range * range;
    }

    protected void writeDescription( @Nonnull NBTTagCompound nbt )
    {
    }

    protected void readDescription( @Nonnull NBTTagCompound nbt )
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
        NBTTagCompound nbt = new NBTTagCompound();
        writeDescription( nbt );
        return new SPacketUpdateTileEntity( getPos(), 0, nbt );
    }

    @Override
    public final void onDataPacket( NetworkManager net, SPacketUpdateTileEntity packet )
    {
        if( packet.getTileEntityType() == 0 ) readDescription( packet.getNbtCompound() );
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
