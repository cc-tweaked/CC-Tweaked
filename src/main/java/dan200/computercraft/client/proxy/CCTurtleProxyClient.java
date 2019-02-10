/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.proxy;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.render.TileEntityTurtleRenderer;
import dan200.computercraft.shared.proxy.CCTurtleProxyCommon;
import dan200.computercraft.shared.turtle.blocks.TileTurtle;
import dan200.computercraft.shared.turtle.items.ItemTurtleBase;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.client.registry.ClientRegistry;

public class CCTurtleProxyClient extends CCTurtleProxyCommon
{
    @Override
    public void init()
    {
        super.init();

        // Setup turtle colours
        Minecraft.getMinecraft().getItemColors().registerItemColorHandler( ( stack, tintIndex ) -> {
            if( tintIndex == 0 )
            {
                ItemTurtleBase turtle = (ItemTurtleBase) stack.getItem();
                int colour = turtle.getColour( stack );
                if( colour != -1 ) return colour;
            }

            return 0xFFFFFF;
        }, ComputerCraft.Blocks.turtle, ComputerCraft.Blocks.turtleExpanded, ComputerCraft.Blocks.turtleAdvanced );

        // Setup renderers
        ClientRegistry.bindTileEntitySpecialRenderer( TileTurtle.class, new TileEntityTurtleRenderer() );
    }
}
