/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import dan200.computercraft.shared.network.NetworkHandler;
import dan200.computercraft.shared.network.client.PlayRecordClientMessage;
import net.minecraft.item.Item;
import net.minecraft.item.ItemRecord;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import javax.annotation.Nonnull;

public final class RecordUtil
{
    private RecordUtil() {}

    public static void playRecord( SoundEvent record, String recordInfo, World world, BlockPos pos )
    {
        IMessage packet = record != null ? new PlayRecordClientMessage( pos, record, recordInfo ) : new PlayRecordClientMessage( pos );

        NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint( world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), 64 );
        NetworkHandler.sendToAllAround( packet, point );
    }

    public static String getRecordInfo( @Nonnull ItemStack recordStack )
    {
        Item item = recordStack.getItem();
        if( !(item instanceof ItemRecord) ) return null;

        ItemRecord record = (ItemRecord) item;
        return StringUtil.translate( record.displayName );
    }
}
