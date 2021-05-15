/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import dan200.computercraft.shared.network.NetworkHandler;
import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.network.client.PlayRecordClientMessage;

import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class RecordUtil {
    private RecordUtil() {}

    public static void playRecord(SoundEvent record, String recordInfo, World world, BlockPos pos) {
        NetworkMessage packet = record != null ? new PlayRecordClientMessage(pos, record, recordInfo) : new PlayRecordClientMessage(pos);
        NetworkHandler.sendToAllAround(packet, world, Vec3d.ofCenter(pos), 64);
    }
}
