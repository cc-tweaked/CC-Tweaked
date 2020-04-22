/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.render;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.turtle.core.TurtlePlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TurtlePlayerRenderer extends EntityRenderer<TurtlePlayer>
{
    public TurtlePlayerRenderer( EntityRendererManager renderManager )
    {
        super( renderManager );
    }

    @Override
    public void doRender( @Nonnull TurtlePlayer entity, double x, double y, double z, float entityYaw, float partialTicks )
    {
        ComputerCraft.log.error( "Rendering TurtlePlayer on the client side, at {}", entity.getPosition() );
    }

    @Nullable
    @Override
    protected ResourceLocation getEntityTexture( @Nonnull TurtlePlayer entity )
    {
        return null;
    }
}
