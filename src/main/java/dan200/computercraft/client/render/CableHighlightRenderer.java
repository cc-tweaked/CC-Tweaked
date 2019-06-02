/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.peripheral.modem.wired.BlockCable;
import dan200.computercraft.shared.peripheral.modem.wired.CableShapes;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

public final class CableHighlightRenderer
{
    private CableHighlightRenderer()
    {
    }

    /**
     * Draw an outline for a specific part of a cable "Multipart".
     *
     * @see WorldRenderer#drawHighlightedBlockOutline(Entity, HitResult, int, float)
     */
    public static boolean drawHighlight( Camera camera, BlockHitResult hit )
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        BlockPos pos = hit.getBlockPos();
        World world = mc.world;

        BlockState state = world.getBlockState( pos );

        // We only care about instances with both cable and modem.
        if( state.getBlock() != ComputerCraft.Blocks.cable || state.get( BlockCable.MODEM ).getFacing() == null || !state.get( BlockCable.CABLE ) )
        {
            return false;
        }

        GlStateManager.enableBlend();
        GlStateManager.blendFuncSeparate( GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO );
        GlStateManager.lineWidth( Math.max( 2.5F, mc.window.getFramebufferWidth() / 1920.0F * 2.5F ) );
        GlStateManager.disableTexture();
        GlStateManager.depthMask( false );
        GlStateManager.matrixMode( GL11.GL_PROJECTION );
        GlStateManager.pushMatrix();
        GlStateManager.scalef( 1.0F, 1.0F, 0.999F );

        VoxelShape shape = WorldUtil.isVecInside( CableShapes.getModemShape( state ), hit.getPos().subtract( pos.getX(), pos.getY(), pos.getZ() ) )
            ? CableShapes.getModemShape( state )
            : CableShapes.getCableShape( state );

        WorldRenderer.drawShapeOutline( shape, pos.getX() - camera.getPos().getX(), pos.getY() - camera.getPos().getY(), pos.getZ() - camera.getPos().getZ(), 0.0F, 0.0F, 0.0F, 0.4F );

        GlStateManager.popMatrix();
        GlStateManager.matrixMode( GL11.GL_MODELVIEW );
        GlStateManager.depthMask( true );
        GlStateManager.enableTexture();
        GlStateManager.disableBlend();

        return true;
    }
}
