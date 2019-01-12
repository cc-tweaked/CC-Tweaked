/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.proxy;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.render.TileEntityTurtleRenderer;
import dan200.computercraft.client.render.TurtleSmartItemModel;
import dan200.computercraft.shared.proxy.CCTurtleProxyCommon;
import dan200.computercraft.shared.turtle.blocks.TileTurtle;
import dan200.computercraft.shared.turtle.items.ItemTurtleBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class CCTurtleProxyClient extends CCTurtleProxyCommon
{
    @Override
    public void preInit()
    {
        super.preInit();
        MinecraftForge.EVENT_BUS.register( new ForgeHandlers() );
    }

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

    public static class ForgeHandlers
    {
        private final TurtleSmartItemModel m_turtleSmartItemModel = new TurtleSmartItemModel();

        ForgeHandlers()
        {
            IResourceManager resourceManager = Minecraft.getMinecraft().getResourceManager();
            if( resourceManager instanceof IReloadableResourceManager )
            {
                ((IReloadableResourceManager) resourceManager).registerReloadListener( m_turtleSmartItemModel );
            }
        }

        @SubscribeEvent
        public void onModelBakeEvent( ModelBakeEvent event )
        {
            event.getModelRegistry().putObject( new ModelResourceLocation( "computercraft:turtle_dynamic", "inventory" ), m_turtleSmartItemModel );
        }
    }

}
