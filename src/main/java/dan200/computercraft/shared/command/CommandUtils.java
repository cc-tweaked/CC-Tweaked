/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.command;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.common.util.FakePlayer;

public final class CommandUtils
{
    private CommandUtils() {}

    public static boolean isPlayer( ICommandSender sender )
    {
        return sender instanceof EntityPlayerMP
            && !(sender instanceof FakePlayer)
            && ((EntityPlayerMP) sender).connection != null;
    }
}
