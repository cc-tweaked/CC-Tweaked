/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.render;

import dan200.computercraft.shared.turtle.core.TurtlePlayer;
import javax.annotation.Nonnull;

import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class TurtlePlayerRenderer extends EntityRenderer<TurtlePlayer>
{
    public TurtlePlayerRenderer( EntityRenderDispatcher renderManager )
    {
        super( renderManager );
    }

    public TurtlePlayerRenderer(EntityRenderDispatcher entityRenderDispatcher, EntityRendererRegistry.Context context) {
        super(entityRenderDispatcher);
    }

    @Nonnull
    @Override
    public Identifier getTexture( @Nonnull TurtlePlayer entity )
    {
        return ComputerBorderRenderer.BACKGROUND_NORMAL;
    }

    @Override
    public void render( @Nonnull TurtlePlayer entityIn, float entityYaw, float partialTicks, @Nonnull MatrixStack transform, @Nonnull VertexConsumerProvider buffer, int packedLightIn )
    {
    }
}
