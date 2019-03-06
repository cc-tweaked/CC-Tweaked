/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class TurtlePermissions
{
    public static boolean isBlockEnterable( World world, BlockPos pos, PlayerEntity player )
    {
        MinecraftServer server = world.getServer();
        return server == null || world.isClient || !server.isSpawnProtected( world, pos, player );
    }

    public static boolean isBlockEditable( World world, BlockPos pos, PlayerEntity player )
    {
        MinecraftServer server = world.getServer();
        return server == null || world.isClient || !server.isSpawnProtected( world, pos, player );
    }

    /*
    @SubscribeEvent
    public static void onTurtleAction( TurtleActionEvent event )
    {
        if( ComputerCraft.turtleDisabledActions.contains( event.getAction() ) )
        {
            event.setCanceled( true, "Action has been disabled" );
        }
    }
    */
}
