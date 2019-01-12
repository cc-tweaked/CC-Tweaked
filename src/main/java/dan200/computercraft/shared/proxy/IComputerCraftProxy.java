/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.proxy;

import net.minecraft.server.MinecraftServer;

public interface IComputerCraftProxy
{
    void preInit();

    void init();

    void initServer( MinecraftServer server );
}
