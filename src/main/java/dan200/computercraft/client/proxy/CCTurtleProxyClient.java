/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.proxy;

import dan200.computercraft.client.render.TileEntityTurtleRenderer;
import dan200.computercraft.shared.proxy.CCTurtleProxyCommon;
import dan200.computercraft.shared.turtle.blocks.TileTurtle;
import net.minecraftforge.fml.client.registry.ClientRegistry;

public class CCTurtleProxyClient extends CCTurtleProxyCommon
{
    @Override
    public void init()
    {
        super.init();

        // Setup renderers
        ClientRegistry.bindTileEntitySpecialRenderer( TileTurtle.class, new TileEntityTurtleRenderer() );
    }
}
