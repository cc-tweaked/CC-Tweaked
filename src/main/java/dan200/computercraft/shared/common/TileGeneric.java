/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.common;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraftforge.common.util.Constants;

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
        setChanged();
        BlockPos pos = getBlockPos();
        BlockState state = getBlockState();
        getLevel().sendBlockUpdated( pos, state, state, Constants.BlockFlags.DEFAULT );
    }

    @Nonnull
    public ActionResultType onActivate( PlayerEntity player, Hand hand, BlockRayTraceResult hit )
    {
        return ActionResultType.PASS;
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

    protected double getInteractRange( PlayerEntity player )
    {
        return 8.0;
    }

    public boolean isUsable( PlayerEntity player, boolean ignoreRange )
    {
        if( player == null || !player.isAlive() || getLevel().getBlockEntity( getBlockPos() ) != this ) return false;
        if( ignoreRange ) return true;

        double range = getInteractRange( player );
        BlockPos pos = getBlockPos();
        return player.getCommandSenderWorld() == getLevel() &&
            player.distanceToSqr( pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5 ) <= range * range;
    }

    @Override
    public final void onDataPacket( NetworkManager net, SUpdateTileEntityPacket packet )
    {
        if( packet.getType() == 0 ) handleUpdateTag( packet.getTag() );
    }

    @Override
    public final void handleUpdateTag( @Nonnull BlockState state, @Nonnull CompoundNBT tag )
    {
        handleUpdateTag( tag );
    }

    protected void handleUpdateTag( @Nonnull CompoundNBT tag )
    {
    }
}
