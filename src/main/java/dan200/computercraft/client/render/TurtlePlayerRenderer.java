/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import dan200.computercraft.shared.turtle.core.TurtlePlayer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;

public class TurtlePlayerRenderer extends EntityRenderer<TurtlePlayer>
{
    public TurtlePlayerRenderer( EntityRendererManager renderManager )
    {
        super( renderManager );
    }

    @Nonnull
    @Override
    public ResourceLocation getEntityTexture( @Nonnull TurtlePlayer entity )
    {
        return ComputerBorderRenderer.BACKGROUND_NORMAL;
    }

    @Override
    public void render( @Nonnull TurtlePlayer entityIn, float entityYaw, float partialTicks, @Nonnull MatrixStack transform, @Nonnull IRenderTypeBuffer buffer, int packedLightIn )
    {
    }
}
