/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.network.client.PlayRecordClientMessage;
import dan200.computercraft.shared.platform.PlatformHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class RecordUtil {
    private RecordUtil() {
    }

    public static void playRecord(SoundEvent record, String recordInfo, Level world, BlockPos pos) {
        NetworkMessage packet = record != null ? new PlayRecordClientMessage(pos, record, recordInfo) : new PlayRecordClientMessage(pos);
        PlatformHelper.get().sendToAllAround(packet, (ServerLevel) world, Vec3.atCenterOf(pos), 64);
    }
}
