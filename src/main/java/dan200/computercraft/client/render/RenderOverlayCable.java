/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.render;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.peripheral.modem.wired.BlockCable;
import dan200.computercraft.shared.peripheral.modem.wired.CableShapes;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL11;

@Mod.EventBusSubscriber( modid = ComputerCraft.MOD_ID, value = Dist.CLIENT )
public final class RenderOverlayCable
{
    private RenderOverlayCable()
    {
    }

    /**
     * Draw an outline for a specific part of a cable "Multipart".
     *
     * @param event The event to observe
     * @see WorldRenderer#drawSelectionBox(EntityPlayer, RayTraceResult, int, float)
     */
    @SubscribeEvent
    public static void drawHighlight( DrawBlockHighlightEvent event )
    {
        if( event.getTarget().type != RayTraceResult.Type.BLOCK ) return;

        BlockPos pos = event.getTarget().getBlockPos();
        World world = event.getPlayer().getEntityWorld();

        IBlockState state = world.getBlockState( pos );

        // We only care about instances with both cable and modem.
        if( state.getBlock() != ComputerCraft.Blocks.cable || state.get( BlockCable.MODEM ).getFacing() == null || !state.get( BlockCable.CABLE ) )
        {
            return;
        }

        event.setCanceled( true );

        EntityPlayer player = event.getPlayer();
        Minecraft mc = Minecraft.getInstance();
        float partialTicks = event.getPartialTicks();

        GlStateManager.enableBlend();
        GlStateManager.blendFuncSeparate( GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO );
        GlStateManager.lineWidth( Math.max( 2.5F, mc.mainWindow.getFramebufferWidth() / 1920.0F * 2.5F ) );
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask( false );
        GlStateManager.matrixMode( GL11.GL_PROJECTION );
        GlStateManager.pushMatrix();
        GlStateManager.scalef( 1.0F, 1.0F, 0.999F );

        double x = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
        double y = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
        double z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;

        VoxelShape shape = WorldUtil.isVecInside( CableShapes.getModemShape( state ), event.getTarget().hitVec.subtract( pos.getX(), pos.getY(), pos.getZ() ) )
            ? CableShapes.getModemShape( state )
            : CableShapes.getCableShape( state );

        WorldRenderer.drawShape( shape, pos.getX() - x, pos.getY() - y, pos.getZ() - z, 0.0F, 0.0F, 0.0F, 0.4F );

        GlStateManager.popMatrix();
        GlStateManager.matrixMode( GL11.GL_MODELVIEW );
        GlStateManager.depthMask( true );
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }
}
