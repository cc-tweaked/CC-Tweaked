/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2018. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.network.ComputerCraftPacket;
import net.minecraft.item.Item;
import net.minecraft.item.ItemRecord;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import javax.annotation.Nonnull;

public class RecordUtil
{
    public static void playRecord( SoundEvent record, String recordInfo, World world, BlockPos pos )
    {
        ComputerCraftPacket packet = new ComputerCraftPacket();
        packet.m_packetType = ComputerCraftPacket.PlayRecord;
        if( record != null )
        {
            packet.m_dataInt = new int[] { pos.getX(), pos.getY(), pos.getZ(), SoundEvent.REGISTRY.getIDForObject( record ) };
            packet.m_dataString = new String[] { recordInfo };
        }
        else
        {
            packet.m_dataInt = new int[] { pos.getX(), pos.getY(), pos.getZ() };
        }

        NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint( world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), 64 );
        ComputerCraft.sendToAllAround( packet, point );
    }

    public static String getRecordInfo( @Nonnull ItemStack recordStack )
    {
        Item item = recordStack.getItem();
        if( !(item instanceof ItemRecord) ) return null;

        ItemRecord record = (ItemRecord) item;
        return StringUtil.translateToLocal( record.displayName );
    }
}
