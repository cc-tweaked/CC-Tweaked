/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class TurtlePermissions
{
    public static boolean isBlockEnterable( Level world, BlockPos pos, Player player )
    {
        MinecraftServer server = world.getServer();
        return server == null || world.isClientSide || (world instanceof ServerLevel && !server.isUnderSpawnProtection( (ServerLevel) world, pos, player ));
    }

    public static boolean isBlockEditable( Level world, BlockPos pos, Player player )
    {
        return isBlockEnterable( world, pos, player );
    }
}
