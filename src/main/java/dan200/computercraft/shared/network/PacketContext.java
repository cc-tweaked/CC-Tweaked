/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network;

import net.minecraft.world.entity.player.Player;

import java.util.concurrent.Executor;

public record PacketContext(Player player, Executor executor)
{
    public Player getPlayer()
    {
        return player();
    }
}
