/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2018. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.api.turtle.event;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;

/**
 * A wrapper for {@link ServerPlayerEntity} which denotes a "fake" player.
 *
 * Please note that this does not implement any of the traditional fake player behaviour. It simply exists to prevent
 * me passing in normal players.
 */
public class FakePlayer extends ServerPlayerEntity
{
    public FakePlayer( ServerWorld world, GameProfile gameProfile )
    {
        super( world.getServer(), world, gameProfile, new ServerPlayerInteractionManager( world ) );
    }
}
