/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.common;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;

public abstract class TileGeneric extends TileEntity
{
    public TileGeneric( TileEntityType<? extends TileGeneric> type )
    {
        super( type );
    }

    public void destroy()
    {
    }

    public final void updateBlock()
    {
        markDirty();
        BlockPos pos = getPos();
        IBlockState state = getBlockState();
        getWorld().markBlockRangeForRenderUpdate( pos, pos );
        getWorld().notifyBlockUpdate( pos, state, state, 3 );
    }

    public boolean onActivate( EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ )
    {
        return false;
    }

    public void onNeighbourChange( @Nonnull BlockPos neighbour )
    {
    }

    public void onNeighbourTileEntityChange( @Nonnull BlockPos neighbour )
    {
    }

    protected void blockTick()
    {
    }

    protected double getInteractRange( EntityPlayer player )
    {
        return 8.0;
    }

    public boolean isUsable( EntityPlayer player, boolean ignoreRange )
    {
        if( player == null || !player.isAlive() || getWorld().getTileEntity( getPos() ) != this ) return false;
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

    @Nonnull
    @Override
    public final SPacketUpdateTileEntity getUpdatePacket()
    {
        NBTTagCompound nbt = new NBTTagCompound();
        writeDescription( nbt );
        return new SPacketUpdateTileEntity( pos, 0, nbt );
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
