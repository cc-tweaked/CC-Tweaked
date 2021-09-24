/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.common;

import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;

public abstract class TileGeneric extends BlockEntity implements BlockEntityClientSerializable
{
    public TileGeneric( BlockEntityType<? extends TileGeneric> type, BlockPos pos, BlockState state )
    {
        super( type, pos, state );
    }

    public void destroy()
    {
    }

    public void onChunkUnloaded()
    {
    }

    public final void updateBlock()
    {
        markDirty();
        BlockPos pos = getPos();
        BlockState state = getCachedState();
        getWorld().updateListeners( pos, state, state, 3 );
    }

    @Nonnull
    public ActionResult onActivate( PlayerEntity player, Hand hand, BlockHitResult hit )
    {
        return ActionResult.PASS;
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

    public boolean isUsable( PlayerEntity player, boolean ignoreRange )
    {
        if( player == null || !player.isAlive() || getWorld().getBlockEntity( getPos() ) != this )
        {
            return false;
        }
        if( ignoreRange )
        {
            return true;
        }

        double range = getInteractRange( player );
        BlockPos pos = getPos();
        return player.getEntityWorld() == getWorld() && player.squaredDistanceTo( pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5 ) <= range * range;
    }

    protected double getInteractRange( PlayerEntity player )
    {
        return 8.0;
    }

    @Override
    public void fromClientTag( NbtCompound compoundTag )
    {
        readDescription( compoundTag );
    }

    protected void readDescription( @Nonnull NbtCompound nbt )
    {
    }

    @Override
    public NbtCompound toClientTag( NbtCompound compoundTag )
    {
        writeDescription( compoundTag );
        return compoundTag;
    }

    protected void writeDescription( @Nonnull NbtCompound nbt )
    {
    }
}
