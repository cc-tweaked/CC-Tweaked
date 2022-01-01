/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dan200.computercraft.shared.turtle.core.TurtlePlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;

public class TurtlePlayerRenderer extends EntityRenderer<TurtlePlayer>
{
    public TurtlePlayerRenderer( EntityRendererProvider.Context renderManager )
    {
        super( renderManager );
    }

    @Nonnull
    @Override
    public ResourceLocation getTextureLocation( @Nonnull TurtlePlayer entity )
    {
        return ComputerBorderRenderer.BACKGROUND_NORMAL;
    }

    @Override
    public void render( @Nonnull TurtlePlayer entityIn, float entityYaw, float partialTicks, @Nonnull PoseStack transform, @Nonnull MultiBufferSource buffer, int packedLightIn )
    {
    }
}
